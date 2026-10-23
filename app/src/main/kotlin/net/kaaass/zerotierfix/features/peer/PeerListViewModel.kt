package net.kaaass.zerotierfix.features.peer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import net.kaaass.zerotierfix.R
import net.kaaass.zerotierfix.ui.common.UiText
import net.kaaass.zerotierfix.vpn.engine.VpnCommandResult
import net.kaaass.zerotierfix.vpn.engine.VpnEngine

import javax.inject.Inject

/**
 * Peer 列表页面的 ViewModel。
 *
 * 保留原行为的差异：服务未运行时静默无响应；节点未就绪时提示
 * `fail_retrieve_peer_list`；下拉刷新最多显示 1 秒加载状态。
 */
@HiltViewModel
class PeerListViewModel @Inject constructor(
    private val vpnEngine: VpnEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(PeerListUiState())

    /** 页面状态。 */
    val uiState: StateFlow<PeerListUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<PeerListEffect>(extraBufferCapacity = 4)

    /** 一次性事件。 */
    val effects: SharedFlow<PeerListEffect> = _effects.asSharedFlow()

    /**
     * 首次进入页面时请求 Peer 列表，不显示下拉刷新指示器。
     */
    fun load() {
        viewModelScope.launch { requestPeers() }
    }

    /**
     * 下拉刷新。
     *
     * 请求结果返回前最多显示 1 秒加载状态，与原实现的定时器一致。
     */
    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch { requestPeers() }
        viewModelScope.launch {
            delay(REFRESH_TIMEOUT_MS)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    /**
     * 请求 Peer 列表并更新页面状态。
     */
    private suspend fun requestPeers() {
        when (val result = vpnEngine.loadPeers()) {
            is VpnCommandResult.Success -> _uiState.update {
                it.copy(peers = result.value, isRefreshing = false, hasLoaded = true)
            }
            VpnCommandResult.NodeNotReady -> {
                _uiState.update { it.copy(hasLoaded = true) }
                _effects.emit(
                    PeerListEffect.ShowSnackbar(
                        UiText.Resource(R.string.fail_retrieve_peer_list)
                    )
                )
            }
            else -> _uiState.update { it.copy(hasLoaded = true) }
        }
    }

    companion object {
        private const val REFRESH_TIMEOUT_MS = 1000L
    }
}
