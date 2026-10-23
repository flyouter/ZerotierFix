package net.kaaass.zerotierfix.features.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import net.kaaass.zerotierfix.data.repository.NetworkRepository
import net.kaaass.zerotierfix.data.repository.SettingsRepository
import net.kaaass.zerotierfix.di.IoDispatcher
import net.kaaass.zerotierfix.model.dto.VirtualNetworkConfigDto
import net.kaaass.zerotierfix.navigation.ZeroTierRoutes
import net.kaaass.zerotierfix.vpn.engine.VpnCommandResult
import net.kaaass.zerotierfix.vpn.engine.VpnEngine

import java.net.Inet6Address
import java.net.InetSocketAddress

import javax.inject.Inject

/**
 * 网络详情页面的 ViewModel。
 *
 * 本地配置来自 Repository，实时配置来自 [VpnEngine]；实时配置优先于快照，
 * 以复刻原 `VirtualNetworkConfigReplyEvent` 与 `VirtualNetworkConfigChangedEvent`
 * 的覆盖顺序。
 */
@HiltViewModel
class NetworkDetailViewModel @Inject constructor(
    private val networkRepository: NetworkRepository,
    private val settingsRepository: SettingsRepository,
    private val vpnEngine: VpnEngine,
    savedStateHandle: SavedStateHandle,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val networkId: Long =
        checkNotNull(savedStateHandle[ZeroTierRoutes.DETAIL_ARG_NETWORK_ID])

    private val _uiState = MutableStateFlow(NetworkDetailUiState(networkId = networkId))

    /** 页面状态。 */
    val uiState: StateFlow<NetworkDetailUiState> = _uiState.asStateFlow()

    init {
        observeVirtualNetworkConfig()
    }

    /**
     * 加载本地配置与实时配置。
     *
     * 线程：数据库读取切换到 IO 线程。
     */
    fun load() {
        viewModelScope.launch {
            val summary = withContext(ioDispatcher) {
                networkRepository.getNetworkSummary(networkId)
            } ?: return@launch
            val config = withContext(ioDispatcher) {
                networkRepository.getNetworkConfig(networkId)
            } ?: return@launch
            _uiState.update {
                it.copy(
                    networkIdStr = summary.networkIdStr ?: "",
                    networkName = summary.networkName.orEmpty(),
                    dnsMode = config.dnsMode,
                    isRouteViaZeroTier = config.isRouteViaZeroTier,
                    isLoading = false
                )
            }
            // 复刻旧实现中 setChecked(true) 触发的重配副作用，仅一次
            if (config.isRouteViaZeroTier) {
                vpnEngine.reconfigure(networkId)
            }
            when (val result = vpnEngine.virtualNetworkConfig(networkId)) {
                is VpnCommandResult.Success -> applyVirtualConfig(result.value)
                else -> Unit
            }
        }
    }

    /**
     * 更新“通过 ZeroTier 路由”选项。
     *
     * 数据库写入后请求服务重配隧道；与旧实现一致，仅影响服务当前的网络。
     *
     * @param enabled 是否通过 ZeroTier 路由
     */
    fun onRouteViaZeroTierChanged(enabled: Boolean) {
        val state = _uiState.value
        if (state.dnsMode == null || state.isRouteViaZeroTier == enabled) {
            return
        }
        _uiState.update { it.copy(isRouteViaZeroTier = enabled) }
        viewModelScope.launch {
            withContext(ioDispatcher) {
                networkRepository.updateRouteViaZeroTier(networkId, enabled)
            }
            vpnEngine.reconfigure(networkId)
        }
    }

    /**
     * 订阅实时配置变化。
     */
    private fun observeVirtualNetworkConfig() {
        viewModelScope.launch {
            vpnEngine.virtualNetworkConfigs.collect { configs ->
                val config = configs?.get(networkId) ?: return@collect
                applyVirtualConfig(config)
            }
        }
    }

    /**
     * 应用实时配置并刷新展示字段。
     *
     * @param config 实时网络配置
     */
    private fun applyVirtualConfig(config: VirtualNetworkConfigDto) {
        if (config.networkId != networkId) {
            return
        }
        val disableIpv6 = settingsRepository.isIpv6Disabled
        _uiState.update { state ->
            state.copy(
                virtualConfig = config,
                networkName = config.networkName?.takeIf { it.isNotEmpty() } ?: state.networkName,
                ipAddresses = config.assignedAddresses.map {
                    formatAddress(it, disableIpv6) ?: "null"
                },
                dnsServers = config.dns?.servers?.map {
                    formatAddress(it, disableIpv6) ?: "null"
                } ?: emptyList()
            )
        }
    }

    /**
     * 将套接字地址格式化为 `地址/前缀`。
     *
     * @param address 套接字地址
     * @param disableIpv6 是否过滤 IPv6 地址
     * @return 格式化字符串；地址不可用或被过滤时返回 `null`
     */
    private fun formatAddress(address: InetSocketAddress, disableIpv6: Boolean): String? {
        val inetAddress = address.address ?: return null
        if (disableIpv6 && inetAddress is Inet6Address) {
            return null
        }
        var text = inetAddress.toString()
        if (text.startsWith("/")) {
            text = text.substring(1)
        }
        return "$text/${address.port}"
    }
}
