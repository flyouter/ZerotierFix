package net.kaaass.zerotierfix.model.dto

import androidx.compose.runtime.Immutable

import java.net.InetSocketAddress

/**
 * ZeroTier 网络下发的 DNS 配置。
 */
@Immutable
data class VirtualNetworkDnsDto(
    /**
     * DNS 搜索域；可能为 `null`。
     */
    val domain: String?,

    /**
     * DNS 服务器地址列表；不为 `null`。
     */
    val servers: List<InetSocketAddress>
)
