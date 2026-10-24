package net.kaaass.zerotierfix.features.networklist

import androidx.compose.runtime.Immutable

import net.kaaass.zerotierfix.model.dto.NetworkSummary
import net.kaaass.zerotierfix.model.dto.NodeStatusDto
import net.kaaass.zerotierfix.model.enums.NetworkStatus
import net.kaaass.zerotierfix.ui.common.UiText
import net.kaaass.zerotierfix.vpn.engine.ServiceState

/**
 * 网络列表中的单个条目。
 *
 * @property summary 数据库中的网络概要
 * @property runtimeStatus 引擎实时状态；`null` 表示未连接或尚无实时配置
 * @property ipAddresses 已按 IPv6 设置过滤的分配地址
 * @property displayName 展示用网络名：优先数据库名称，其次引擎下发的名称
 */
@Immutable
data class NetworkListItem(
    val summary: NetworkSummary,
    val runtimeStatus: NetworkStatus?,
    val ipAddresses: List<String>,
    val displayName: String?
)

/**
 * 网络列表页面状态。
 *
 * @property networks 已加入的网络列表
 * @property connectedNetworkId 当前连接的网络 ID；无连接时为 `null`
 * @property operatingNetworkId 正在连接 / 断开 / 删除的网络 ID；空闲时为 `null`
 * @property serviceState VPN 服务生命周期状态
 * @property nodeId 本机节点 ID 字符串
 * @property nodeStatus 节点状态；服务未启动时为 `null`
 * @property clientVersion ZeroTier 客户端版本；尚未取得时为 `null`
 * @property isLoading 首次加载是否进行中
 * @property isNotificationAlertVisible 是否显示通知权限提示
 * @property appVersion 应用版本号
 */
@Immutable
data class NetworkListUiState(
    val networks: List<NetworkListItem> = emptyList(),
    val connectedNetworkId: Long? = null,
    val operatingNetworkId: Long? = null,
    val serviceState: ServiceState = ServiceState.UNKNOWN,
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
