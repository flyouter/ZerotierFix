package net.kaaass.zerotierfix.model.dto

/**
 * 网络本地配置，用于跨层传递。
 */
data class NetworkConfigDto(
    /**
     * 是否通过 ZeroTier 路由默认流量。
     */
    val isRouteViaZeroTier: Boolean,

    /**
     * DNS 模式，取值见 [net.kaaass.zerotierfix.model.enums.DNSMode]。
     */
    val dnsMode: Int,

    /**
     * 自定义 DNS 服务器地址列表；不为 `null`，无配置时为空列表。
     */
    val dnsServers: List<String>
)
