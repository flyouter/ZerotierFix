package net.kaaass.zerotierfix.service

import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import android.service.quicksettings.TileService
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
import kotlinx.coroutines.flow.collect
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
    private var lastListeningRequest = 0L

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

    init {
        // 面板关闭期间服务状态变化时，请求 SystemUI 重新绑定磁贴，避免磁贴
        // 长期显示旧状态。requestListeningState 内部已做节流。
        scope.launch {
            vpnEngine.serviceState.collect {
                requestTileListeningState()
            }
        }
    }

    /** 网络列表是否已加载完成。 */
    val isLoaded: Boolean
        get() = loaded.value

    /**
     * 等待网络列表首次加载完成。
     *
     * 磁贴服务冷启动时 `onClick()` 往往早于 `refresh()` 完成；有界等待可以
     * 避免第一次点击被直接丢弃。超时后由调用方决定如何提示用户。
     *
     * @param timeoutMs 最长等待时间
     */
    suspend fun awaitLoaded(timeoutMs: Long = LOAD_TIMEOUT_MS) {
        if (loaded.value) return
        if (refreshJob?.isActive != true) {
            refresh()
        }
        withTimeoutOrNull(timeoutMs) { loaded.first { it } }
    }

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
     * 前台页面已接手连接意图：停止超时回滚。
     *
     * 系统 VPN 授权弹窗的停留时间不可控，[toggle] 的看门狗只用于“页面没有被
     * 拉起”的情况。页面出现后应交给它自己的收敛循环，否则用户阅读授权说明
     * 超过超时时间，意图会被清除，授权后也不会连接。
     */
    fun holdIntent() {
        intentWatchdog?.cancel()
        intentWatchdog = null
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
     * 从磁贴直接连接：不启动 Activity，服务启动后由应用级作用域继续收敛。
     *
     * 已授权时磁贴点击走这条路径，快捷设置面板保持展开；只有首次授权或
     * 系统拒绝后台启动服务时才回退到 Activity。
     *
     * @param networkId 目标网络 ID
     * @return `true` 表示已处理（连接已发起或已提示用户）；`false` 表示
     *         系统拒绝启动服务，调用方应回退到 Activity 路径
     */
    suspend fun connectFromTile(networkId: Long): Boolean {
        if (!prepareTarget(networkId)) {
            abandonIntent()
            return true
        }
        return if (vpnEngine.start(networkId)) {
            // `startService` 已同步写入 STARTING，settleAwait 不会重复启动。
            scope.launch {
                settleAwait(allowConnect = true)
            }
            true
        } else {
            false
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
     * 校验目标网络并记录为最近连接的网络。
     *
     * @return 可以继续启动服务时返回 `true`
     */
    private suspend fun prepareTarget(networkId: Long): Boolean {
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
        return true
    }

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
        if (!prepareTarget(networkId)) {
            return false
        }

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
     * 检查当前是否有可用网络。
     *
     * 蜂窝网络下是否允许访问 ZeroTier 内网由数据面策略处理，
     * 不影响隧道建立，这里只检查网络是否存在。
     *
     * @return 有网络时返回 `true`；否则提示并返回 `false`
     */
    private fun checkNetworkAvailable(): Boolean {
        if (NetworkInfoUtils.getNetworkInfoCurrentConnection(context) !=
            NetworkInfoUtils.CurrentConnection.CONNECTION_NONE
        ) {
            return true
        }
        toast(R.string.toast_no_network)
        return false
    }

    private fun toast(messageRes: Int) {
        Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()
    }

    /**
     * 请求 SystemUI 重新绑定磁贴以刷新状态。
     *
     * 面板打开时 SystemUI 本来就在监听；该方法用于面板关闭期间的状态变化。
     * 节流避免状态抖动时频繁触发绑定。
     */
    private fun requestTileListeningState() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastListeningRequest < LISTENING_REQUEST_INTERVAL_MS) {
            return
        }
        lastListeningRequest = now
        try {
            TileService.requestListeningState(
                context,
                ComponentName(context, ZeroTierTileService::class.java)
            )
        } catch (e: Exception) {
            Log.w(TAG, "requestListeningState failed", e)
        }
    }

    companion object {
        private const val TAG = "QuickTileController"

        /** 等待服务离开 STARTING 的最长时间。 */
        private const val STATE_TIMEOUT_MS = 8_000L

        /** 点击连接后等待前台页面开始收敛的最长时间。 */
        private const val CONNECT_INTENT_TIMEOUT_MS = 10_000L

        /** 磁贴首次点击等待网络列表加载的最长时间。 */
        private const val LOAD_TIMEOUT_MS = 1_500L

        /** requestListeningState 的最小调用间隔。 */
        private const val LISTENING_REQUEST_INTERVAL_MS = 500L
    }
}
