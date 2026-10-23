package net.kaaass.zerotierfix.vpn.tun

import android.util.Log

import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.HashMap

/**
 * ARP 表，维护 MAC 与 IPv4 地址映射并在超时后清理。
 */
class ARPTable {

    private val entriesMap = HashMap<Long, ARPEntry>()
    private val inetAddressToMacAddress = HashMap<InetAddress, Long>()
    private val ipEntriesMap = HashMap<InetAddress, ARPEntry>()
    private val macAddressToInetAdddress = HashMap<Long, InetAddress>()

    private val timeoutThread: Thread = object : Thread("ARP Timeout Thread") {
        override fun run() {
            while (!isInterrupted) {
                try {
                    for (arpEntry in HashMap(this@ARPTable.entriesMap).values) {
                        if (arpEntry.time + ENTRY_TIMEOUT < System.currentTimeMillis()) {
                            Log.d(TAG, "Removing " + arpEntry.address.toString() + " from ARP cache")
                            synchronized(this@ARPTable.macAddressToInetAdddress) {
                                this@ARPTable.macAddressToInetAdddress.remove(arpEntry.mac)
                            }
                            synchronized(this@ARPTable.inetAddressToMacAddress) {
                                this@ARPTable.inetAddressToMacAddress.remove(arpEntry.address)
                            }
                            synchronized(this@ARPTable.entriesMap) {
                                this@ARPTable.entriesMap.remove(arpEntry.mac)
                            }
                            synchronized(this@ARPTable.ipEntriesMap) {
                                this@ARPTable.ipEntriesMap.remove(arpEntry.address)
                            }
                        }
                    }
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    Log.e(TAG, "Tun/Tap Interrupted", e)
                    break
                } catch (e: Exception) {
                    Log.d(TAG, e.toString())
                }
            }
            Log.d(TAG, "ARP Timeout Thread Ended.")
        }
    }

    init {
        this.timeoutThread.start()
    }

    /**
     * 停止超时清理线程。
     */
    fun stop() {
        try {
            this.timeoutThread.interrupt()
            this.timeoutThread.join()
        } catch (ignored: InterruptedException) {
        }
    }

    /**
     * 记录地址与 MAC 的映射。
     *
     * @param inetAddress IPv4 地址
     * @param mac MAC 地址
     */
    internal fun setAddress(inetAddress: InetAddress, mac: Long) {
        synchronized(this.inetAddressToMacAddress) {
            this.inetAddressToMacAddress[inetAddress] = mac
        }
        synchronized(this.macAddressToInetAdddress) {
            this.macAddressToInetAdddress[mac] = inetAddress
        }
        val arpEntry = ARPEntry(mac, inetAddress)
        synchronized(this.entriesMap) {
            this.entriesMap[mac] = arpEntry
        }
        synchronized(this.ipEntriesMap) {
            this.ipEntriesMap[inetAddress] = arpEntry
        }
    }

    private fun updateArpEntryTime(mac: Long) {
        synchronized(this.entriesMap) {
            this.entriesMap[mac]?.updateTime()
        }
    }

    private fun updateArpEntryTime(inetAddress: InetAddress) {
        synchronized(this.ipEntriesMap) {
            this.ipEntriesMap[inetAddress]?.updateTime()
        }
    }

    /**
     * 查询地址对应的 MAC。
     *
     * @param inetAddress IPv4 地址
     * @return MAC 地址；不存在时返回 -1
     */
    internal fun getMacForAddress(inetAddress: InetAddress): Long {
        synchronized(this.inetAddressToMacAddress) {
            if (!this.inetAddressToMacAddress.containsKey(inetAddress)) {
                return -1
            }
            Log.d(TAG, "Returning MAC for " + inetAddress.toString())
            val longValue = this.inetAddressToMacAddress[inetAddress]
            if (longValue != null) {
                updateArpEntryTime(longValue)
                return longValue
            }
        }
        return -1
    }

    /**
     * 查询 MAC 对应的地址。
     *
     * @param mac MAC 地址
     * @return IPv4 地址；不存在时返回 `null`
     */
    internal fun getAddressForMac(mac: Long): InetAddress? {
        synchronized(this.macAddressToInetAdddress) {
            if (!this.macAddressToInetAdddress.containsKey(mac)) {
                return null
            }
            val inetAddress = this.macAddressToInetAdddress[mac]!!
            updateArpEntryTime(inetAddress)
            return inetAddress
        }
    }

    /**
     * 判断是否存在地址对应的 MAC。
     *
     * @param inetAddress IPv4 地址
     * @return 存在返回 `true`
     */
    fun hasMacForAddress(inetAddress: InetAddress): Boolean =
        synchronized(this.inetAddressToMacAddress) {
            this.inetAddressToMacAddress.containsKey(inetAddress)
        }

    /**
     * 判断是否存在 MAC 对应的地址。
     *
     * @param mac MAC 地址
     * @return 存在返回 `true`
     */
    fun hasAddressForMac(mac: Long): Boolean =
        synchronized(this.macAddressToInetAdddress) {
            this.macAddressToInetAdddress.containsKey(mac)
        }

    /**
     * 构造 ARP 请求报文。
     *
     * @param mac 源 MAC
     * @param inetAddress 源 IPv4 地址
     * @param inetAddress2 目标 IPv4 地址
     * @return ARP 报文
     */
    fun getRequestPacket(mac: Long, inetAddress: InetAddress, inetAddress2: InetAddress): ByteArray =
        getARPPacket(REQUEST, mac, 0, inetAddress, inetAddress2)

    /**
     * 构造 ARP 应答报文。
     *
     * @param mac 源 MAC
     * @param inetAddress 源 IPv4 地址
     * @param mac2 目标 MAC
     * @param inetAddress2 目标 IPv4 地址
     * @return ARP 报文
     */
    fun getReplyPacket(
        mac: Long,
        inetAddress: InetAddress,
        mac2: Long,
        inetAddress2: InetAddress
    ): ByteArray = getARPPacket(REPLY, mac, mac2, inetAddress, inetAddress2)

    /**
     * 构造 ARP 报文。
     *
     * @param type 报文类型
     * @param mac 源 MAC
     * @param mac2 目标 MAC
     * @param inetAddress 源 IPv4 地址
     * @param inetAddress2 目标 IPv4 地址
     * @return ARP 报文
     */
    fun getARPPacket(
        type: Int,
        mac: Long,
        mac2: Long,
        inetAddress: InetAddress,
        inetAddress2: InetAddress
    ): ByteArray {
        val packet = ByteArray(28)
        packet[0] = 0
        packet[1] = 1
        packet[2] = 8
        packet[3] = 0
        packet[4] = 6
        packet[5] = 4
        packet[6] = 0
        packet[7] = type.toByte()
        System.arraycopy(longToBytes(mac), 2, packet, 8, 6)
        System.arraycopy(inetAddress.address, 0, packet, 14, 4)
        System.arraycopy(longToBytes(mac2), 2, packet, 18, 6)
        System.arraycopy(inetAddress2.address, 0, packet, 24, 4)
        return packet
    }

    /**
     * 解析并处理 ARP 报文，更新表项。
     *
     * @param packetData ARP 报文
     * @return 需要应答时返回应答数据；否则返回 `null`
     */
    fun processARPPacket(packetData: ByteArray): ARPReplyData? {
        Log.d(TAG, "Processing ARP packet")

        // 解析包内 IP、MAC 地址
        val rawSrcMac = ByteArray(8)
        System.arraycopy(packetData, 8, rawSrcMac, 2, 6)
        val rawSrcAddress = ByteArray(4)
        System.arraycopy(packetData, 14, rawSrcAddress, 0, 4)
        val rawDstMac = ByteArray(8)
        System.arraycopy(packetData, 18, rawDstMac, 2, 6)
        val rawDstAddress = ByteArray(4)
        System.arraycopy(packetData, 24, rawDstAddress, 0, 4)
        val srcAddress: InetAddress? = try {
            InetAddress.getByAddress(rawSrcAddress)
        } catch (unused: Exception) {
            null
        }
        val dstAddress: InetAddress? = try {
            InetAddress.getByAddress(rawDstAddress)
        } catch (unused: Exception) {
            null
        }
        val srcMac = ByteBuffer.wrap(rawSrcMac).long
        val dstMac = ByteBuffer.wrap(rawDstMac).long

        // 更新 ARP 表项
        if (srcMac != 0L && srcAddress != null) {
            setAddress(srcAddress, srcMac)
        }
        if (dstMac != 0L && dstAddress != null) {
            setAddress(dstAddress, dstMac)
        }

        // 处理响应行为
        val packetType = packetData[7]
        return if (packetType.toInt() == REQUEST) {
            // ARP 请求，返回应答数据
            Log.d(TAG, "Reply needed")
            ARPReplyData(srcMac, srcAddress)
        } else {
            null
        }
    }

    companion object {
        /** 日志标签。 */
        const val TAG = "ARPTable"

        private const val ENTRY_TIMEOUT = 120000L
        private const val REPLY = 2
        private const val REQUEST = 1

        /**
         * 将 `long` 转为 8 字节数组。
         *
         * @param value 待转换值
         * @return 大端字节数组
         */
        @JvmStatic
        fun longToBytes(value: Long): ByteArray {
            val allocate = ByteBuffer.allocate(8)
            allocate.putLong(value)
            return allocate.array()
        }
    }
}
