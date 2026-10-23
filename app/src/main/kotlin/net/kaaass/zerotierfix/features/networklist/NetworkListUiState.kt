package net.kaaass.zerotierfix.features.networklist

import net.kaaass.zerotierfix.model.dto.NetworkSummary
import net.kaaass.zerotierfix.model.dto.NodeStatusDto
import net.kaaass.zerotierfix.ui.common.UiText

/**
 * 网络列表页面状态。
 *
 * @property networks 已加入的网络列表
 * @property connectedNetworkId 当前连接的网络 ID；无连接时为 `null`
 * @property nodeId 本机节点 ID 字符串
 * @property nodeStatus 节点状态；服务未启动时为 `null`
 * @property clientVersion ZeroTier 客户端版本；尚未取得时为 `null`
 * @property isLoading 首次加载是否进行中
 * @property isNotificationAlertVisible 是否显示通知权限提示
 * @property appVersion 应用版本号
 */
data class NetworkListUiState(
    val networks: List<NetworkSummary> = emptyList(),
    val connectedNetworkId: Long? = null,
    val nodeId: String? = null,
    val nodeStatus: NodeStatusDto? = null,
    val clientVersion: String? = null,
    val isLoading: Boolean = true,
    val isNotificationAlertVisible: Boolean = false,
    val appVersion: String = ""
)

/**
 * 网络列表的一次性事件。
 */
sealed interface NetworkListEffect {

    /**
     * 显示 Toast。
     *
     * @property text 文本
     */
    data class ShowToast(val text: UiText) : NetworkListEffect

    /**
     * 请求 VPN 授权。
     *
     * @property networkId 授权通过后要连接的网络 ID
     */
    data class RequestVpnConsent(val networkId: Long) : NetworkListEffect

    /**
     * 打开系统通知设置。
     */
    data object OpenNotificationSettings : NetworkListEffect
}
