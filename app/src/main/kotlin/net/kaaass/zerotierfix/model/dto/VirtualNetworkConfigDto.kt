package net.kaaass.zerotierfix.model.dto

import net.kaaass.zerotierfix.model.enums.NetworkStatus
import net.kaaass.zerotierfix.model.enums.NetworkType
import java.net.InetSocketAddress

/**
 * ZeroTier 虚拟网络的实时配置，用于跨层传递。
 */
data class VirtualNetworkConfigDto(
    /**
     * 网络 ID（十进制）。
     */
    val networkId: Long,

    /**
     * 网络名称；可能为 `null` 或空字符串。
     */
    val networkName: String?,

    /**
     * 网络状态。
     */
    val status: NetworkStatus,

    /**
     * 网络类型。
     */
    val type: NetworkType,

    /**
     * 虚拟网卡 MAC 地址。
     */
    val mac: Long,

    /**
     * 最大传输单元。
     */
    val mtu: Int,

    /**
     * 是否启用广播。
     */
    val isBroadcastEnabled: Boolean,

    /**
     * 是否启用桥接。
     */
    val isBridge: Boolean,

    /**
     * 分配给本机的地址列表；不为 `null`。
     */
    val assignedAddresses: List<InetSocketAddress>,

    /**
     * 网络下发的 DNS 配置；未配置时为 `null`。
     */
    val dns: VirtualNetworkDnsDto?
)
