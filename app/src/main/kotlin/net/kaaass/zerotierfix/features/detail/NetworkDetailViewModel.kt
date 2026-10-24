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
import net.kaaass.zerotierfix.model.enums.DNSMode
import net.kaaass.zerotierfix.navigation.ZeroTierRoutes
import net.kaaass.zerotierfix.util.StringUtils
import net.kaaass.zerotierfix.vpn.engine.ServiceState
import net.kaaass.zerotierfix.vpn.engine.VpnCommandResult
import net.kaaass.zerotierfix.vpn.engine.VpnEngine

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
        observeServiceState()
    }

    /**
     * 加载本地配置与实时配置。
     *
     * 线程：数据库读取切换到 IO 线程。
     */
    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isNotFound = false) }
            val summary = withContext(ioDispatcher) {
                networkRepository.getNetworkSummary(networkId)
            }
            val config = withContext(ioDispatcher) {
                networkRepository.getNetworkConfig(networkId)
            }
            if (summary == null || config == null) {
                _uiState.update { it.copy(isLoading = false, isNotFound = true) }
                return@launch
            }
            val dnsMode = parseDnsMode(config.dnsMode)
            _uiState.update {
                it.copy(
                    networkIdStr = summary.networkIdStr ?: "",
                    networkName = summary.networkName.orEmpty(),
                    dnsMode = dnsMode,
                    customDnsServers = config.dnsServers,
                    isRouteViaZeroTier = config.isRouteViaZeroTier,
                    isLoading = false,
                    isNotFound = false
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
     * 重新加载页面。
     */
    fun retry() {
        load()
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
     * 订阅服务生命周期状态。
     */
    private fun observeServiceState() {
        viewModelScope.launch {
            vpnEngine.serviceState.collect { serviceState ->
                _uiState.update { state ->
                    if (serviceState == ServiceState.STOPPED ||
                        serviceState == ServiceState.UNKNOWN
                    ) {
                        state.copy(
                            serviceState = serviceState,
                            virtualConfig = null,
                            ipAddresses = emptyList(),
                            dnsServers = emptyList(),
                            dnsDomain = null
                        )
                    } else {
                        state.copy(serviceState = serviceState)
                    }
                }
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
                ipAddresses = config.assignedAddresses.mapNotNull {
                    StringUtils.formatAddressWithPrefix(it, disableIpv6)
                },
                dnsServers = config.dns?.servers?.mapNotNull {
                    StringUtils.formatAddressWithPrefix(it, disableIpv6)
                } ?: emptyList(),
                dnsDomain = config.dns?.domain?.takeIf { it.isNotEmpty() }
            )
        }
    }

    /**
     * 解析持久化的 DNS 模式，非法值回退到无 DNS，避免旧数据导致崩溃。
     *
     * @param value 持久化值
     * @return 合法的 DNS 模式持久化值
     */
    private fun parseDnsMode(value: Int): Int = runCatching {
        DNSMode.fromInt(value).toInt()
    }.getOrDefault(DNSMode.NO_DNS.toInt())
}
