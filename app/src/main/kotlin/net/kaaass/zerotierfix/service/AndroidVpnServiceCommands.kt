package net.kaaass.zerotierfix.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    @Volatile
    private var boundService: ZeroTierOneService? = null

    private var connection: ServiceConnection? = null

    init {
        scope.launch { probe() }
    }

    override suspend fun probe(): Unit = withContext(dispatcher) {
        mutex.withLock {
            val state = store.serviceState.value
            if (state == ServiceState.RUNNING || state == ServiceState.STARTING) {
                return@withLock
            }
            val service = acquire(PROBE_TIMEOUT_MS)
            store.setServiceState(
                if (service != null) ServiceState.RUNNING else ServiceState.STOPPED
            )
            release()
        }
    }

    override suspend fun startService(networkId: Long): Boolean = withContext(dispatcher) {
        mutex.withLock {
            store.setServiceState(ServiceState.STARTING)
            val intent = Intent(context, ZeroTierOneService::class.java)
                .putExtra(ZeroTierOneService.ZT1_NETWORK_ID, networkId)
            val component = try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "startService failed", e)
                null
            }
            if (component == null) {
                store.setServiceState(ServiceState.STOPPED)
                false
            } else {
                true
            }
        }
    }

    override suspend fun stop(): Unit = withContext(dispatcher) {
        mutex.withLock {
            val service = acquire(STOP_CONNECT_TIMEOUT_MS)
            if (service != null) {
                try {
                    service.stopZeroTier()
                } catch (e: Exception) {
                    Log.e(TAG, "stopZeroTier failed", e)
                }
                release()
            }
            try {
                context.stopService(Intent(context, ZeroTierOneService::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "stopService failed", e)
            }
            store.setServiceState(ServiceState.STOPPED)
        }
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
        if (store.serviceState.value == ServiceState.UNKNOWN) {
            probe()
        }
        val state = store.serviceState.value
        if (state != ServiceState.RUNNING && state != ServiceState.STARTING) {
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
     * 获取服务 binder；未运行时返回 `null`。
     *
     * 必须在 [mutex] 内调用。
     *
     * @param timeoutMs 等待连接的超时时间
     * @return 服务实例或 `null`
     */
    private suspend fun acquire(timeoutMs: Long): ZeroTierOneService? {
        boundService?.let { return it }
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
        val conn = connection ?: return
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
            store.setBinderState(BinderState.DISCONNECTED)
            if (!deferred.isCompleted) {
                deferred.complete(null)
            }
        }

        override fun onBindingDied(name: ComponentName?) {
            if (connection !== this) return
            boundService = null
            store.setBinderState(BinderState.DISCONNECTED)
            store.setServiceState(ServiceState.STOPPED)
            if (!deferred.isCompleted) {
                deferred.complete(null)
            }
        }

        override fun onNullBinding(name: ComponentName?) {
            if (connection !== this) return
            boundService = null
            store.setBinderState(BinderState.DISCONNECTED)
            if (!deferred.isCompleted) {
                deferred.complete(null)
            }
        }
    }

    companion object {
        private const val TAG = "AndroidVpnCommands"
        private const val PROBE_TIMEOUT_MS = 300L
        private const val CONNECT_TIMEOUT_MS = 500L
        private const val STOP_CONNECT_TIMEOUT_MS = 500L
    }
}
