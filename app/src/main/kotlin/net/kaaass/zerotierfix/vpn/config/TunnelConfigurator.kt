package net.kaaass.zerotierfix.vpn.config

import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log

import com.zerotier.sdk.Node
import com.zerotier.sdk.ResultCode
import com.zerotier.sdk.VirtualNetworkConfig
import com.zerotier.sdk.util.StringUtils as SdkStringUtils

import net.kaaass.zerotierfix.data.repository.NetworkRepository
import net.kaaass.zerotierfix.model.dto.NetworkSummary
import net.kaaass.zerotierfix.util.Constants
import net.kaaass.zerotierfix.util.InetAddressUtils
import net.kaaass.zerotierfix.vpn.route.Route
import net.kaaass.zerotierfix.vpn.tun.TunTapAdapter

import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer

/**
 * 隧道配置过程需要由宿主（`ZeroTierOneService`）提供的平台能力。
 *
 * 通过接口隔离，`vpn/config` 无需依赖 `service` 包。
 */
interface TunnelHost {

    /** 用于创建 [VpnService.Builder] 的 VPN 服务。 */
    val vpnService: VpnService

    /** 当前 ZeroTier 节点；未初始化时为 `null`。 */
    val currentNode: Node?

    /** 当前 TUN/TAP 适配器；未初始化时为 `null`。 */
    val currentAdapter: TunTapAdapter?

    /** 是否已请求停止；为 `true` 时不得建立隧道或发送通知。 */
    val isStopping: Boolean

    /** VPN 未授权或已撤销时的错误消息。 */
    val vpnNotPreparedMessage: String

    /**
     * 读取指定网络的实时配置。
     *
     * @param networkId 网络 ID
     * @return 实时配置；尚未收到时为 `null`
     */
    fun readVirtualNetworkConfig(networkId: Long): VirtualNetworkConfig?

    /**
     * 停止适配器线程。
     */
    fun restartTunnelAdapter()

    /**
     * 关闭旧的 VPN socket 与输入输出流。
     */
    fun closeVpnResources()

    /**
     * 保存新建立的 VPN socket 与流，并启动适配器线程。
     *
     * @param socket 新建的 VPN socket
     * @param input VPN 输入流
     * @param output VPN 输出流
     */
    fun onTunnelEstablished(
        socket: ParcelFileDescriptor,
        input: FileInputStream,
        output: FileOutputStream
    )

    /**
     * 显示已连接通知。
     *
     * @param networkIdStr 当前网络的 16 进制字符串
     * @param networkName 当前网络名称；可能为 `null`
     */
    fun showConnectedNotification(networkIdStr: String?, networkName: String?)

    /**
     * 在旧版本 Android 上启动多播扫描线程。
     *
     * @param disableIpv6 是否禁用 IPv6
     */
    fun startMulticastScanners(disableIpv6: Boolean)

    /**
     * 上报隧道错误。
     *
     * @param message 错误消息
     */
    fun emitTunnelError(message: String)
}

/**
 * 根据网络配置重建 TUN/TAP 与 VPN socket。
 *
 * 方法为同步阻塞调用，应在后台线程执行；不持有数据库锁。
 */
class TunnelConfigurator(
    private val host: TunnelHost,
    private val networkRepository: NetworkRepository
) {

    /**
     * 判断配置中是否存在可用于建立隧道的分配地址。
     *
     * 与 [configure] 使用同一套过滤规则，避免“状态显示已连接”与“实际能建隧道”
     * 口径不一致。
     *
     * @param config 虚拟网络配置
     * @param disableIpv6 是否禁用 IPv6
     * @return 至少有一个可用地址时返回 `true`
     */
    fun hasUsableAssignedAddresses(
        config: VirtualNetworkConfig,
        disableIpv6: Boolean
    ): Boolean {
        val addresses = config.assignedAddresses ?: return false
        return addresses.any { vpnAddress ->
            routeFor(vpnAddress, disableIpv6) != null
        }
    }

    /**
     * 计算分配地址对应的路由；地址不可用时返回 `null`。
     */
    private fun routeFor(
        vpnAddress: InetSocketAddress,
        disableIpv6: Boolean
    ): InetAddress? {
        if (disableIpv6 && vpnAddress.address is Inet6Address) {
            return null
        }
        return InetAddressUtils.addressToRoute(vpnAddress.address, vpnAddress.port)
    }

    /**
     * 应用指定网络的最新配置。
     *
     * 网络刚加入、尚未分配地址时不会调用 `establish()`，而是等待下一次配置更新。
     *
     * @param network 数据库中的网络摘要
     * @param disableIpv6 是否禁用 IPv6
     * @param allowDefaultRoute 是否允许安装默认路由（ZeroTier 出口点）；
     *   为 `false` 时跳过 `0.0.0.0/0`、`::/0`，流量直接走物理网络
     * @return 配置成功返回 `true`；无可用地址或建立失败返回 `false`
     */
    fun configure(
        network: NetworkSummary,
        disableIpv6: Boolean,
        allowDefaultRoute: Boolean
    ): Boolean {
        // 停止流程已开始：不得再建立隧道，否则会在停止后重新拉起 VPN。
        if (host.isStopping) {
            Log.i(TAG, "Service is stopping; skip tunnel configuration")
            return false
        }
        val node = host.currentNode ?: return false
        val networkId = network.networkId
        val networkConfig = networkRepository.getNetworkConfig(networkId)
        if (networkConfig == null) {
            Log.e(TAG, "Network config not found: $networkId")
            return false
        }
        val virtualNetworkConfig = host.readVirtualNetworkConfig(networkId) ?: return false
        val adapter = host.currentAdapter ?: return false

        // Android 13+ 要求 establish() 前至少添加一个地址。网络刚加入时
        // 可能还没有分配地址，此时先不重建隧道，等待下一次 CONFIG_UPDATE。
        val assignedAddresses = virtualNetworkConfig.assignedAddresses
        Log.i(TAG, "address length: " + assignedAddresses.size)
        val vpnAddresses = assignedAddresses.mapNotNull { vpnAddress ->
            val route = routeFor(vpnAddress, disableIpv6)
            if (route == null) {
                // 禁用 IPv6 是主动过滤，不算错误；其余情况记录以便排查。
                if (!(disableIpv6 && vpnAddress.address is Inet6Address)) {
                    Log.e(TAG, "NULL route calculated!")
                }
                null
            } else {
                vpnAddress to route
            }
        }
        if (vpnAddresses.isEmpty()) {
            Log.i(TAG, "No usable assigned address; defer tunnel configuration")
            return false
        }

        // 重启 TUN TAP
        host.restartTunnelAdapter()
        adapter.clearRouteMap()

        // 重启 VPN Socket
        host.closeVpnResources()

        // 配置 VPN
        Log.i(TAG, "Configuring VpnService.Builder")
        val builder = host.vpnService.Builder()
        val isRouteViaZeroTier = networkConfig.isRouteViaZeroTier

        // 遍历 ZT 网络中当前设备的 IP 地址，组播配置
        for ((vpnAddress, route) in vpnAddresses) {
            Log.d(
                TAG,
                "Adding VPN Address: " + vpnAddress.address +
                    " Mac: " + SdkStringUtils.macAddressToString(virtualNetworkConfig.mac)
            )
            val rawAddress = vpnAddress.address.address
            val address = vpnAddress.address
            val port = vpnAddress.port

            // 计算 VPN 地址相关的组播 MAC 与 ADI
            val multicastGroup: Long
            val multicastAdi: Long
            if (rawAddress.size == 4) {
                // IPv4
                multicastGroup = InetAddressUtils.BROADCAST_MAC_ADDRESS
                multicastAdi = ByteBuffer.wrap(rawAddress).int.toLong()
            } else {
                // IPv6
                multicastGroup = ByteBuffer.wrap(
                    byteArrayOf(
                        0, 0, 0x33, 0x33, 0xFF.toByte(),
                        rawAddress[13], rawAddress[14], rawAddress[15]
                    )
                ).long
                multicastAdi = 0L
            }

            // 订阅组播并添加至 TUN TAP 路由
            val result = node.multicastSubscribe(networkId, multicastGroup, multicastAdi)
            if (result != ResultCode.RESULT_OK) {
                Log.e(TAG, "Error joining multicast group")
            } else {
                Log.d(TAG, "Joined multicast group")
            }
            builder.addAddress(address, port)
            builder.addRoute(route, port)
            adapter.addRouteAndNetwork(Route(route, port), networkId)
        }

        // 遍历网络的路由规则，将网络负责路由的地址路由至 VPN
        try {
            val v4Loopback = InetAddress.getByName("0.0.0.0")
            val v6Loopback = InetAddress.getByName("::")
            val routes = virtualNetworkConfig.routes
            if (routes.isNotEmpty()) {
                for (routeConfig in routes) {
                    val target = routeConfig.target
                    val via = routeConfig.via
                    val targetAddress = target.address
                    val targetPort = target.port
                    val viaAddress = InetAddressUtils.addressToRoute(targetAddress, targetPort)

                    val isIPv6Route =
                        (targetAddress is Inet6Address) || (viaAddress is Inet6Address)
                    val isDisabledV6Route = disableIpv6 && isIPv6Route
                    val shouldRouteToZerotier = viaAddress != null && (
                        isRouteViaZeroTier ||
                            (viaAddress != v4Loopback && viaAddress != v6Loopback)
                        )
                    if (!isDisabledV6Route && shouldRouteToZerotier) {
                        // targetPort 为 0 表示默认路由，即经 ZeroTier 出口点转发全部流量。
                        if (targetPort == 0 && !allowDefaultRoute) {
                            Log.i(TAG, "Skip default route: ZeroTier exit not allowed")
                            continue
                        }
                        builder.addRoute(viaAddress!!, targetPort)
                        val route = Route(viaAddress, targetPort)
                        if (via != null) {
                            route.gateway = via.address
                        }
                        adapter.addRouteAndNetwork(route, networkId)
                    }
                }
            }
            builder.addRoute(InetAddress.getByName("224.0.0.0"), 4)
        } catch (e: Exception) {
            host.emitTunnelError(e.localizedMessage ?: "")
            return false
        }

        if (Build.VERSION.SDK_INT >= 29) {
            builder.setMetered(false)
        }
        DnsConfigurator.addDnsServers(
            builder, networkConfig, virtualNetworkConfig, disableIpv6
        )

        // 配置 MTU
        var mtu = virtualNetworkConfig.mtu
        Log.i(TAG, "MTU from Network Config: $mtu")
        if (mtu == 0) {
            mtu = 2800
        }
        Log.i(TAG, "MTU Set: $mtu")
        builder.setMtu(mtu)

        builder.setSession(Constants.VPN_SESSION_NAME)

        // 设置部分 APP 不经过 VPN
        if (!isRouteViaZeroTier && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (app in DISALLOWED_APPS) {
                try {
                    builder.addDisallowedApplication(app)
                } catch (e: Exception) {
                    Log.e(TAG, "Cannot disallow app", e)
                }
            }
        }

        // 建立 VPN 连接
        val socket = builder.establish()
        if (socket == null) {
            host.emitTunnelError(host.vpnNotPreparedMessage)
            return false
        }
        // establish() 无法中断；若期间已开始停止，立即关闭刚建立的隧道。
        if (host.isStopping) {
            Log.i(TAG, "Service stopped during establish; close new tunnel")
            try {
                socket.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing tunnel socket", e)
            }
            return false
        }
        val input = FileInputStream(socket.fileDescriptor)
        val output = FileOutputStream(socket.fileDescriptor)
        host.onTunnelEstablished(socket, input, output)

        // onTunnelEstablished 可能已因停止而关闭隧道，此时不再发通知。
        if (host.isStopping) {
            return false
        }

        // 状态栏提示
        host.showConnectedNotification(network.networkIdStr, network.networkName)
        Log.i(TAG, "ZeroTier One Connected")

        // 旧版本 Android 多播处理
        host.startMulticastScanners(disableIpv6)
        return true
    }

    companion object {
        private val DISALLOWED_APPS = arrayOf("com.android.vending")
        private const val TAG = "TunnelConfigurator"
    }
}
