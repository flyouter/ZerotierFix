package net.kaaass.zerotierfix.features.peer

import androidx.compose.runtime.Immutable

import net.kaaass.zerotierfix.model.dto.PeerInfoDto
import net.kaaass.zerotierfix.ui.common.UiText

/**
 * Peer 列表加载错误类型。
 */
enum class PeerListError {
    /** VPN 服务未运行。 */
    SERVICE_NOT_RUNNING,

    /** 节点尚未就绪。 */
    NODE_NOT_READY,

    /** 请求失败。 */
    RETRIEVE_FAILED
}

/**
 * Peer 列表页面状态。
 *
 * @property peers 当前 Peer 列表
 * @property isRefreshing 下拉刷新是否进行中
 * @property hasLoaded 是否已收到过应答
 * @property isLoading 首次加载是否进行中
 * @property error 最近一次加载错误；`null` 表示没有错误
 */
@Immutable
data class PeerListUiState(
    val peers: List<PeerInfoDto> = emptyList(),
    val isRefreshing: Boolean = false,
    val hasLoaded: Boolean = false,
    val isLoading: Boolean = true,
    val error: PeerListError? = null
) {

    /** 直连 Peer 数量。 */
    val directCount: Int
        get() = peers.count { it.preferredPath != null }

    /** 中继 Peer 数量。 */
    val relayCount: Int
        get() = peers.size - directCount

    /** 平均延迟（毫秒）；无 Peer 时为 `null`。 */
    val averageLatency: Int?
        get() = if (peers.isEmpty()) null else peers.sumOf { it.latency } / peers.size
}

/**
 * Peer 列表页面的一次性事件。
 */
sealed interface PeerListEffect {

    /**
     * 显示 Snackbar。
     *
     * @property text 文本
     */
    data class ShowSnackbar(val text: UiText) : PeerListEffect
}
