package net.kaaass.zerotierfix.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.SystemClock
import android.util.Log

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

import net.kaaass.zerotierfix.di.VpnCommandDispatcher
import net.kaaass.zerotierfix.model.dto.MoonOrbitDto
import net.kaaass.zerotierfix.model.dto.PeerInfoDto
import net.kaaass.zerotierfix.model.dto.VirtualNetworkConfigDto
import net.kaaass.zerotierfix.vpn.engine.BinderState
import net.kaaass.zerotierfix.vpn.engine.ServiceState
import net.kaaass.zerotierfix.vpn.engine.VpnCommandResult
import net.kaaass.zerotierfix.vpn.engine.VpnServiceCommands
import net.kaaass.zerotierfix.vpn.engine.VpnStateStore

import javax.inject.Inject
import javax.inject.Singleton

/**
 * [VpnServiceCommands] 的 Android 实现。
 *
 * 通过按需绑定 `ZeroTierOneService` 执行命令；所有命令在单并发调度器上串行，
 * 绑定计数归零立即解绑，避免 bound service 阻止 `stopSelf` 销毁。
 */
@Singleton
class AndroidVpnServiceCommands @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: VpnStateStore,
    @VpnCommandDispatcher private val dispatcher: CoroutineDispatcher
) : VpnServiceCommands {

    private val mutex = Mutex()

    @Volatile
    private var boundService: ZeroTierOneService? = null

    /** 最近一次发出启动请求的时间，用于给 `STARTING` 留出冷启动宽限期。 */
    @Volatile
    private var startRequestedAt = 0L

    private var connection: ServiceConnection? = null

    override suspend fun startService(networkId: Long): Boolean = withContext(dispatcher) {
        mutex.withLock {
            store.setServiceState(ServiceState.STARTING)
            startRequestedAt = SystemClock.elapsedRealtime()
            val intent = Intent(context, ZeroTierOneService::class.java)
                .putExtra(ZeroTierOneService.ZT1_NETWORK_ID, networkId)
                .putExtra(ZeroTierOneService.EXTRA_START_IN_FOREGROUND, true)
            val component = startServiceCompat(intent)
            if (component == null) {
                store.setServiceState(ServiceState.STOPPED)
                false
            } else {
                true
            }
        }
    }

    /**
     * 启动 VPN 服务：优先以 `startForegroundService()` 启动，失败时回退到普通
     * `startService()`。
     *
     * - 前台启动能让服务在连接窗口内立即进入前台，避免被系统回收；
     * - 部分 OEM/系统会拒绝从后台提升前台服务，此时 WireGuard 式的普通
     *   `startService()` 仍可能成功，作为第二道兜底。
     *
     * @return 系统接受的 ComponentName；两种方式都失败时返回 `null`
     */
    private fun startServiceCompat(intent: Intent): ComponentName? {
        return try {
            context.startForegroundService(intent)
        } catch (e: Exception) {
            Log.w(
                TAG,
                "startForegroundService failed (${e.javaClass.simpleName}), " +
                    "falling back to startService",
                e
            )
            startServiceFallback(intent)
        }
    }

    /**
     * 普通 `startService()` 兜底。
     */
    private fun startServiceFallback(intent: Intent): ComponentName? {
        return try {
            context.startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "startService also failed", e)
            null
        }
    }

    override suspend fun stop(): Unit = withContext(dispatcher) {
        mutex.withLock {
            val previous = store.serviceState.value
            if (previous == ServiceState.RUNNING || previous == ServiceState.STARTING) {
                val service = acquire(STOP_CONNECT_TIMEOUT_MS)
                if (service != null) {
                    try {
                        service.stopZeroTier()
                    } catch (e: Exception) {
                        Log.e(TAG, "stopZeroTier failed", e)
                    }
                    release()
                }
            }
            try {
                context.stopService(Intent(context, ZeroTierOneService::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "stopService failed", e)
            }

            if (awaitServiceStopped()) {
                store.setServiceState(ServiceState.STOPPED)
            } else {
                // 服务确实还在运行：保持真实状态，避免磁贴/UI 伪报断开。
                val actual = if (previous == ServiceState.STARTING) {
                    ServiceState.STARTING
                } else {
                    ServiceState.RUNNING
                }
                store.setServiceState(actual)
                Log.w(TAG, "Service still running after stop timeout")
            }
        }
    }

    /**
     * 等待服务真正从系统中消失。
     *
     * `stopZeroTier()` 与 `stopService()` 都是异步生效，必须确认后再写
     * [ServiceState.STOPPED]，否则停止失败会被状态掩盖。
     */
    private suspend fun awaitServiceStopped(): Boolean {
        val deadline = SystemClock.elapsedRealtime() + STOP_VERIFY_TIMEOUT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            if (!ServiceLiveness.isRunning(context, ZeroTierOneService::class.java)) {
                return true
            }
            delay(STOP_VERIFY_POLL_MS)
        }
        return !ServiceLiveness.isRunning(context, ZeroTierOneService::class.java)
    }

    override suspend fun join(networkId: Long): VpnCommandResult<Unit> =
        withContext(dispatcher) {
            mutex.withLock {
                try {
                    val result = useService { it.joinNetwork(networkId) }
                    if (result == null) {
                        VpnCommandResult.ServiceNotRunning
                    } else {
                        VpnCommandResult.Success(Unit)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "join failed", e)
                    VpnCommandResult.Failure(e)
                }
            }
        }

    override suspend fun leave(networkId: Long): VpnCommandResult<Unit> =
        withContext(dispatcher) {
            mutex.withLock {
                try {
                    val result = useService { it.leaveNetwork(networkId) }
                    if (result == null) {
                        VpnCommandResult.ServiceNotRunning
                    } else {
                        VpnCommandResult.Success(Unit)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "leave failed", e)
                    VpnCommandResult.Failure(e)
                }
            }
        }

    override suspend fun orbit(moons: List<MoonOrbitDto>): Unit = withContext(dispatcher) {
        mutex.withLock {
            try {
                useService { it.orbitMoons(moons) }
            } catch (e: Exception) {
                Log.e(TAG, "orbit failed", e)
            }
        }
    }

    override suspend fun loadPeers(): VpnCommandResult<List<PeerInfoDto>> =
        withContext(dispatcher) {
            mutex.withLock {
                val service = acquire(CONNECT_TIMEOUT_MS)
                    ?: return@withLock VpnCommandResult.ServiceNotRunning
                try {
                    val peers = service.getPeers()
                    if (peers == null) {
                        VpnCommandResult.NodeNotReady
                    } else {
                        VpnCommandResult.Success(peers)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "loadPeers failed", e)
                    VpnCommandResult.Failure(e)
                } finally {
                    release()
                }
            }
        }

    override suspend fun reconfigure(networkId: Long): Unit = withContext(dispatcher) {
        mutex.withLock {
            try {
                useService { it.reconfigureNetwork(networkId) }
            } catch (e: Exception) {
                Log.e(TAG, "reconfigure failed", e)
            }
        }
    }

    override suspend fun virtualNetworkConfig(
        networkId: Long
    ): VpnCommandResult<VirtualNetworkConfigDto> = withContext(dispatcher) {
        mutex.withLock {
            val service = acquire(CONNECT_TIMEOUT_MS)
                ?: return@withLock VpnCommandResult.ServiceNotRunning
            try {
                val config = service.getVirtualNetworkConfigDto(networkId)
                if (config == null) {
                    VpnCommandResult.NodeNotReady
                } else {
                    VpnCommandResult.Success(config)
                }
            } catch (e: Exception) {
                Log.e(TAG, "virtualNetworkConfig failed", e)
                VpnCommandResult.Failure(e)
            } finally {
                release()
            }
        }
    }

    override suspend fun refreshState(): Unit = withContext(dispatcher) {
        // 先确认服务真的存在再决定是否绑定：服务已死时 `bindService` 会重新
        // 创建服务实例，把真实状态掩盖成「仍在运行」，磁贴也就一直显示已连接。
        if (!ServiceLiveness.isRunning(context, ZeroTierOneService::class.java)) {
            reconcileDeadService()
            return@withContext
        }
        val state = store.serviceState.value
        if (state != ServiceState.RUNNING && state != ServiceState.STARTING) {
            if (state == ServiceState.UNKNOWN) {
                // 原子归一化：服务可能同时在主线程写入 STARTING/RUNNING。
                store.compareAndSetServiceState(ServiceState.UNKNOWN, ServiceState.STOPPED)
            }
            return@withContext
        }
        mutex.withLock {
            try {
                useService { service ->
                    service.refreshNodeStatus()
                    service.refreshNetworkListState()
                }
            } catch (e: Exception) {
                Log.e(TAG, "refreshState failed", e)
            }
        }
    }

    /**
     * 服务实际不存在时纠正进程内的残留状态。
     *
     * 进程被系统杀死后 [VpnStateStore] 里的 `RUNNING` / `STARTING` 可能残留；
     * 不纠正会让磁贴与页面一直显示「已连接」。`STARTING` 保留宽限期，避免把
     * 刚发出启动请求、服务尚未创建的冷启动误判为停止。
     */
    private fun reconcileDeadService() {
        when (store.serviceState.value) {
            ServiceState.RUNNING -> {
                if (store.compareAndSetServiceState(ServiceState.RUNNING, ServiceState.STOPPED)) {
                    clearStaleState("RUNNING")
                }
            }
            ServiceState.STARTING -> {
                val elapsed = SystemClock.elapsedRealtime() - startRequestedAt
                if (startRequestedAt > 0L && elapsed >= START_GRACE_MS &&
                    store.compareAndSetServiceState(ServiceState.STARTING, ServiceState.STOPPED)
                ) {
                    clearStaleState("STARTING")
                }
            }
            ServiceState.UNKNOWN -> {
                store.compareAndSetServiceState(ServiceState.UNKNOWN, ServiceState.STOPPED)
            }
            ServiceState.STOPPED -> Unit
        }
    }

    /**
     * 清空服务存活期间才会有意义的数据，避免页面展示过期状态。
     */
    private fun clearStaleState(from: String) {
        Log.i(TAG, "Service not running; reconciled $from -> STOPPED")
        store.setNodeStatus(null)
        store.setConnectedNetworks(null)
        store.clearVirtualNetworkConfigs()
        store.setBinderState(BinderState.DISCONNECTED)
    }

    /**
     * 获取服务 binder；未运行时返回 `null`。
     *
     * 必须在 [mutex] 内调用。
     *
     * @param timeoutMs 等待连接的超时时间
     * @return 服务实例或 `null`
     */
    private suspend fun acquire(timeoutMs: Long): ZeroTierOneService? {
        boundService?.let { return it }
        // 服务被系统杀死时连接可能已失效但还没有释放；先解绑，避免这条绑定
        // 一直挂着让系统重新拉起服务。
        release()
        val deferred = CompletableDeferred<ZeroTierOneService?>()
        val conn = BinderConnection(deferred)
        connection = conn
        store.setBinderState(BinderState.CONNECTING)
        val requested = try {
            context.bindService(
                Intent(context, ZeroTierOneService::class.java),
                conn,
                Context.BIND_NOT_FOREGROUND
            )
        } catch (e: Exception) {
            Log.e(TAG, "bindService failed", e)
            false
        }
        if (!requested) {
            connection = null
            store.setBinderState(BinderState.DISCONNECTED)
            return null
        }
        val service = withTimeoutOrNull(timeoutMs) { deferred.await() }
        if (service == null) {
            release()
            return null
        }
        return service
    }

    /**
     * 解除当前绑定。
     *
     * 必须在 [mutex] 内调用。
     */
    private fun release() {
        detachConnection(connection)
    }

    /**
     * 解绑指定连接。
     *
     * 同时可能被命令线程与主线程的 [ServiceConnection] 回调调用，因此用
     * `@Synchronized` 保护，并用调用方身份过滤已经失效的旧连接。
     *
     * @param conn 待解绑连接；为 `null` 或已不是当前连接时忽略
     */
    @Synchronized
    private fun detachConnection(conn: ServiceConnection?) {
        if (conn == null || connection !== conn) {
            return
        }
        connection = null
        boundService = null
        try {
            context.unbindService(conn)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "unbindService ignored", e)
        }
        store.setBinderState(BinderState.DISCONNECTED)
    }

    /**
     * 绑定服务并执行命令，结束后立即解绑。
     *
     * 必须在 [mutex] 内调用。
     *
     * @param timeoutMs 绑定超时时间
     * @param block 命令
     * @return 命令结果；服务未运行时返回 `null`
     */
    private suspend fun <T> useService(
        timeoutMs: Long = CONNECT_TIMEOUT_MS,
        block: (ZeroTierOneService) -> T
    ): T? {
        val service = acquire(timeoutMs) ?: return null
        return try {
            block(service)
        } finally {
            release()
        }
    }

    /**
     * Binder 连接回调。
     *
     * 通过 `connection === this` 过滤已经失效的旧连接，避免超时解绑后的
     * 迟到回调重新写入 [boundService]。
     */
    private inner class BinderConnection(
        private val deferred: CompletableDeferred<ZeroTierOneService?>
    ) : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (connection !== this) return
            val service = (binder as? ZeroTierOneService.ZeroTierBinder)?.getService()
            boundService = service
            store.setBinderState(
                if (service != null) BinderState.CONNECTED else BinderState.DISCONNECTED
            )
            if (!deferred.isCompleted) {
                deferred.complete(service)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            if (connection !== this) return
            boundService = null
            // 服务被杀死后绑定不会自动解除，必须显式释放；否则系统会保持
            // 绑定并可能重新创建服务，磁贴也就无法确认服务已经停止。
            detachConnection(this)
            if (!deferred.isCompleted) {
                deferred.complete(null)
            }
        }

        override fun onBindingDied(name: ComponentName?) {
            if (connection !== this) return
            boundService = null
            detachConnection(this)
            store.setServiceState(ServiceState.STOPPED)
            if (!deferred.isCompleted) {
                deferred.complete(null)
            }
        }

        override fun onNullBinding(name: ComponentName?) {
            if (connection !== this) return
            boundService = null
            detachConnection(this)
            if (!deferred.isCompleted) {
                deferred.complete(null)
            }
        }
    }

    companion object {
        private const val TAG = "AndroidVpnCommands"
        private const val CONNECT_TIMEOUT_MS = 500L
        private const val STOP_CONNECT_TIMEOUT_MS = 500L

        /** 发出启动请求后，允许服务尚未出现在系统服务列表中的宽限期。 */
        private const val START_GRACE_MS = 8_000L

        /** 停止后确认服务消失的最长等待时间。 */
        private const val STOP_VERIFY_TIMEOUT_MS = 2_000L

        /** 停止确认的轮询间隔。 */
        private const val STOP_VERIFY_POLL_MS = 50L
    }
}
