package net.kaaass.zerotierfix.vpn.tun

import android.os.ParcelFileDescriptor
import android.util.Log

import com.zerotier.sdk.Node
import com.zerotier.sdk.ResultCode
import com.zerotier.sdk.VirtualNetworkConfig
import com.zerotier.sdk.VirtualNetworkFrameListener
import com.zerotier.sdk.util.StringUtils as SdkStringUtils

import net.kaaass.zerotierfix.vpn.engine.VpnRuntime
import net.kaaass.zerotierfix.util.DebugLog
import net.kaaass.zerotierfix.util.IPPacketUtils
import net.kaaass.zerotierfix.util.InetAddressUtils
import net.kaaass.zerotierfix.vpn.route.Route

import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.HashMap
import java.util.Objects

/**
 * TUN 与 ZeroTier 虚拟网络之间的帧转发适配器。
 */
class TunTapAdapter(
    private val ztService: VpnRuntime,
    private val networkId: Long
) : VirtualNetworkFrameListener {

    private val routeMap = HashMap<Route, Long>()

    private var arpTable: ARPTable? = ARPTable()
    private var input: FileInputStream? = null
    private var ndpTable: NDPTable? = NDPTable()
    private var node: Node? = null
    private var output: FileOutputStream? = null
    private var receiveThread: Thread? = null
    private var vpnSocket: ParcelFileDescriptor? = null

    private fun addMulticastRoutes() {
    }

    /**
     * 设置关联的 ZeroTier 节点，并订阅默认组播地址。
     *
     * @param node 节点实例
     */
    fun setNode(node: Node) {
        this.node = node
        try {
            val multicastAddress = InetAddress.getByName("224.224.224.224")
            val result = node
                .multicastSubscribe(this.networkId, multicastAddressToMAC(multicastAddress))
            if (result != ResultCode.RESULT_OK) {
                Log.e(TAG, "Error when calling multicastSubscribe: $result")
            }
        } catch (e: UnknownHostException) {
            Log.e(TAG, e.toString(), e)
        }
    }

    /**
     * 设置 VPN 文件描述符。
     *
     * @param vpnSocket VPN 文件描述符
     */
    fun setVpnSocket(vpnSocket: ParcelFileDescriptor?) {
        this.vpnSocket = vpnSocket
    }

    /**
     * 设置 TUN 读写流。
     *
     * @param fileInputStream 输入流
     * @param fileOutputStream 输出流
     */
    fun setFileStreams(fileInputStream: FileInputStream, fileOutputStream: FileOutputStream) {
        this.input = fileInputStream
        this.output = fileOutputStream
    }

    /**
     * 添加路由与网络的映射。
     *
     * @param route 路由
     * @param networkId 网络 ID
     */
    fun addRouteAndNetwork(route: Route, networkId: Long) {
        synchronized(this.routeMap) {
            this.routeMap[route] = networkId
        }
    }

    /**
     * 清空路由映射并重新添加组播路由。
     */
    fun clearRouteMap() {
        synchronized(this.routeMap) {
            this.routeMap.clear()
            addMulticastRoutes()
        }
    }

    private fun isIPv4Multicast(inetAddress: InetAddress): Boolean =
        (inetAddress.address[0].toInt() and 0xF0) == 224

    private fun isIPv6Multicast(inetAddress: InetAddress): Boolean =
        (inetAddress.address[0].toInt() and 0xFF) == 0xFF

    /**
     * 启动 TUN 接收线程。
     */
    fun startThreads() {
        this.receiveThread = object : Thread("Tunnel Receive Thread") {

            override fun run() {
                // 创建 ARP、NDP 表
                if (this@TunTapAdapter.ndpTable == null) {
                    this@TunTapAdapter.ndpTable = NDPTable()
                }
                if (this@TunTapAdapter.arpTable == null) {
                    this@TunTapAdapter.arpTable = ARPTable()
                }
                // 转发 TUN 消息至 Zerotier
                try {
                    Log.d(TAG, "TUN Receive Thread Started")
                    val buffer = ByteBuffer.allocate(32767)
                    buffer.order(ByteOrder.LITTLE_ENDIAN)
                    while (!isInterrupted) {
                        try {
                            var noDataBeenRead = true
                            val readCount = this@TunTapAdapter.input!!.read(buffer.array())
                            if (readCount > 0) {
                                DebugLog.d(
                                    TAG,
                                    "Sending packet to ZeroTier. $readCount bytes."
                                )
                                val readData = ByteArray(readCount)
                                System.arraycopy(buffer.array(), 0, readData, 0, readCount)
                                val ipVersion = IPPacketUtils.getIPVersion(readData)
                                if (ipVersion.toInt() == 4) {
                                    this@TunTapAdapter.handleIPv4Packet(readData)
                                } else if (ipVersion.toInt() == 6) {
                                    this@TunTapAdapter.handleIPv6Packet(readData)
                                } else {
                                    Log.e(TAG, "Unknown IP version")
                                }
                                buffer.clear()
                                noDataBeenRead = false
                            }
                            if (noDataBeenRead) {
                                Thread.sleep(10)
                            }
                        } catch (e: IOException) {
                            Log.e(TAG, "Error in TUN Receive: " + e.message, e)
                        }
                    }
                } catch (ignored: InterruptedException) {
                }
                Log.d(TAG, "TUN Receive Thread ended")
                // 关闭 ARP、NDP 表
                this@TunTapAdapter.ndpTable!!.stop()
                this@TunTapAdapter.ndpTable = null
                this@TunTapAdapter.arpTable!!.stop()
                this@TunTapAdapter.arpTable = null
            }
        }
        this.receiveThread!!.start()
    }

    private fun handleIPv4Packet(packetData: ByteArray) {
        var packet = packetData
        var destIP = IPPacketUtils.getDestIP(packet)
        val sourceIP = IPPacketUtils.getSourceIP(packet)
        val virtualNetworkConfig = this.ztService.getVirtualNetworkConfig(this.networkId)

        if (virtualNetworkConfig == null) {
            Log.e(TAG, "TunTapAdapter has no network config yet")
            return
        } else if (destIP == null) {
            Log.e(TAG, "destAddress is null")
            return
        } else if (sourceIP == null) {
            Log.e(TAG, "sourceAddress is null")
            return
        }
        val isMulticast: Boolean
        if (isIPv4Multicast(destIP!!)) {
            val result = this.node!!.multicastSubscribe(
                this.networkId,
                multicastAddressToMAC(destIP!!)
            )
            if (result != ResultCode.RESULT_OK) {
                Log.e(TAG, "Error when calling multicastSubscribe: $result")
            }
            isMulticast = true
        } else {
            isMulticast = false
        }
        val route = routeForDestination(destIP!!)
        val gateway = if (route != null) route.gateway else null

        // 查找当前节点的 v4 地址
        val ztAddresses = virtualNetworkConfig.assignedAddresses
        var localV4Address: InetAddress? = null
        var cidr = 0

        for (address in ztAddresses) {
            if (address.address is Inet4Address) {
                localV4Address = address.address
                cidr = address.port
                break
            }
        }

        val destRoute = InetAddressUtils.addressToRouteNo0Route(destIP!!, cidr)
        val sourceRoute = InetAddressUtils.addressToRouteNo0Route(sourceIP, cidr)
        if (gateway != null && !Objects.equals(destRoute, sourceRoute)) {
            destIP = gateway
        }
        if (localV4Address == null) {
            Log.e(TAG, "Couldn't determine local address")
            return
        }

        val localMac = virtualNetworkConfig.mac
        val nextDeadline = LongArray(1)
        if (isMulticast || this.arpTable!!.hasMacForAddress(destIP!!)) {
            // 已确定目标 MAC，直接发送
            val destMac: Long = if (isIPv4Multicast(destIP!!)) {
                multicastAddressToMAC(destIP!!)
            } else {
                this.arpTable!!.getMacForAddress(destIP!!)
            }
            val result = this.node!!.processVirtualNetworkFrame(
                System.currentTimeMillis(),
                this.networkId,
                localMac,
                destMac,
                IPV4_PACKET,
                0,
                packet,
                nextDeadline
            )
            if (result != ResultCode.RESULT_OK) {
                Log.e(TAG, "Error calling processVirtualNetworkFrame: " + result.toString())
                return
            }
            Log.d(TAG, "Packet sent to ZT")
            this.ztService.setNextBackgroundTaskDeadline(nextDeadline[0])
        } else {
            // 目标 MAC 未知，进行 ARP 查询
            Log.d(TAG, "Unknown dest MAC address.  Need to look it up. $destIP")
            val destMac = InetAddressUtils.BROADCAST_MAC_ADDRESS
            packet = this.arpTable!!.getRequestPacket(localMac, localV4Address, destIP!!)
            val result = this.node!!.processVirtualNetworkFrame(
                System.currentTimeMillis(),
                this.networkId,
                localMac,
                destMac,
                ARP_PACKET,
                0,
                packet,
                nextDeadline
            )
            if (result != ResultCode.RESULT_OK) {
                Log.e(TAG, "Error sending ARP packet: " + result.toString())
                return
            }
            Log.d(TAG, "ARP Request Sent!")
            this.ztService.setNextBackgroundTaskDeadline(nextDeadline[0])
        }
    }

    private fun handleIPv6Packet(packetData: ByteArray) {
        var packet = packetData
        var destIP = IPPacketUtils.getDestIP(packet)
        val sourceIP = IPPacketUtils.getSourceIP(packet)
        val virtualNetworkConfig = this.ztService.getVirtualNetworkConfig(this.networkId)

        if (virtualNetworkConfig == null) {
            Log.e(TAG, "TunTapAdapter has no network config yet")
            return
        } else if (destIP == null) {
            Log.e(TAG, "destAddress is null")
            return
        } else if (sourceIP == null) {
            Log.e(TAG, "sourceAddress is null")
            return
        }
        if (isIPv6Multicast(destIP!!)) {
            val result = this.node!!.multicastSubscribe(
                this.networkId,
                multicastAddressToMAC(destIP!!)
            )
            if (result != ResultCode.RESULT_OK) {
                Log.e(TAG, "Error when calling multicastSubscribe: $result")
            }
        }
        val route = routeForDestination(destIP!!)
        val gateway = if (route != null) route.gateway else null

        // 查找当前节点的 v6 地址
        val ztAddresses = virtualNetworkConfig.assignedAddresses
        var localV4Address: InetAddress? = null
        var cidr = 0

        for (address in ztAddresses) {
            if (address.address is Inet6Address) {
                localV4Address = address.address
                cidr = address.port
                break
            }
        }

        val destRoute = InetAddressUtils.addressToRouteNo0Route(destIP!!, cidr)
        val sourceRoute = InetAddressUtils.addressToRouteNo0Route(sourceIP, cidr)
        if (gateway != null && !Objects.equals(destRoute, sourceRoute)) {
            destIP = gateway
        }
        if (localV4Address == null) {
            Log.e(TAG, "Couldn't determine local address")
            return
        }

        val localMac = virtualNetworkConfig.mac
        val nextDeadline = LongArray(1)

        // 确定目标 MAC 地址
        var destMac: Long
        var sendNSPacket = false
        if (isNeighborSolicitation(packet)) {
            // 收到本地 NS 报文，根据 NDP 表记录确定是否广播查询
            destMac = if (this.ndpTable!!.hasMacForAddress(destIP!!)) {
                this.ndpTable!!.getMacForAddress(destIP!!)
            } else {
                InetAddressUtils.ipv6ToMulticastAddress(destIP!!)
            }
        } else if (isIPv6Multicast(destIP!!)) {
            // 多播报文
            destMac = multicastAddressToMAC(destIP!!)
        } else if (isNeighborAdvertisement(packet)) {
            // 收到本地 NA 报文
            destMac = if (this.ndpTable!!.hasMacForAddress(destIP!!)) {
                this.ndpTable!!.getMacForAddress(destIP!!)
            } else {
                // 目标 MAC 未知，不发送数据包
                0L
            }
            sendNSPacket = true
        } else {
            // 收到普通数据包，根据 NDP 表记录确定是否发送 NS 请求
            if (this.ndpTable!!.hasMacForAddress(destIP!!)) {
                // 目标地址 MAC 已知
                destMac = this.ndpTable!!.getMacForAddress(destIP!!)
            } else {
                destMac = 0L
                sendNSPacket = true
            }
        }
        // 发送数据包
        if (destMac != 0L) {
            val result = this.node!!.processVirtualNetworkFrame(
                System.currentTimeMillis(),
                this.networkId,
                localMac,
                destMac,
                IPV6_PACKET,
                0,
                packet,
                nextDeadline
            )
            if (result != ResultCode.RESULT_OK) {
                Log.e(TAG, "Error calling processVirtualNetworkFrame: " + result.toString())
            } else {
                DebugLog.d(TAG, "Packet sent to ZT")
                this.ztService.setNextBackgroundTaskDeadline(nextDeadline[0])
            }
        }
        // 发送 NS 请求
        if (sendNSPacket) {
            if (destMac == 0L) {
                destMac = InetAddressUtils.ipv6ToMulticastAddress(destIP!!)
            }
            Log.d(TAG, "Sending Neighbor Solicitation")
            packet = this.ndpTable!!.getNeighborSolicitationPacket(sourceIP, destIP!!, localMac)
            val result = this.node!!.processVirtualNetworkFrame(
                System.currentTimeMillis(),
                this.networkId,
                localMac,
                destMac,
                IPV6_PACKET,
                0,
                packet,
                nextDeadline
            )
            if (result != ResultCode.RESULT_OK) {
                Log.e(TAG, "Error calling processVirtualNetworkFrame: " + result.toString())
            } else {
                Log.d(TAG, "Neighbor Solicitation sent to ZT")
                this.ztService.setNextBackgroundTaskDeadline(nextDeadline[0])
            }
        }
    }

    /**
     * 停止接收线程并关闭流。
     */
    fun interrupt() {
        if (this.receiveThread != null) {
            try {
                this.input!!.close()
                this.output!!.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error stopping in/out: " + e.message, e)
            }
            this.receiveThread!!.interrupt()
            try {
                this.receiveThread!!.join()
            } catch (ignored: InterruptedException) {
            }
        }
    }

    /**
     * 等待接收线程结束。
     *
     * @throws InterruptedException 等待时线程被中断
     */
    @Throws(InterruptedException::class)
    fun join() {
        this.receiveThread!!.join()
    }

    private fun isNeighborSolicitation(packetData: ByteArray): Boolean =
        packetData[6].toInt() == 58 && packetData[40].toInt() == -121

    private fun isNeighborAdvertisement(packetData: ByteArray): Boolean =
        packetData[6].toInt() == 58 && packetData[40].toInt() == -120

    /**
     * 判断接收线程是否运行中。
     *
     * @return 运行中返回 `true`
     */
    fun isRunning(): Boolean {
        val thread = this.receiveThread ?: return false
        return thread.isAlive
    }

    /**
     * 响应并处理 ZT 网络发送至本节点的以太网帧。
     */
    override fun onVirtualNetworkFrame(
        networkId: Long,
        srcMac: Long,
        destMac: Long,
        etherType: Long,
        vlanId: Long,
        frameData: ByteArray
    ) {
        DebugLog.d(
            TAG,
            "Got Virtual Network Frame. " +
                " Network ID: " + SdkStringUtils.networkIdToString(networkId) +
                " Source MAC: " + SdkStringUtils.macAddressToString(srcMac) +
                " Dest MAC: " + SdkStringUtils.macAddressToString(destMac) +
                " Ether type: " + SdkStringUtils.etherTypeToString(etherType) +
                " VLAN ID: " + vlanId + " Frame Length: " + frameData.size
        )
        if (this.vpnSocket == null) {
            Log.e(TAG, "vpnSocket is null!")
        } else if (this.input == null || this.output == null) {
            Log.e(TAG, "no in/out streams")
        } else if (etherType == ARP_PACKET.toLong()) {
            // 收到 ARP 包。更新 ARP 表，若需要则进行应答
            Log.d(TAG, "Got ARP Packet")
            val arpReply = this.arpTable!!.processARPPacket(frameData)
            if (arpReply != null && arpReply.destMac != 0L && arpReply.destAddress != null) {
                // 获取本地 V4 地址
                val networkConfig = this.node!!.networkConfig(networkId)
                var localV4Address: InetAddress? = null
                for (address in networkConfig.assignedAddresses) {
                    if (address.address is Inet4Address) {
                        localV4Address = address.address
                        break
                    }
                }
                // 构造并返回 ARP 应答
                if (localV4Address != null) {
                    val nextDeadline = LongArray(1)
                    val packetData = this.arpTable!!.getReplyPacket(
                        networkConfig.mac,
                        localV4Address,
                        arpReply.destMac,
                        arpReply.destAddress
                    )
                    val result = this.node!!
                        .processVirtualNetworkFrame(
                            System.currentTimeMillis(), networkId,
                            networkConfig.mac, srcMac, ARP_PACKET, 0,
                            packetData, nextDeadline
                        )
                    if (result != ResultCode.RESULT_OK) {
                        Log.e(TAG, "Error sending ARP packet: " + result.toString())
                        return
                    }
                    Log.d(TAG, "ARP Reply Sent!")
                    this.ztService.setNextBackgroundTaskDeadline(nextDeadline[0])
                }
            }
        } else if (etherType == IPV4_PACKET.toLong()) {
            // 收到 IPv4 包。根据需要发送至 TUN
            DebugLog.d(TAG, "Got IPv4 packet. Length: " + frameData.size + " Bytes")
            try {
                val sourceIP = IPPacketUtils.getSourceIP(frameData)
                if (sourceIP != null) {
                    if (isIPv4Multicast(sourceIP)) {
                        val result = this.node!!.multicastSubscribe(
                            this.networkId,
                            multicastAddressToMAC(sourceIP)
                        )
                        if (result != ResultCode.RESULT_OK) {
                            Log.e(TAG, "Error when calling multicastSubscribe: $result")
                        }
                    } else {
                        this.arpTable!!.setAddress(sourceIP, srcMac)
                    }
                }
                this.output!!.write(frameData)
            } catch (e: Exception) {
                Log.e(TAG, "Error writing data to vpn socket: " + e.message, e)
            }
        } else if (etherType == IPV6_PACKET.toLong()) {
            // 收到 IPv6 包。根据需要发送至 TUN，并更新 NDP 表
            DebugLog.d(TAG, "Got IPv6 packet. Length: " + frameData.size + " Bytes")
            try {
                val sourceIP = IPPacketUtils.getSourceIP(frameData)
                if (sourceIP != null) {
                    if (isIPv6Multicast(sourceIP)) {
                        val result = this.node!!.multicastSubscribe(
                            this.networkId,
                            multicastAddressToMAC(sourceIP)
                        )
                        if (result != ResultCode.RESULT_OK) {
                            Log.e(TAG, "Error when calling multicastSubscribe: $result")
                        }
                    } else {
                        this.ndpTable!!.setAddress(sourceIP, srcMac)
                    }
                }
                this.output!!.write(frameData)
            } catch (e: Exception) {
                Log.e(TAG, "Error writing data to vpn socket: " + e.message, e)
            }
        } else if (frameData.size >= 14) {
            Log.d(
                TAG,
                "Unknown Packet Type Received: 0x" +
                    String.format("%02X%02X", frameData[12], frameData[13])
            )
        } else {
            Log.d(TAG, "Unknown Packet Received.  Packet Length: " + frameData.size)
        }
    }

    private fun routeForDestination(destAddress: InetAddress): Route? {
        synchronized(this.routeMap) {
            for (route in this.routeMap.keys) {
                if (route.belongsToRoute(destAddress)) {
                    return route
                }
            }
            return null
        }
    }

    @Suppress("unused")
    private fun networkIdForDestination(destAddress: InetAddress): Long {
        synchronized(this.routeMap) {
            for (route in this.routeMap.keys) {
                if (route.belongsToRoute(destAddress)) {
                    return this.routeMap[route]!!
                }
            }
            return 0
        }
    }

    companion object {
        /** 日志标签。 */
        const val TAG = "TunTapAdapter"

        private const val ARP_PACKET = 2054
        private const val IPV4_PACKET = 2048
        private const val IPV6_PACKET = 34525

        /**
         * 将组播地址转换为对应的 MAC 地址。
         *
         * @param inetAddress 组播地址
         * @return MAC 地址；非 IPv4/IPv6 地址时返回 0
         */
        @JvmStatic
        fun multicastAddressToMAC(inetAddress: InetAddress): Long {
            if (inetAddress is Inet4Address) {
                val address = inetAddress.address
                return ByteBuffer.wrap(
                    byteArrayOf(
                        0, 0, 1, 0, 94,
                        (address[1].toInt() and 0x7F).toByte(),
                        address[2], address[3]
                    )
                ).long
            } else if (inetAddress !is Inet6Address) {
                return 0
            } else {
                val address = inetAddress.address
                return ByteBuffer.wrap(
                    byteArrayOf(0, 0, 51, 51, address[12], address[13], address[14], address[15])
                ).long
            }
        }
    }
}
