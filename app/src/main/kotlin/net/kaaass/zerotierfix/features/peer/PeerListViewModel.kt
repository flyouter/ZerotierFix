package net.kaaass.zerotierfix.features.peer

import android.os.SystemClock

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
import net.kaaass.zerotierfix.vpn.engine.ServiceState
import net.kaaass.zerotierfix.vpn.engine.VpnCommandResult
import net.kaaass.zerotierfix.vpn.engine.VpnEngine

import javax.inject.Inject

/**
 * Peer 列表页面的 ViewModel。
 *
 * 区分服务未运行、节点未就绪与请求失败三种错误，供页面展示可重试的错误态。
 * 刷新期间页面显示整屏加载态，并保证至少 [MIN_REFRESH_DURATION_MS] 的可见时长。
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
     * 首次进入页面时请求 Peer 列表，显示首屏加载态。
     */
    fun load() {
        _uiState.update { it.copy(isLoading = it.peers.isEmpty(), error = null) }
        viewModelScope.launch { requestPeers() }
    }

    /**
     * 下拉刷新或点击顶栏刷新。
     *
     * 刷新状态由请求本身驱动，并在请求结束后保证至少展示
     * [MIN_REFRESH_DURATION_MS]，避免请求瞬间完成时加载屏一闪而过。
     */
    fun refresh() {
        if (_uiState.value.isRefreshing) {
            return
        }
        _uiState.update { it.copy(isRefreshing = true, error = null) }
        viewModelScope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            try {
                requestPeers()
            } finally {
                val elapsed = SystemClock.elapsedRealtime() - startedAt
                if (elapsed < MIN_REFRESH_DURATION_MS) {
                    delay(MIN_REFRESH_DURATION_MS - elapsed)
                }
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    /**
     * 重试加载。
     */
    fun retry() {
        load()
    }

    /**
     * 请求 Peer 列表并更新页面状态。
     */
    private suspend fun requestPeers() {
        val serviceState = vpnEngine.serviceState.value
        if (serviceState != ServiceState.RUNNING && serviceState != ServiceState.STARTING) {
            _uiState.update {
                it.copy(
                    hasLoaded = true,
                    isLoading = false,
                    error = PeerListError.SERVICE_NOT_RUNNING
                )
            }
            return
        }
        when (val result = vpnEngine.loadPeers()) {
            is VpnCommandResult.Success -> _uiState.update {
                it.copy(
                    peers = result.value,
                    hasLoaded = true,
                    isLoading = false,
                    error = null
                )
            }
            VpnCommandResult.ServiceNotRunning -> _uiState.update {
                it.copy(
                    hasLoaded = true,
                    isLoading = false,
                    error = PeerListError.SERVICE_NOT_RUNNING
                )
            }
            VpnCommandResult.NodeNotReady -> {
                _uiState.update {
                    it.copy(
                        hasLoaded = true,
                        isLoading = false,
                        error = PeerListError.NODE_NOT_READY
                    )
                }
                _effects.emit(
                    PeerListEffect.ShowSnackbar(
                        UiText.Resource(R.string.fail_retrieve_peer_list)
                    )
                )
            }
            is VpnCommandResult.Failure -> _uiState.update {
                it.copy(
                    hasLoaded = true,
                    isLoading = false,
                    error = PeerListError.RETRIEVE_FAILED
                )
            }
        }
    }

    companion object {
        /** 刷新加载屏的最短展示时长。 */
        private const val MIN_REFRESH_DURATION_MS = 1000L
    }
}
