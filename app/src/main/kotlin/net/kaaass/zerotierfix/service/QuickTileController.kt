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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

import net.kaaass.zerotierfix.R
import net.kaaass.zerotierfix.data.repository.NetworkRepository
import net.kaaass.zerotierfix.data.repository.SettingsRepository
import net.kaaass.zerotierfix.di.IoDispatcher
import net.kaaass.zerotierfix.model.dto.NetworkSummary
import net.kaaass.zerotierfix.util.NetworkInfoUtils
import net.kaaass.zerotierfix.vpn.engine.QuickTileState
import net.kaaass.zerotierfix.vpn.engine.QuickTileStatus
import net.kaaass.zerotierfix.vpn.engine.ServiceState
import net.kaaass.zerotierfix.vpn.engine.VpnEngine
import net.kaaass.zerotierfix.vpn.engine.awaitServiceState
import net.kaaass.zerotierfix.vpn.engine.mapQuickTileStatus
import net.kaaass.zerotierfix.vpn.engine.selectQuickTileTarget

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 快捷设置磁贴的连接控制器。
 *
 * 点击采用“乐观状态 + 意图合并 + 后台收敛”：
 * - [toggle] 立即翻转用户期望状态，磁贴无需等待服务启停即可反馈；
 * - [settleAwait] 在后台串行执行必要的连接/断开，直到实际状态与期望一致；
 * - 操作期间的新点击只更新期望状态，不会排队执行多次互相抵消的启停；
 * - 收敛失败或超时时放弃意图，磁贴回到实际状态，保证最终一致。
 *
 * 连接需要前台 Activity（Android 12+ 禁止后台启动服务），由
 * [VpnConsentActivity] 调用 [settleAwait]；断开可以在后台完成。
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
    private val settleMutex = Mutex()

    private val networks = MutableStateFlow<List<NetworkSummary>>(emptyList())
    private val lastActivated = MutableStateFlow<Long?>(null)
    private val loaded = MutableStateFlow(false)

    /** 用户最近一次表达的期望状态；`null` 表示跟随实际服务状态。 */
    private val desiredConnected = MutableStateFlow<Boolean?>(null)

    private var refreshJob: Job? = null
    private var intentWatchdog: Job? = null

    /**
     * 磁贴状态流，由网络列表、上次连接记录、加载状态、用户期望与服务状态合并而成。
     */
    val state: StateFlow<QuickTileState> = combine(
        networks,
        lastActivated,
        loaded,
        desiredConnected,
        vpnEngine.serviceState
    ) { networks, lastActivated, loaded, desired, serviceState ->
        QuickTileState(
            networksLoaded = loaded,
            networkCount = networks.size,
            targetNetworkId = selectQuickTileTarget(networks, lastActivated),
            desiredConnected = desired,
            status = mapQuickTileStatus(loaded, networks.size, serviceState, desired)
        )
    }.stateIn(scope, SharingStarted.Eagerly, QuickTileState())

    /** 网络列表是否已加载完成。 */
    val isLoaded: Boolean
        get() = loaded.value

    /**
     * 立即计算当前状态，供磁贴点击时同步决策。
     */
    fun currentStatus(): QuickTileStatus = mapQuickTileStatus(
        networksLoaded = loaded.value,
        networkCount = networks.value.size,
        serviceState = vpnEngine.serviceState.value,
        desiredConnected = desiredConnected.value
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
     * 翻转用户期望状态。
     *
     * 只更新期望值，不在此处执行连接：连接需要由前台 Activity 驱动，
     * 调用方根据返回值决定是否启动 [VpnConsentActivity]。
     *
     * @return 翻转后的期望状态；`true` 表示期望连接
     */
    fun toggle(): Boolean {
        val current = desiredConnected.value ?: (actualState() != ActualState.DISCONNECTED)
        val next = !current
        desiredConnected.value = next

        intentWatchdog?.cancel()
        intentWatchdog = null
        if (next) {
            // 兜底：Activity 启动被拒或解锁被取消时，避免磁贴永久停留在“正在连接”。
            intentWatchdog = scope.launch {
                delay(CONNECT_INTENT_TIMEOUT_MS)
                if (desiredConnected.value == true &&
                    actualState() == ActualState.DISCONNECTED
                ) {
                    Log.w(TAG, "Connect intent timed out before settling")
                    desiredConnected.value = null
                }
            }
        }
        return next
    }

    /**
     * 放弃尚未完成的用户意图，磁贴回到实际状态。
     */
    fun abandonIntent() {
        intentWatchdog?.cancel()
        intentWatchdog = null
        desiredConnected.value = null
    }

    /**
     * 在后台推进到期望状态。
     *
     * 只适用于断开（`startService` 才有后台限制）。若循环中发现用户随后
     * 又想要连接，会保留意图并退出，交给前台 Activity 接手。
     */
    fun startSettle() {
        scope.launch {
            settleAwait(allowConnect = false)
        }
    }

    /**
     * 持续执行连接/断开，直到实际状态与用户期望一致。
     *
     * @param allowConnect 是否允许执行连接。连接必须由前台 Activity 调用
     * （Android 12+ 禁止后台启动服务）；后台调用遇到连接意图时会保留意图，
     * 等待前台接手。
     *
     * 每次循环都会重新读取用户期望，因此操作期间的新点击会被自然合并。
     * 前台收敛结束或失败后清除意图，磁贴跟随真实状态；后台遇到连接意图时
     * 保留意图，避免吞掉用户的最后一次点击。
     */
    suspend fun settleAwait(allowConnect: Boolean) {
        settleMutex.withLock {
            var keepIntent = false
            try {
                if (vpnEngine.serviceState.value == ServiceState.UNKNOWN) {
                    vpnEngine.refreshState()
                }
                while (true) {
                    val wantConnected = desiredConnected.value ?: return
                    when (actualState()) {
                        ActualState.TRANSITIONING -> {
                            if (!awaitStableState()) {
                                Log.w(TAG, "Service state did not settle in time")
                                return
                            }
                        }
                        ActualState.CONNECTED -> {
                            if (wantConnected) {
                                return
                            }
                            if (!disconnectOnce()) {
                                return
                            }
                        }
                        ActualState.DISCONNECTED -> {
                            if (!wantConnected) {
                                return
                            }
                            if (!allowConnect) {
                                // 后台不能启动服务：保留意图，等前台 Activity 接手。
                                keepIntent = true
                                return
                            }
                            if (!connectOnce()) {
                                return
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "settle failed", e)
            } finally {
                if (!keepIntent) {
                    // 收敛完成或失败后回到“跟随实际状态”，保证磁贴与真实状态一致。
                    intentWatchdog?.cancel()
                    intentWatchdog = null
                    desiredConnected.value = null
                }
            }
        }
    }

    private enum class ActualState {
        CONNECTED,
        DISCONNECTED,
        TRANSITIONING
    }

    private fun actualState(): ActualState = when (vpnEngine.serviceState.value) {
        ServiceState.RUNNING -> ActualState.CONNECTED
        ServiceState.STARTING -> ActualState.TRANSITIONING
        ServiceState.STOPPED, ServiceState.UNKNOWN -> ActualState.DISCONNECTED
    }

    /**
     * 等待服务离开 `STARTING`，返回是否在超时前稳定下来。
     */
    private suspend fun awaitStableState(): Boolean =
        withTimeoutOrNull(STATE_TIMEOUT_MS) {
            vpnEngine.serviceState.first {
                it == ServiceState.RUNNING || it == ServiceState.STOPPED
            }
        } != null

    /**
     * 连接目标网络并等待服务稳定。
     *
     * @return 服务最终是否处于运行状态
     */
    private suspend fun connectOnce(): Boolean {
        val networkId = currentTargetNetworkId()
        if (networkId == null) {
            toast(R.string.quick_tile_toast_no_network)
            return false
        }
        val network = withContext(ioDispatcher) {
            networkRepository.getNetworkSummary(networkId)
        }
        if (network == null) {
            toast(R.string.quick_tile_toast_network_missing)
            return false
        }
        if (!checkNetworkAvailable()) {
            return false
        }

        withContext(ioDispatcher) {
            networkRepository.setLastActivated(networkId)
        }
        lastActivated.value = networkId

        if (!vpnEngine.start(networkId)) {
            toast(R.string.toast_connect_failed)
            return false
        }
        if (!awaitStableState()) {
            Log.w(TAG, "Connect timed out")
            return false
        }
        return vpnEngine.serviceState.value == ServiceState.RUNNING
    }

    /**
     * 断开当前网络并等待服务停止。
     *
     * @return 服务最终是否处于停止状态
     */
    private suspend fun disconnectOnce(): Boolean {
        val connectedId = vpnEngine.connectedNetworks.value
            ?.lastOrNull()
            ?.networkId
            ?: lastActivated.value
        vpnEngine.leaveAndStop(connectedId)
        // 只认 STOPPED：停止失败时必须回到真实状态，不能假装已断开。
        if (!awaitServiceState(vpnEngine.serviceState, ServiceState.STOPPED, STATE_TIMEOUT_MS)) {
            Log.w(TAG, "Service did not stop in time")
            return false
        }
        return true
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

        /** 等待服务离开 STARTING 的最长时间。 */
        private const val STATE_TIMEOUT_MS = 8_000L

        /** 点击连接后等待前台页面开始收敛的最长时间。 */
        private const val CONNECT_INTENT_TIMEOUT_MS = 10_000L
    }
}
