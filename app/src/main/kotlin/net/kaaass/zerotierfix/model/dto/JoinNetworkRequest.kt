package net.kaaass.zerotierfix.model.dto

/**
 * 加入 ZeroTier 网络的请求参数。
 */
data class JoinNetworkRequest(
    /**
     * 网络 ID（十进制）。
     */
    val networkId: Long,

    /**
     * 网络 ID 的 16 进制字符串，保持用户输入格式；可能为 `null`。
     */
    val networkIdStr: String?,

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
