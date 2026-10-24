package net.kaaass.zerotierfix.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import android.widget.Toast

import com.zerotier.sdk.Event
import com.zerotier.sdk.EventListener
import com.zerotier.sdk.Node
import com.zerotier.sdk.ResultCode
import com.zerotier.sdk.VirtualNetworkConfig
import com.zerotier.sdk.VirtualNetworkConfigListener
import com.zerotier.sdk.VirtualNetworkConfigOperation
import com.zerotier.sdk.util.StringUtils as SdkStringUtils

import dagger.hilt.android.AndroidEntryPoint

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

import net.kaaass.zerotierfix.R
import net.kaaass.zerotierfix.data.database.DatabaseUtils
import net.kaaass.zerotierfix.data.repository.AppNodeRepository
import net.kaaass.zerotierfix.data.repository.MoonOrbitRepository
import net.kaaass.zerotierfix.data.repository.NetworkRepository
import net.kaaass.zerotierfix.data.repository.SettingsRepository
import net.kaaass.zerotierfix.model.dto.MoonOrbitDto
import net.kaaass.zerotierfix.model.dto.NetworkSummary
import net.kaaass.zerotierfix.model.dto.PeerInfoDto
import net.kaaass.zerotierfix.model.dto.VirtualNetworkConfigDto
import net.kaaass.zerotierfix.model.enums.NetworkStatus
import net.kaaass.zerotierfix.util.NetworkInfoUtils
import net.kaaass.zerotierfix.vpn.config.TunnelConfigurator
import net.kaaass.zerotierfix.vpn.config.TunnelHost
import net.kaaass.zerotierfix.vpn.config.VirtualNetworkConfigMapper
import net.kaaass.zerotierfix.vpn.engine.ServiceState
import net.kaaass.zerotierfix.vpn.engine.VpnRuntime
import net.kaaass.zerotierfix.vpn.engine.VpnStateStore
import net.kaaass.zerotierfix.vpn.node.NodeInfoMapper
import net.kaaass.zerotierfix.vpn.node.UdpCom
import net.kaaass.zerotierfix.vpn.store.DataStore
import net.kaaass.zerotierfix.vpn.tun.TunTapAdapter

import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.util.HashMap
import java.util.concurrent.locks.ReentrantReadWriteLock

import javax.inject.Inject

/**
 * ZeroTier 前台 VPN 服务。
 *
 * 生命周期与状态写入：
 * - 节点初始化成功后写 [VpnStateStore] 的 `RUNNING`，失败或停止路径写 `STOPPED`；
 * - 节点状态、已连接网络列表与实时网络配置写入 [VpnStateStore]，由 UI 通过 Flow 读取；
 * - 不再使用 EventBus，所有跨层通知均为状态或 [VpnStateStore] 的事件流。
 *
 * 线程：
 * - `onStartCommand`、`onDestroy`、`onRevoke` 运行在主线程；
 * - 重配任务与节点状态刷新运行在 [serviceScope] 的串行调度器上；
 * - [stopZeroTier] 会 join 工作线程，调用方应位于后台线程（命令层已保证）。
 */
@AndroidEntryPoint
class ZeroTierOneService : VpnService(), Runnable, EventListener,
    VirtualNetworkConfigListener, VpnRuntime, TunnelHost {

    @Inject
    lateinit var networkRepository: NetworkRepository

    @Inject
    lateinit var appNodeRepository: AppNodeRepository

    @Inject
    lateinit var moonOrbitRepository: MoonOrbitRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var store: VpnStateStore

    private val mBinder: IBinder = ZeroTierBinder()
    private val virtualNetworkConfigMap = HashMap<Long, VirtualNetworkConfig>()

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))

    private val tunnelConfigurator by lazy {
        TunnelConfigurator(this, networkRepository)
    }

    private val stopStateLock = Any()
    private var isStopping = false

    private var dataStore: DataStore? = null
    private var input: FileInputStream? = null
    private var output: FileOutputStream? = null
    private var svrSocket: DatagramSocket? = null
    private var vpnSocket: ParcelFileDescriptor? = null

    private var bindCount = 0
    private var disableIPv6 = false
    private var mStartID = -1
    private var networkId = 0L
    private var nextBackgroundTaskDeadline = 0L
    private val notificationController = VpnNotificationController(this)

    /** 是否已通过 [startForeground] 进入前台。 */
    private var foregroundStarted = false
    private var node: Node? = null
    private var tunTapAdapter: TunTapAdapter? = null
    private var udpCom: UdpCom? = null
    private var udpThread: Thread? = null

    private var v4MulticastScanner: MulticastScanner? =
        MulticastScanner({ this.node }, { this.networkId }, false)

    private var v6MulticastScanner: MulticastScanner? =
        MulticastScanner({ this.node }, { this.networkId }, true)

    private var vpnThread: Thread? = null

    override fun onCreate() {
        super.onCreate()
        // Hilt 字段注入在 super.onCreate() 中完成，DataStore 依赖 SettingsRepository，
        // 因此不能使用字段初始化器提前构造。
        this.dataStore = DataStore(this, settingsRepository)
    }

    /**
     * 获取指定网络的实时配置。
     *
     * @param networkId 网络 ID
     * @return 网络配置；尚未收到配置时返回 `null`
     */
    override fun getVirtualNetworkConfig(networkId: Long): VirtualNetworkConfig? {
        synchronized(this.virtualNetworkConfigMap) {
            return this.virtualNetworkConfigMap[networkId]
        }
    }

    /**
     * 保存指定网络的实时配置。
     *
     * @param networkId 网络 ID
     * @param virtualNetworkConfig 网络配置
     * @return 原配置；不存在时返回 `null`
     */
    fun setVirtualNetworkConfig(
        networkId: Long,
        virtualNetworkConfig: VirtualNetworkConfig
    ): VirtualNetworkConfig? {
        synchronized(this.virtualNetworkConfigMap) {
            return this.virtualNetworkConfigMap.put(networkId, virtualNetworkConfig)
        }
    }

    /**
     * 清除指定网络的实时配置。
     *
     * @param networkId 网络 ID
     * @return 被清除的配置；不存在时返回 `null`
     */
    fun clearVirtualNetworkConfig(networkId: Long): VirtualNetworkConfig? {
        synchronized(this.virtualNetworkConfigMap) {
            return this.virtualNetworkConfigMap.remove(networkId)
        }
    }

    private fun logBindCount() {
        Log.i(TAG, "Bind Count: " + this.bindCount)
    }

    override fun onBind(intent: Intent?): IBinder {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Bound by: " + packageManager.getNameForUid(Binder.getCallingUid()))
        }
        this.bindCount++
        logBindCount()
        return this.mBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Unbound by: " + packageManager.getNameForUid(Binder.getCallingUid()))
        }
        this.bindCount--
        logBindCount()
        return false
    }

    /**
     * 设置下一次后台任务截止时间。
     *
     * 线程：可在任意线程调用。
     *
     * @param deadline 截止时间（毫秒时间戳）
     */
    override fun setNextBackgroundTaskDeadline(deadline: Long) {
        synchronized(this) {
            this.nextBackgroundTaskDeadline = deadline
        }
    }

    /**
     * 启动 ZT 服务，连接至给定网络或最近连接的网络。
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        if (intent == null) {
            Log.e(TAG, "NULL intent.  Cannot start")
            markServiceStopped()
            stopSelf(startId)
            return Service.START_NOT_STICKY
        }
        this.mStartID = startId
        store.setServiceState(ServiceState.STARTING)

        // 确定待启动的网络 ID
        val targetNetworkId: Long
        if (intent.hasExtra(ZT1_NETWORK_ID)) {
            // Intent 中指定了目标网络，直接使用此 ID
            targetNetworkId = intent.getLongExtra(ZT1_NETWORK_ID, 0)
        } else {
            // 默认启用最近一次启动的网络
            val lastActivatedNetworkId = networkRepository.getLastActivatedNetworkId()
            if (lastActivatedNetworkId == null) {
                Log.e(TAG, "Couldn't find last activated connection")
                markServiceStopped()
                stopSelf(startId)
                return Service.START_NOT_STICKY
            }
            targetNetworkId = lastActivatedNetworkId
            Log.i(TAG, "Got Always On request for ZeroTier")
        }
        if (targetNetworkId == 0L) {
            Log.e(TAG, "Network ID not provided to service")
            markServiceStopped()
            stopSelf(startId)
            return Service.START_NOT_STICKY
        }
        this.networkId = targetNetworkId

        // 检查当前的网络环境
        val useCellularData = settingsRepository.isCellularDataAllowed
        this.disableIPv6 = settingsRepository.isIpv6Disabled
        val currentNetworkInfo = NetworkInfoUtils.getNetworkInfoCurrentConnection(this)

        if (currentNetworkInfo == NetworkInfoUtils.CurrentConnection.CONNECTION_NONE) {
            // 未连接网络
            Toast.makeText(this, R.string.toast_no_network, Toast.LENGTH_SHORT).show()
            markServiceStopped()
            stopSelf(this.mStartID)
            return Service.START_NOT_STICKY
        } else if (currentNetworkInfo == NetworkInfoUtils.CurrentConnection.CONNECTION_MOBILE &&
            !useCellularData
        ) {
            // 使用移动网络，但未在设置中允许移动网络访问
            Toast.makeText(this, R.string.toast_mobile_data, Toast.LENGTH_LONG).show()
            markServiceStopped()
            stopSelf(this.mStartID)
            return Service.START_NOT_STICKY
        }

        // 启动 ZT 服务
        synchronized(this) {
            try {
                // 创建本地 ZT 服务 Socket，监听本地端口
                if (this.svrSocket == null) {
                    this.svrSocket = DatagramSocket(null)
                    this.svrSocket!!.reuseAddress = true
                    this.svrSocket!!.soTimeout = 1000
                    this.svrSocket!!.bind(InetSocketAddress(9994))
                }
                if (!protect(this.svrSocket!!)) {
                    Log.e(TAG, "Error protecting UDP socket from feedback loop.")
                }

                // 创建本地节点
                if (this.node == null) {
                    val dataStore = this.dataStore!!
                    this.udpCom = UdpCom(this, this.svrSocket)
                    this.tunTapAdapter = TunTapAdapter(this, networkId)

                    // 创建节点对象并初始化
                    this.node = Node(System.currentTimeMillis())
                    val result = this.node!!.init(
                        dataStore, dataStore, this.udpCom!!, this, this.tunTapAdapter!!, this, null
                    )

                    if (result == ResultCode.RESULT_OK) {
                        Log.d(TAG, "ZeroTierOne Node Initialized")
                    } else {
                        Log.e(TAG, "Error starting ZT1 Node: $result")
                        markServiceStopped()
                        stopSelf(startId)
                        return Service.START_NOT_STICKY
                    }

                    // 持久化当前节点信息
                    val address = this.node!!.address()
                    appNodeRepository.saveNodeId(address)
                    store.setNodeId(appNodeRepository.getNodeIdStr())

                    this.udpCom!!.setNode(this.node!!)
                    this.tunTapAdapter!!.setNode(this.node!!)

                    // 启动 UDP 消息处理线程
                    val thread = Thread(this.udpCom!!, "UDP Communication Thread")
                    this.udpThread = thread
                    thread.start()
                }

                // 创建并启动 VPN 服务线程
                if (this.vpnThread == null) {
                    val thread = Thread(this, "ZeroTier Service Thread")
                    this.vpnThread = thread
                    thread.start()
                }

                // 启动 UDP 消息处理线程
                if (!this.udpThread!!.isAlive) {
                    this.udpThread!!.start()
                }
            } catch (e: Exception) {
                Log.e(TAG, e.toString(), e)
                markServiceStopped()
                stopSelf(startId)
                return Service.START_NOT_STICKY
            }
        }
        store.setServiceState(ServiceState.RUNNING)
        refreshNodeStatus()
        refreshNetworkListState()
        joinNetwork(targetNetworkId)
        return Service.START_STICKY
    }

    /**
     * 停止 ZeroTier 服务并释放资源。
     *
     * 线程：会短暂 join UDP / TUN / VPN 线程，应在后台线程调用；
     * `onDestroy` 等主线程路径依赖 [TEARDOWN_TIMEOUT_MS] 的总预算避免 ANR。
     * 幂等：已有停止流程在进行时立即返回，避免 `onDestroy` 或 UDP 线程
     * 等待命令线程而互相阻塞。
     *
     * 顺序很重要：先关闭 TUN fd 拆掉隧道并撤销通知，再等待工作线程退出。
     * 工作线程可能阻塞在 native 调用或 TUN 读上，`interrupt()` 不保证唤醒，
     * 因此 join 必须有超时；即使线程未退出，也要保证隧道、通知和状态已清理。
     */
    fun stopZeroTier() {
        synchronized(stopStateLock) {
            if (this.isStopping) {
                return
            }
            this.isStopping = true
        }
        try {
            val deadline = SystemClock.elapsedRealtime() + TEARDOWN_TIMEOUT_MS

            // 先拆隧道：关闭 TUN fd 后系统 VPN 图标立即消失，并尽量唤醒阻塞的读线程。
            closeVpnResources()

            // 关闭 UDP socket，停止 SDK 的收发。
            val socket = this.svrSocket
            this.svrSocket = null
            try {
                socket?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing UDP socket", e)
            }

            // 先撤销通知/前台状态，后续任何一步卡住都不会留下“已连接”假象。
            leaveForeground()

            var threadsStopped = joinThread(this.udpThread, deadline, "UDP")
            this.udpThread = null

            val adapter = this.tunTapAdapter
            if (adapter != null) {
                threadsStopped = adapter.interrupt() && threadsStopped
                this.tunTapAdapter = null
            }

            threadsStopped = joinThread(this.vpnThread, deadline, "VPN") && threadsStopped
            this.vpnThread = null

            threadsStopped =
                joinThread(this.v4MulticastScanner, deadline, "IPv4 multicast") && threadsStopped
            this.v4MulticastScanner = null

            threadsStopped =
                joinThread(this.v6MulticastScanner, deadline, "IPv6 multicast") && threadsStopped
            this.v6MulticastScanner = null

            closeVpnSocket()
            val node = this.node
            if (node != null) {
                try {
                    store.setNodeStatus(null)
                    if (threadsStopped) {
                        node.close()
                    } else {
                        // 仍有线程可能正在调用 SDK；此时关闭 node 可能导致 native 崩溃。
                        Log.w(TAG, "Skip node.close(): worker threads still alive")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing node", e)
                }
                this.node = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "stopZeroTier failed", e)
        } finally {
            store.setServiceState(ServiceState.STOPPED)
            store.setConnectedNetworks(null)
            store.clearVirtualNetworkConfigs()
            leaveForeground()
            if (!stopSelfResult(this.mStartID)) {
                Log.e(TAG, "stopSelfResult() failed!")
            }
            synchronized(stopStateLock) {
                this.isStopping = false
            }
        }
    }

    /**
     * 在截止时间前中断并等待工作线程退出。
     *
     * 超时只记录日志，不抛出异常：调用方仍需继续清理隧道与状态。
     *
     * @param thread 目标线程
     * @param deadline 总预算的截止时间（[SystemClock.elapsedRealtime]）
     * @param name 线程名，用于日志
     * @return 线程已退出（或无需等待）时返回 `true`
     */
    private fun joinThread(thread: Thread?, deadline: Long, name: String): Boolean {
        if (thread == null || thread === Thread.currentThread() || !thread.isAlive) {
            return true
        }
        thread.interrupt()
        val remaining = deadline - SystemClock.elapsedRealtime()
        if (remaining <= 0L) {
            Log.w(TAG, "$name thread did not stop before teardown deadline")
            return false
        }
        try {
            thread.join(remaining)
        } catch (e: InterruptedException) {
            Log.w(TAG, "Interrupted while waiting for $name thread")
        }
        if (thread.isAlive) {
            Log.w(TAG, "$name thread still alive after ${remaining}ms")
            return false
        }
        return true
    }

    override fun onDestroy() {
        try {
            stopZeroTier()
            closeVpnSocket()
            stopSelf(this.mStartID)
        } catch (e: Exception) {
            Log.e(TAG, e.toString(), e)
        } finally {
            leaveForeground()
            serviceScope.cancel()
            super.onDestroy()
        }
    }

    override fun onRevoke() {
        try {
            stopZeroTier()
            closeVpnSocket()
            stopSelf(this.mStartID)
        } finally {
            leaveForeground()
        }
        super.onRevoke()
    }

    override fun run() {
        Log.d(TAG, "ZeroTierOne Service Started")
        Log.d(TAG, "This Node Address: " + SdkStringUtils.addressToString(this.node!!.address()))
        while (!Thread.interrupted()) {
            try {
                // 在后台任务截止期前循环进行后台任务
                val taskDeadline = this.nextBackgroundTaskDeadline
                val currentTime = System.currentTimeMillis()
                val cmp = java.lang.Long.compare(taskDeadline, currentTime)
                if (cmp <= 0) {
                    val newDeadline = longArrayOf(0)
                    val taskResult = this.node!!.processBackgroundTasks(currentTime, newDeadline)
                    synchronized(this) {
                        this.nextBackgroundTaskDeadline = newDeadline[0]
                    }
                    if (taskResult != ResultCode.RESULT_OK) {
                        Log.e(TAG, "Error on processBackgroundTasks: " + taskResult.toString())
                        shutdown()
                    }
                }
                Thread.sleep(if (cmp > 0) taskDeadline - currentTime else 100L)
            } catch (ignored: InterruptedException) {
                break
            } catch (e: Exception) {
                Log.e(TAG, e.toString(), e)
            }
        }
        Log.d(TAG, "ZeroTierOne Service Ended")
    }

    /**
     * 加入 ZT 网络。
     *
     * 线程：可在任意线程调用；内部同步调用 SDK。
     *
     * @param networkId 网络 ID
     */
    fun joinNetwork(networkId: Long) {
        val node = this.node
        if (node == null) {
            Log.e(TAG, "Can't join network if ZeroTier isn't running")
            return
        }
        // 连接到新网络
        val result = node.join(networkId)
        if (result != ResultCode.RESULT_OK) {
            Log.e(
                TAG,
                "Failed to join network " + java.lang.Long.toHexString(networkId) + ": " + result
            )
            return
        }
        // 连接后事件：入轨全部已保存的 Moon，并刷新已连接网络
        orbitMoons(moonOrbitRepository.getAll())
        refreshNetworkListState()
    }

    /**
     * 离开 ZT 网络。
     *
     * 线程：可在任意线程调用；内部同步调用 SDK。
     * 若离开后没有剩余网络，则停止服务并释放 VPN socket。
     *
     * @param networkId 网络 ID
     */
    fun leaveNetwork(networkId: Long) {
        val node = this.node
        if (node == null) {
            Log.e(TAG, "Can't leave network if ZeroTier isn't running")
            return
        }
        val result = node.leave(networkId)
        if (result != ResultCode.RESULT_OK) {
            Log.e(
                TAG,
                "Failed to leave network " + java.lang.Long.toHexString(networkId) + ": " + result
            )
            return
        }
        refreshNetworkListState()
        val networkConfigs = node.networkConfigs()
        if (networkConfigs != null && networkConfigs.isNotEmpty()) {
            return
        }
        stopZeroTier()
        closeVpnSocket()
        stopSelf(this.mStartID)
    }

    /**
     * 刷新节点在线状态到 [VpnStateStore]。
     *
     * 线程：可在任意线程调用；节点未初始化时静默返回。
     */
    fun refreshNodeStatus() {
        val node = this.node ?: return
        store.setNodeStatus(NodeInfoMapper.toDto(node.status(), node.version))
    }

    /**
     * 刷新已连接网络列表到 [VpnStateStore]。
     *
     * 线程：可在任意线程调用。
     * 服务未运行或 `networkConfigs()` 为空时不更新，保持原“无应答则保留旧值”的行为。
     */
    fun refreshNetworkListState() {
        val node = this.node ?: return
        val networks = node.networkConfigs() ?: return
        if (networks.isEmpty()) {
            return
        }
        store.setConnectedNetworks(VirtualNetworkConfigMapper.toDtoList(networks))
    }

    /**
     * 获取 Peer 列表。
     *
     * 线程：可在任意线程调用。
     *
     * @return Peer 列表；节点未初始化时返回 `null`
     */
    fun getPeers(): List<PeerInfoDto>? {
        val node = this.node ?: return null
        return NodeInfoMapper.toDto(node.peers())
    }

    /**
     * 获取指定网络的实时配置 DTO。
     *
     * 线程：可在任意线程调用。
     *
     * @param networkId 网络 ID
     * @return 配置 DTO；节点未初始化或网络无配置时返回 `null`
     */
    fun getVirtualNetworkConfigDto(networkId: Long): VirtualNetworkConfigDto? {
        val node = this.node ?: return null
        val config = node.networkConfig(networkId) ?: return null
        return VirtualNetworkConfigMapper.toDto(config)
    }

    /**
     * 用户修改网络配置后重配隧道。
     *
     * 线程：可在任意线程调用；重配任务在 [serviceScope] 上异步执行。
     *
     * @param networkId 网络 ID
     */
    fun reconfigureNetwork(networkId: Long) {
        if (networkId != this.networkId) {
            return
        }
        val network = networkRepository.getNetworkSummary(networkId)
        if (network == null) {
            Log.e(TAG, "Network not found: $networkId")
            return
        }
        serviceScope.launch { updateTunnelConfig(network) }
    }

    /**
     * 入轨多个 Moon。
     *
     * 线程：可在任意线程调用；节点未初始化时静默返回。
     *
     * @param moons Moon 列表
     */
    fun orbitMoons(moons: List<MoonOrbitDto>) {
        if (this.node == null) {
            Log.e(TAG, "Can't orbit network if ZeroTier isn't running")
            return
        }
        for (moonOrbit in moons) {
            Log.i(TAG, "Orbiting moon: " + java.lang.Long.toHexString(moonOrbit.moonWorldId))
            orbitNetwork(moonOrbit.moonWorldId, moonOrbit.moonSeed)
        }
    }

    /**
     * 当前网络入轨 Moon。
     *
     * @param moonWorldId Moon 节点地址
     * @param moonSeed Moon 种子节点地址
     */
    fun orbitNetwork(moonWorldId: Long?, moonSeed: Long?) {
        val node = this.node
        if (node == null) {
            Log.e(TAG, "Can't orbit network if ZeroTier isn't running")
            return
        }
        // 入轨
        val result = node.orbit(moonWorldId!!, moonSeed!!)
        if (result != ResultCode.RESULT_OK) {
            Log.e(
                TAG,
                "Failed to orbit " + java.lang.Long.toHexString(moonWorldId!!) + ": " + result
            )
        }
    }

    /**
     * Zerotier 事件回调。
     *
     * @param event [Event] 枚举
     */
    override fun onEvent(event: Event) {
        Log.d(TAG, "Event: " + event.toString())
        // 更新节点状态
        val node = this.node ?: return
        if (node.isInited()) {
            store.setNodeStatus(NodeInfoMapper.toDto(node.status(), node.version))
        }
    }

    override fun onTrace(str: String) {
        Log.d(TAG, "Trace: $str")
    }

    /**
     * 当 ZT 网络配置发生更新。
     *
     * 数据库与实时配置在写锁内同步；隧道重配任务在锁释放后提交到 [serviceScope]，
     * 避免在数据库锁内等待 TUN/UDP 线程。
     */
    override fun onNetworkConfigurationUpdated(
        networkId: Long,
        op: VirtualNetworkConfigOperation,
        config: VirtualNetworkConfig?
    ): Int {
        Log.i(TAG, "Virtual Network Config Operation: $op")
        var reconfigure: ReconfigureRequest? = null
        DatabaseUtils.writeLock.lock()
        try {
            // 查找网络 ID 对应的配置
            val network = networkRepository.getNetworkSummary(networkId)
                ?: throw IllegalStateException("Database is inconsistent")
            // 根据当前网络状态确定更改配置的行为
            when (op) {
                VirtualNetworkConfigOperation.VIRTUAL_NETWORK_CONFIG_OPERATION_UP -> {
                    val cfg = config!!
                    Log.d(
                        TAG,
                        "Network Type: " + cfg.type + " Network Status: " + cfg.status +
                            " Network Name: " + cfg.name + " "
                    )
                    // 将网络配置的更新交给第一次 Update
                }
                VirtualNetworkConfigOperation.VIRTUAL_NETWORK_CONFIG_OPERATION_CONFIG_UPDATE -> {
                    Log.i(TAG, "Network Config Update!")
                    val cfg = config!!
                    val isChanged = setVirtualNetworkConfigAndUpdateDatabase(network, cfg)
                    val dto = VirtualNetworkConfigMapper.toDto(cfg)
                    store.setVirtualNetworkConfig(networkId, dto)
                    reconfigure = ReconfigureRequest(isChanged, network, dto)
                }
                VirtualNetworkConfigOperation.VIRTUAL_NETWORK_CONFIG_OPERATION_DOWN,
                VirtualNetworkConfigOperation.VIRTUAL_NETWORK_CONFIG_OPERATION_DESTROY -> {
                    Log.d(TAG, "Network Down!")
                    clearVirtualNetworkConfig(networkId)
                    store.removeVirtualNetworkConfig(networkId)
                }
                else -> {
                }
            }
        } finally {
            DatabaseUtils.writeLock.unlock()
        }
        reconfigure?.let { request ->
            serviceScope.launch { applyReconfigure(request) }
        }
        return 0
    }

    /**
     * 应用网络配置变更：必要时重配隧道，并按原条件发出状态提示。
     *
     * 运行在 [serviceScope] 上。
     *
     * @param request 配置变更请求
     */
    private fun applyReconfigure(request: ReconfigureRequest) {
        val configUpdated = request.isChanged && updateTunnelConfig(request.network)
        val networkIsOk = request.config.status == NetworkStatus.OK
        if (configUpdated || !networkIsOk) {
            store.emitStatusNotice(request.config)
        }
    }

    private fun setVirtualNetworkConfigAndUpdateDatabase(
        network: NetworkSummary,
        virtualNetworkConfig: VirtualNetworkConfig
    ): Boolean {
        val writeLock = DatabaseUtils.writeLock
        if (writeLock is ReentrantReadWriteLock.WriteLock && !writeLock.isHeldByCurrentThread) {
            throw IllegalStateException("DatabaseUtils.writeLock not held")
        }
        val networkId = network.networkId
        val virtualNetworkConfig2 = getVirtualNetworkConfig(networkId)
        setVirtualNetworkConfig(networkId, virtualNetworkConfig)
        val networkName = virtualNetworkConfig.name
        if (!networkName.isNullOrEmpty()) {
            networkRepository.updateNetworkName(networkId, networkName)
        }
        return virtualNetworkConfig != virtualNetworkConfig2
    }

    /**
     * 关闭 ZeroTier 服务并释放 VPN socket。
     *
     * 供 UDP 通信失败等致命错误路径调用；可在任意线程调用。
     */
    override fun shutdown() {
        stopZeroTier()
        closeVpnSocket()
        stopSelf(this.mStartID)
    }

    /**
     * 关闭 VPN socket。
     */
    private fun closeVpnSocket() {
        val socket = this.vpnSocket ?: return
        try {
            socket.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN socket", e)
        }
        this.vpnSocket = null
    }

    /**
     * 启动失败时同步服务状态，避免 UI 停留在 `STARTING`。
     */
    private fun markServiceStopped() {
        store.setServiceState(ServiceState.STOPPED)
        store.setNodeStatus(null)
        store.setConnectedNetworks(null)
        store.clearVirtualNetworkConfigs()
        leaveForeground()
    }

    /**
     * 应用指定网络的最新配置。
     *
     * @param network 数据库中的网络摘要
     * @return 配置成功返回 `true`
     */
    private fun updateTunnelConfig(network: NetworkSummary): Boolean =
        tunnelConfigurator.configure(network, this.disableIPv6)

    override val vpnService: VpnService
        get() = this

    override val currentNode: Node?
        get() = this.node

    override val currentAdapter: TunTapAdapter?
        get() = this.tunTapAdapter

    override val vpnNotPreparedMessage: String
        get() = getString(R.string.toast_vpn_application_not_prepared)

    override fun readVirtualNetworkConfig(networkId: Long): VirtualNetworkConfig? =
        getVirtualNetworkConfig(networkId)

    override fun restartTunnelAdapter() {
        val adapter = this.tunTapAdapter ?: return
        if (adapter.isRunning()) {
            // interrupt() 内部已带超时等待接收线程退出。
            adapter.interrupt()
        }
    }

    override fun closeVpnResources() {
        if (this.vpnSocket == null) {
            return
        }
        try {
            this.vpnSocket!!.close()
            this.input!!.close()
            this.output!!.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN socket: $e", e)
        }
        this.vpnSocket = null
        this.input = null
        this.output = null
    }

    override fun onTunnelEstablished(
        socket: ParcelFileDescriptor,
        input: FileInputStream,
        output: FileOutputStream
    ) {
        this.vpnSocket = socket
        this.input = input
        this.output = output
        val adapter = this.tunTapAdapter ?: return
        adapter.setVpnSocket(socket)
        adapter.setFileStreams(input, output)
        adapter.startThreads()
    }

    override fun showConnectedNotification(networkIdStr: String?) {
        enterForeground(networkIdStr)
    }

    /**
     * 进入前台服务：通知由系统托管，服务/进程结束时自动移除。
     *
     * 先构建通知再尝试 `startForeground`；个别 ROM 可能因前台服务类型
     * 校验失败，此时降级为普通常驻通知，不影响连接本身。
     *
     * @param networkIdStr 网络 ID 的字符串形式
     */
    private fun enterForeground(networkIdStr: String?) {
        val notification = notificationController.buildConnected(networkIdStr)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    VpnNotificationController.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
                )
            } else {
                startForeground(VpnNotificationController.NOTIFICATION_ID, notification)
            }
            this.foregroundStarted = true
        } catch (e: Exception) {
            Log.w(TAG, "startForeground failed; falling back to normal notification", e)
            this.foregroundStarted = false
            notificationController.notifyConnected(networkIdStr)
        }
    }

    /**
     * 退出前台并移除通知。
     *
     * 幂等：`startForeground` 未成功时也会按 ID 取消，避免旧通知残留。
     */
    private fun leaveForeground() {
        if (this.foregroundStarted) {
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } catch (e: Exception) {
                Log.e(TAG, "stopForeground failed", e)
            }
        }
        this.foregroundStarted = false
        this.notificationController.cancel()
    }

    override fun startMulticastScanners(disableIpv6: Boolean) {
        if (Build.VERSION.SDK_INT >= 29) {
            return
        }
        val v4 = this.v4MulticastScanner
        if (v4 != null && !v4.isAlive) {
            v4.start()
        }
        val v6 = this.v6MulticastScanner
        if (!disableIpv6 && v6 != null && !v6.isAlive) {
            v6.start()
        }
    }

    override fun emitTunnelError(message: String) {
        store.emitError(message)
    }

    inner class ZeroTierBinder : Binder() {

        fun getService(): ZeroTierOneService = this@ZeroTierOneService
    }

    /**
     * 网络配置变更后的重配请求。
     *
     * @property isChanged 配置内容是否发生变化
     * @property network 数据库中的网络摘要
     * @property config 变化后的实时配置
     */
    private data class ReconfigureRequest(
        val isChanged: Boolean,
        val network: NetworkSummary,
        val config: VirtualNetworkConfigDto
    )

    companion object {
        const val MSG_JOIN_NETWORK = 1
        const val MSG_LEAVE_NETWORK = 2
        const val ZT1_NETWORK_ID = "com.zerotier.one.network_id"
        const val ZT1_USE_DEFAULT_ROUTE = "com.zerotier.one.use_default_route"

        /** 停止流程等待工作线程退出的总预算，避免主线程路径 ANR。 */
        private const val TEARDOWN_TIMEOUT_MS = 2_000L

        private const val TAG = "ZT1_Service"
    }
}
