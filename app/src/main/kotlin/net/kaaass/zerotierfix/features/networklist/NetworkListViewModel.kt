package net.kaaass.zerotierfix.features.networklist

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

import net.kaaass.zerotierfix.BuildConfig
import net.kaaass.zerotierfix.R
import net.kaaass.zerotierfix.data.repository.AppNodeRepository
import net.kaaass.zerotierfix.data.repository.NetworkRepository
import net.kaaass.zerotierfix.data.repository.SettingsRepository
import net.kaaass.zerotierfix.di.IoDispatcher
import net.kaaass.zerotierfix.model.dto.NetworkSummary
import net.kaaass.zerotierfix.model.dto.NodeStatusDto
import net.kaaass.zerotierfix.model.dto.VirtualNetworkConfigDto
import net.kaaass.zerotierfix.model.enums.NetworkStatus
import net.kaaass.zerotierfix.ui.common.UiText
import net.kaaass.zerotierfix.util.StringUtils
import net.kaaass.zerotierfix.vpn.engine.ServiceState
import net.kaaass.zerotierfix.vpn.engine.VpnEngine

import javax.inject.Inject

/**
 * 网络列表页面的 ViewModel。
 *
 * 负责把服务状态 Flow 映射为页面状态，并串行执行连接 / 断开 / 删除等操作，
 * 复刻原 EventBus `BACKGROUND` 单线程队列的顺序语义。
 */
@HiltViewModel
class NetworkListViewModel @Inject constructor(
    private val networkRepository: NetworkRepository,
    private val appNodeRepository: AppNodeRepository,
    private val settingsRepository: SettingsRepository,
    private val vpnEngine: VpnEngine,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        NetworkListUiState(appVersion = BuildConfig.VERSION_NAME)
    )

    /** 页面状态。 */
    val uiState: StateFlow<NetworkListUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<NetworkListEffect>(extraBufferCapacity = 16)

    /** 一次性事件。 */
    val effects: SharedFlow<NetworkListEffect> = _effects.asSharedFlow()

    private val actionMutex = Mutex()

    /** 数据库中的网络列表，用于引擎状态变化时重建条目。 */
    private var summaries: List<NetworkSummary> = emptyList()

    /** 引擎实时配置缓存。 */
    private var engineConfigs: Map<Long, VirtualNetworkConfigDto>? = null

    init {
        observeVpnState()
    }

    /**
     * 刷新网络列表与节点状态。
     *
     * 线程：主线程调用，数据库读取切换到 IO 线程。
     */
    fun refresh() {
        viewModelScope.launch {
            refreshNetworks()
            val nodeId = withContext(ioDispatcher) { appNodeRepository.getNodeIdStr() }
            if (nodeId != null) {
                _uiState.update { it.copy(nodeId = nodeId) }
            }
            vpnEngine.refreshState()
            checkNotificationPermission()
        }
    }

    /**
     * 切换网络开关。
     *
     * 打开时先离开并停止当前连接，再检查网络环境后启动目标网络；
     * 关闭时离开网络并停止服务。操作进行中忽略新的开关请求。
     *
     * @param network 目标网络
     * @param enabled 是否打开
     */
    fun setNetworkEnabled(network: NetworkSummary, enabled: Boolean) {
        if (_uiState.value.operatingNetworkId != null) {
            return
        }
        viewModelScope.launch {
            actionMutex.withLock {
                _uiState.update { it.copy(operatingNetworkId = network.networkId) }
                try {
                    if (enabled) {
                        connect(network)
                    } else {
                        disconnect(network)
                    }
                } finally {
                    _uiState.update { it.copy(operatingNetworkId = null) }
                }
            }
        }
    }

    /**
     * 处理 VPN 授权结果。
     *
     * @param networkId 待连接的网络 ID
     * @param granted 是否授权
     */
    fun onVpnConsentResult(networkId: Long, granted: Boolean) {
        if (!granted) {
            _effects.tryEmit(
                NetworkListEffect.ShowToast(UiText.Resource(R.string.toast_vpn_permission_denied))
            )
            return
        }
        viewModelScope.launch {
            actionMutex.withLock {
                _uiState.update { it.copy(operatingNetworkId = networkId) }
                try {
                    startNetwork(networkId)
                } finally {
                    _uiState.update { it.copy(operatingNetworkId = null) }
                }
            }
        }
    }

    /**
     * 删除网络。
     *
     * 若删除的是当前连接的网络，先停止服务，再从数据库删除。
     *
     * @param network 目标网络
     */
    fun deleteNetwork(network: NetworkSummary) {
        if (_uiState.value.operatingNetworkId != null) {
            return
        }
        viewModelScope.launch {
            actionMutex.withLock {
                _uiState.update { it.copy(operatingNetworkId = network.networkId) }
                try {
                    if (_uiState.value.connectedNetworkId == network.networkId) {
                        vpnEngine.stop()
                        _uiState.update { it.copy(connectedNetworkId = null) }
                    }
                    withContext(ioDispatcher) {
                        networkRepository.deleteNetwork(network.networkId)
                    }
                    refreshNetworks()
                    _effects.emit(
                        NetworkListEffect.ShowToast(
                            UiText.Resource(R.string.toast_network_deleted)
                        )
                    )
                } finally {
                    _uiState.update { it.copy(operatingNetworkId = null) }
                }
            }
        }
    }

    /**
     * 打开通知设置，并关闭提示框。
     */
    fun openNotificationSettings() {
        _uiState.update { it.copy(isNotificationAlertVisible = false) }
        _effects.tryEmit(NetworkListEffect.OpenNotificationSettings)
    }

    /**
     * 不再提示通知权限，并关闭提示框。
     */
    fun disableNotificationAlert() {
        settingsRepository.setNoNotificationAlertDisabled(true)
        _uiState.update { it.copy(isNotificationAlertVisible = false) }
    }

    /**
     * 关闭通知权限提示框。
     */
    fun dismissNotificationAlert() {
        _uiState.update { it.copy(isNotificationAlertVisible = false) }
    }

    /**
     * 订阅引擎状态。
     */
    private fun observeVpnState() {
        viewModelScope.launch {
            combine(
                vpnEngine.serviceState,
                vpnEngine.connectedNetworks,
                vpnEngine.virtualNetworkConfigs,
                vpnEngine.nodeStatus,
                vpnEngine.nodeId
            ) { serviceState, connectedNetworks, configs, nodeStatus, nodeId ->
                EngineSnapshot(serviceState, connectedNetworks, configs, nodeStatus, nodeId)
            }.collect { snapshot ->
                engineConfigs = snapshot.configs
                val currentConnectedId = _uiState.value.connectedNetworkId
                _uiState.update { state ->
                    state.copy(
                        serviceState = snapshot.serviceState,
                        connectedNetworkId = resolveConnectedId(
                            snapshot.serviceState,
                            snapshot.connectedNetworks,
                            currentConnectedId
                        ),
                        nodeStatus = snapshot.nodeStatus,
                        clientVersion = snapshot.nodeStatus?.clientVersion ?: state.clientVersion,
                        nodeId = snapshot.nodeId
                            ?: snapshot.nodeStatus
                                ?.takeIf { it.isOnline }
                                ?.let { java.lang.Long.toHexString(it.address) }
                            ?: state.nodeId
                    )
                }
                rebuildItems()
            }
        }
        viewModelScope.launch {
            vpnEngine.errors.collect { message ->
                _effects.emit(
                    NetworkListEffect.ShowToast(
                        UiText.Resource(R.string.toast_vpn_error, listOf(message))
                    )
                )
                refreshNetworks()
            }
        }
        viewModelScope.launch {
            vpnEngine.statusNotices.collect { config ->
                emitStatusNotice(config)
                refreshNetworks()
            }
        }
    }

    /**
     * 连接指定网络。
     *
     * @param network 目标网络
     */
    @Suppress("DEPRECATION")
    private suspend fun connect(network: NetworkSummary) {
        vpnEngine.leaveAndStop(_uiState.value.connectedNetworkId)
        _uiState.update { it.copy(connectedNetworkId = null) }

        val activeNetwork = activeNetworkInfo()
        if (activeNetwork == null || !activeNetwork.isConnectedOrConnecting) {
            _effects.emit(
                NetworkListEffect.ShowToast(UiText.Resource(R.string.toast_no_network))
            )
            refreshNetworks()
            return
        }
        if (!settingsRepository.isCellularDataAllowed &&
            activeNetwork.type == ConnectivityManager.TYPE_MOBILE
        ) {
            _effects.emit(
                NetworkListEffect.ShowToast(UiText.Resource(R.string.toast_mobile_data))
            )
            refreshNetworks()
            return
        }

        withContext(ioDispatcher) {
            networkRepository.setLastActivated(network.networkId)
        }
        _effects.emit(NetworkListEffect.RequestVpnConsent(network.networkId))
    }

    /**
     * 断开指定网络。
     *
     * @param network 目标网络
     */
    private suspend fun disconnect(network: NetworkSummary) {
        vpnEngine.leaveAndStop(network.networkId)
        _uiState.update { it.copy(connectedNetworkId = null) }
        refreshNetworks()
    }

    /**
     * 启动服务并标记已连接网络。
     *
     * @param networkId 网络 ID
     */
    private suspend fun startNetwork(networkId: Long) {
        if (vpnEngine.start(networkId)) {
            _uiState.update { it.copy(connectedNetworkId = networkId) }
        } else {
            _effects.emit(
                NetworkListEffect.ShowToast(UiText.Resource(R.string.toast_connect_failed))
            )
            refreshNetworks()
        }
    }

    /**
     * 从数据库重新读取网络列表并重建 UI 条目。
     */
    private suspend fun refreshNetworks() {
        val networks = withContext(ioDispatcher) { networkRepository.getAllNetworks() }
        summaries = networks
        rebuildItems()
        _uiState.update { it.copy(isLoading = false) }
    }

    /**
     * 按当前引擎状态重建列表条目。
     *
     * 服务停止时忽略缓存的实时配置，避免展示过期状态。
     */
    private fun rebuildItems() {
        val state = _uiState.value
        val configs = when (state.serviceState) {
            ServiceState.RUNNING, ServiceState.STARTING -> engineConfigs
            ServiceState.UNKNOWN, ServiceState.STOPPED -> null
        }
        val items = buildNetworkItems(
            summaries = summaries,
            configs = configs,
            disableIpv6 = settingsRepository.isIpv6Disabled
        )
        _uiState.update { it.copy(networks = items) }
    }

    /**
     * 计算当前连接的网络 ID。
     *
     * `STARTING` 与服务刚启动但尚未上报网络时保留调用方设置的乐观值；
     * 服务停止或明确上报空列表时清空。
     */
    private fun resolveConnectedId(
        serviceState: ServiceState,
        connectedNetworks: List<VirtualNetworkConfigDto>?,
        current: Long?
    ): Long? = when (serviceState) {
        ServiceState.UNKNOWN, ServiceState.STOPPED -> null
        ServiceState.STARTING -> connectedNetworks?.lastOrNull()?.networkId ?: current
        ServiceState.RUNNING -> connectedNetworks?.let { it.lastOrNull()?.networkId } ?: current
    }

    /**
     * 发出网络状态提示，保持与原事件相同的 Toast 范围。
     *
     * @param config 发生变化的网络配置
     */
    private suspend fun emitStatusNotice(config: VirtualNetworkConfigDto) {
        val messageRes = when (config.status) {
            NetworkStatus.OK -> R.string.toast_network_status_ok
            NetworkStatus.ACCESS_DENIED -> R.string.toast_network_status_access_denied
            NetworkStatus.NOT_FOUND -> R.string.toast_network_status_not_found
            NetworkStatus.PORT_ERROR -> R.string.toast_network_status_port_error
            NetworkStatus.CLIENT_TOO_OLD -> R.string.toast_network_status_client_too_old
            NetworkStatus.AUTHENTICATION_REQUIRED ->
                R.string.toast_network_status_authentication_required
            else -> null
        } ?: return
        _effects.emit(
            NetworkListEffect.ShowToast(
                UiText.Resource(
                    messageRes,
                    listOf(StringUtils.networkIdToString(config.networkId))
                )
            )
        )
    }

    /**
     * 检查通知权限，必要时显示提示框。
     */
    private fun checkNotificationPermission() {
        val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!enabled && !settingsRepository.isNoNotificationAlertDisabled) {
            _uiState.update { it.copy(isNotificationAlertVisible = true) }
        }
    }

    /**
     * 获取当前活动网络。
     *
     * @return 当前网络；无网络时返回 `null`
     */
    @Suppress("DEPRECATION")
    private fun activeNetworkInfo(): NetworkInfo? {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return manager.activeNetworkInfo
    }

    /**
     * 引擎状态快照，用于单次 combine 更新。
     */
    private data class EngineSnapshot(
        val serviceState: ServiceState,
        val connectedNetworks: List<VirtualNetworkConfigDto>?,
        val configs: Map<Long, VirtualNetworkConfigDto>?,
        val nodeStatus: NodeStatusDto?,
        val nodeId: String?
    )
}
