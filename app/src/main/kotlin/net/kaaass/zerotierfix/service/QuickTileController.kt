package net.kaaass.zerotierfix.service

import android.content.Context
import android.util.Log
import android.widget.Toast

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

import net.kaaass.zerotierfix.R
import net.kaaass.zerotierfix.data.repository.NetworkRepository
import net.kaaass.zerotierfix.data.repository.SettingsRepository
import net.kaaass.zerotierfix.di.IoDispatcher
import net.kaaass.zerotierfix.model.dto.NetworkSummary
import net.kaaass.zerotierfix.util.NetworkInfoUtils
import net.kaaass.zerotierfix.vpn.engine.QuickTileOperation
import net.kaaass.zerotierfix.vpn.engine.QuickTileState
import net.kaaass.zerotierfix.vpn.engine.QuickTileStatus
import net.kaaass.zerotierfix.vpn.engine.ServiceState
import net.kaaass.zerotierfix.vpn.engine.VpnEngine
import net.kaaass.zerotierfix.vpn.engine.mapQuickTileStatus
import net.kaaass.zerotierfix.vpn.engine.selectQuickTileTarget

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 快捷设置磁贴的连接控制器。
 *
 * 负责读取网络列表与上次连接记录、计算磁贴状态、串行执行连接 / 断开。
 * 使用应用级作用域，因此磁贴对应的 [android.service.quicksettings.TileService]
 * 被系统解绑后，已经发起的操作仍能完成。
 *
 * 状态以 [VpnEngine] / [net.kaaass.zerotierfix.vpn.engine.VpnStateStore] 为准，
 * 磁贴自身的“正在切换”状态只保存在本类中，不写入状态仓库。
 */
@Singleton
class QuickTileController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkRepository: NetworkRepository,
    private val settingsRepository: SettingsRepository,
    private val vpnEngine: VpnEngine,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val actionMutex = Mutex()

    private val networks = MutableStateFlow<List<NetworkSummary>>(emptyList())
    private val lastActivated = MutableStateFlow<Long?>(null)
    private val loaded = MutableStateFlow(false)
    private val operation = MutableStateFlow(QuickTileOperation.IDLE)

    private var refreshJob: Job? = null

    /**
     * 磁贴状态流，由网络列表、上次连接记录、加载状态、操作状态与服务状态合并而成。
     */
    val state: StateFlow<QuickTileState> = combine(
        networks,
        lastActivated,
        loaded,
        operation,
        vpnEngine.serviceState
    ) { networks, lastActivated, loaded, operation, serviceState ->
        QuickTileState(
            networksLoaded = loaded,
            networkCount = networks.size,
            targetNetworkId = selectQuickTileTarget(networks, lastActivated),
            status = mapQuickTileStatus(loaded, networks.size, serviceState, operation)
        )
    }.stateIn(scope, SharingStarted.Eagerly, QuickTileState())

    /** 网络列表是否已加载完成。 */
    val isLoaded: Boolean
        get() = loaded.value

    /** 是否有连接 / 断开操作正在进行。 */
    val isBusy: Boolean
        get() = operation.value != QuickTileOperation.IDLE

    /**
     * 立即计算当前状态，供磁贴点击时同步决策。
     */
    fun currentStatus(): QuickTileStatus = mapQuickTileStatus(
        networksLoaded = loaded.value,
        networkCount = networks.value.size,
        serviceState = vpnEngine.serviceState.value,
        operation = operation.value
    )

    /**
     * 立即计算点击磁贴时将连接的网络。
     *
     * @return 目标网络 ID；没有网络时返回 `null`
     */
    fun currentTargetNetworkId(): Long? =
        selectQuickTileTarget(networks.value, lastActivated.value)

    /**
     * 从服务与数据库刷新磁贴所需状态。
     *
     * 先在冷进程时探测服务是否在运行，再读取网络列表与上次连接记录。
     */
    fun refresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            try {
                vpnEngine.refreshState()
            } catch (e: Exception) {
                Log.w(TAG, "refreshState failed", e)
            }
            val snapshot = withContext(ioDispatcher) {
                networkRepository.getAllNetworks() to
                    networkRepository.getLastActivatedNetworkId()
            }
            networks.value = snapshot.first
            lastActivated.value = snapshot.second
            loaded.value = true
        }
    }

    /**
     * 连接指定网络。
     *
     * 若服务已在运行，先离开并停止当前网络；连接前写入上次连接记录，
     * 与 App 内网络列表的行为保持一致。
     *
     * 从磁贴点击时应使用 [connectAwait]：Android 12+ 禁止后台应用启动服务，
     * 需要由前台 Activity 等待连接完成后再关闭。
     *
     * @param networkId 目标网络 ID
     */
    fun connect(networkId: Long) {
        scope.launch {
            connectBlock(networkId)
        }
    }

    /**
     * 连接指定网络并在返回前完成服务启动请求。
     *
     * 调用方（透明授权页）应保持在前台直到本方法返回，否则服务启动可能被
     * 系统的后台限制拒绝。
     *
     * @param networkId 目标网络 ID
     */
    suspend fun connectAwait(networkId: Long) {
        connectBlock(networkId)
    }

    /**
     * 断开当前网络并停止服务。
     */
    fun disconnect() {
        scope.launch {
            disconnectBlock()
        }
    }

    private suspend fun connectBlock(networkId: Long) {
        if (!actionMutex.tryLock()) {
            return
        }
        operation.value = QuickTileOperation.CONNECTING
        try {
            val network = withContext(ioDispatcher) {
                networkRepository.getNetworkSummary(networkId)
            }
            if (network == null) {
                toast(R.string.quick_tile_toast_network_missing)
                return
            }
            if (!checkNetworkAvailable()) {
                return
            }

            stopCurrentIfNeeded()
            withContext(ioDispatcher) {
                networkRepository.setLastActivated(networkId)
            }
            lastActivated.value = networkId

            if (!vpnEngine.start(networkId)) {
                toast(R.string.toast_connect_failed)
            }
        } catch (e: Exception) {
            Log.e(TAG, "connect failed", e)
        } finally {
            operation.value = QuickTileOperation.IDLE
            actionMutex.unlock()
        }
    }

    private suspend fun disconnectBlock() {
        if (!actionMutex.tryLock()) {
            return
        }
        operation.value = QuickTileOperation.DISCONNECTING
        try {
            val serviceState = vpnEngine.serviceState.value
            if (serviceState != ServiceState.RUNNING && serviceState != ServiceState.STARTING) {
                return
            }
            val connectedId = vpnEngine.connectedNetworks.value
                ?.lastOrNull()
                ?.networkId
                ?: lastActivated.value
            vpnEngine.leaveAndStop(connectedId)
        } catch (e: Exception) {
            Log.e(TAG, "disconnect failed", e)
        } finally {
            operation.value = QuickTileOperation.IDLE
            actionMutex.unlock()
        }
    }

    /**
     * 服务已在运行时先离开当前网络并停止服务。
     */
    private suspend fun stopCurrentIfNeeded() {
        val serviceState = vpnEngine.serviceState.value
        if (serviceState == ServiceState.RUNNING || serviceState == ServiceState.STARTING) {
            vpnEngine.leaveAndStop(
                vpnEngine.connectedNetworks.value?.lastOrNull()?.networkId
            )
        }
    }

    /**
     * 检查当前网络环境是否允许建立 VPN。
     *
     * @return 允许时返回 `true`；否则提示并返回 `false`
     */
    private fun checkNetworkAvailable(): Boolean {
        val messageRes = when (NetworkInfoUtils.getNetworkInfoCurrentConnection(context)) {
            NetworkInfoUtils.CurrentConnection.CONNECTION_NONE ->
                R.string.toast_no_network

            NetworkInfoUtils.CurrentConnection.CONNECTION_MOBILE ->
                if (!settingsRepository.isCellularDataAllowed) {
                    R.string.toast_mobile_data
                } else {
                    null
                }

            NetworkInfoUtils.CurrentConnection.CONNECTION_OTHER -> null
        }
        if (messageRes != null) {
            toast(messageRes)
            return false
        }
        return true
    }

    private fun toast(messageRes: Int) {
        Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "QuickTileController"
    }
}
