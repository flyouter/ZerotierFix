package net.kaaass.zerotierfix.features.join

import net.kaaass.zerotierfix.model.enums.DNSMode
import net.kaaass.zerotierfix.ui.common.UiText

/**
 * 添加网络页面状态。
 *
 * @property networkId 网络 ID 输入值（最多 16 位 16 进制字符）
 * @property isNetworkIdValid 网络 ID 是否为合法的 16 位
 * @property isRouteViaZeroTier 是否通过 ZeroTier 路由全部流量
 * @property dnsMode DNS 模式，取值见 [DNSMode]
 * @property dnsV4Primary 自定义 DNS：第一个 IPv4 地址
 * @property dnsV4Secondary 自定义 DNS：第二个 IPv4 地址
 * @property dnsV6Primary 自定义 DNS：第一个 IPv6 地址
 * @property dnsV6Secondary 自定义 DNS：第二个 IPv6 地址
 * @property isSubmitting 是否正在提交
 */
data class JoinNetworkUiState(
    val networkId: String = "",
    val isNetworkIdValid: Boolean = false,
    val isRouteViaZeroTier: Boolean = false,
    val dnsMode: Int = DNSMode.NO_DNS.toInt(),
    val dnsV4Primary: String = "",
    val dnsV4Secondary: String = "",
    val dnsV6Primary: String = "",
    val dnsV6Secondary: String = "",
    val isSubmitting: Boolean = false
)

/**
 * 添加网络页面的一次性事件。
 */
sealed interface JoinNetworkEffect {

    /**
     * 显示 Toast。
     *
     * @property text 文本
     */
    data class ShowToast(val text: UiText) : JoinNetworkEffect

    /**
     * 返回上一页。
     */
    data object NavigateBack : JoinNetworkEffect
}
