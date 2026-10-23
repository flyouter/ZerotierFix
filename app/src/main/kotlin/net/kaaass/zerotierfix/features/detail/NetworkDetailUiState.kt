package net.kaaass.zerotierfix.features.detail

import net.kaaass.zerotierfix.model.dto.VirtualNetworkConfigDto

/**
 * 网络详情页面状态。
 *
 * @property networkId 网络 ID
 * @property networkIdStr 网络 ID 的 16 进制字符串
 * @property networkName 网络名称
 * @property dnsMode 本地配置的 DNS 模式；配置尚未加载时为 `null`
 * @property isRouteViaZeroTier 是否通过 ZeroTier 路由默认流量
 * @property virtualConfig 服务返回的实时配置；不可用时为 `null`
 * @property ipAddresses 展示用的分配地址（已按 IPv6 设置过滤，空值保留为 `"null"`）
 * @property dnsServers 展示用的 DNS 服务器地址
 * @property isLoading 是否正在加载
 */
data class NetworkDetailUiState(
    val networkId: Long = 0L,
    val networkIdStr: String = "",
    val networkName: String = "",
    val dnsMode: Int? = null,
    val isRouteViaZeroTier: Boolean = false,
    val virtualConfig: VirtualNetworkConfigDto? = null,
    val ipAddresses: List<String> = emptyList(),
    val dnsServers: List<String> = emptyList(),
    val isLoading: Boolean = true
)
