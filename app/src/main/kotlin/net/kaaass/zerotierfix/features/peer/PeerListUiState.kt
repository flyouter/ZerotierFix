package net.kaaass.zerotierfix.features.peer

import net.kaaass.zerotierfix.model.dto.PeerInfoDto
import net.kaaass.zerotierfix.ui.common.UiText

/**
 * Peer 列表页面状态。
 *
 * @property peers 当前 Peer 列表
 * @property isRefreshing 下拉刷新是否进行中
 * @property hasLoaded 是否已收到过应答
 */
data class PeerListUiState(
    val peers: List<PeerInfoDto> = emptyList(),
    val isRefreshing: Boolean = false,
    val hasLoaded: Boolean = false
)

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
