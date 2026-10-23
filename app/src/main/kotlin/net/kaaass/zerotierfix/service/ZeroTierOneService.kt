package net.kaaass.zerotierfix.service

import android.app.Service
import android.content.Intent
import android.net.VpnService
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.preference.PreferenceManager
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

import net.kaaass.zerotierfix.R
import net.kaaass.zerotierfix.ZerotierFixApplication
import net.kaaass.zerotierfix.data.database.DatabaseUtils
import net.kaaass.zerotierfix.data.repository.NetworkRepository
import net.kaaass.zerotierfix.events.AfterJoinNetworkEvent
import net.kaaass.zerotierfix.events.IsServiceRunningReplyEvent
import net.kaaass.zerotierfix.events.IsServiceRunningRequestEvent
import net.kaaass.zerotierfix.events.NetworkConfigChangedByUserEvent
import net.kaaass.zerotierfix.events.NetworkListReplyEvent
import net.kaaass.zerotierfix.events.NetworkListRequestEvent
import net.kaaass.zerotierfix.events.NetworkReconfigureEvent
import net.kaaass.zerotierfix.events.NodeDestroyedEvent
import net.kaaass.zerotierfix.events.NodeIDEvent
import net.kaaass.zerotierfix.events.NodeStatusEvent
import net.kaaass.zerotierfix.events.NodeStatusRequestEvent
import net.kaaass.zerotierfix.events.OrbitMoonEvent
import net.kaaass.zerotierfix.events.PeerInfoReplyEvent
import net.kaaass.zerotierfix.events.PeerInfoRequestEvent
import net.kaaass.zerotierfix.events.StopEvent
import net.kaaass.zerotierfix.events.VPNErrorEvent
import net.kaaass.zerotierfix.events.VirtualNetworkConfigChangedEvent
import net.kaaass.zerotierfix.events.VirtualNetworkConfigReplyEvent
import net.kaaass.zerotierfix.events.VirtualNetworkConfigRequestEvent
import net.kaaass.zerotierfix.model.dto.MoonOrbitDto
import net.kaaass.zerotierfix.model.dto.NetworkConfigDto
import net.kaaass.zerotierfix.model.dto.NetworkSummary
import net.kaaass.zerotierfix.model.enums.NetworkStatus
import net.kaaass.zerotierfix.util.Constants
import net.kaaass.zerotierfix.util.InetAddressUtils
import net.kaaass.zerotierfix.util.NetworkInfoUtils
import net.kaaass.zerotierfix.vpn.config.DnsConfigurator
import net.kaaass.zerotierfix.vpn.config.VirtualNetworkConfigMapper
import net.kaaass.zerotierfix.vpn.node.NodeInfoMapper
import net.kaaass.zerotierfix.vpn.node.UdpCom
import net.kaaass.zerotierfix.vpn.route.Route
import net.kaaass.zerotierfix.vpn.store.DataStore
import net.kaaass.zerotierfix.vpn.tun.TunTapAdapter

import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramSocket
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.HashMap
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * ZeroTier 前台 VPN 服务。
 */
class ZeroTierOneService : VpnService(), Runnable, EventListener,
    VirtualNetworkConfigListener {

    private val mBinder: IBinder = ZeroTierBinder()
    private val dataStore = DataStore(this)
    private val eventBus = EventBus.getDefault()
    private val virtualNetworkConfigMap = HashMap<Long, VirtualNetworkConfig>()

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
    private var node: Node? = null
    private var tunTapAdapter: TunTapAdapter? = null
    private var udpCom: UdpCom? = null
    private var udpThread: Thread? = null

    private var v4MulticastScanner: MulticastScanner? =
        MulticastScanner({ this.node }, { this.networkId }, false)

    private var v6MulticastScanner: MulticastScanner? =
        MulticastScanner({ this.node }, { this.networkId }, true)

    private var vpnThread: Thread? = null

    /**
     * 获取指定网络的实时配置。
     *
     * @param networkId 网络 ID
     * @return 网络配置；尚未收到配置时返回 `null`
     */
    fun getVirtualNetworkConfig(networkId: Long): VirtualNetworkConfig? {
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
        Log.d(TAG, "Bound by: " + packageManager.getNameForUid(Binder.getCallingUid()))
        this.bindCount++
        logBindCount()
        return this.mBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "Unbound by: " + packageManager.getNameForUid(Binder.getCallingUid()))
        this.bindCount--
        logBindCount()
        return false
    }

    /**
     * 设置下一次后台任务截止时间。
     *
     * @param deadline 截止时间（毫秒时间戳）
     */
    fun setNextBackgroundTaskDeadline(deadline: Long) {
        synchronized(this) {
            this.nextBackgroundTaskDeadline = deadline
        }
    }

    /**
     * 获取网络数据仓库。
     *
     * @return 网络仓库，非空
     */
    private fun getNetworkRepository(): NetworkRepository =
        (application as ZerotierFixApplication).appContainer.networkRepository

    /**
     * 启动 ZT 服务，连接至给定网络或最近连接的网络。
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        if (startId == 3) {
            Log.i(TAG, "Authorizing VPN")
            return Service.START_NOT_STICKY
        } else if (intent == null) {
            Log.e(TAG, "NULL intent.  Cannot start")
            return Service.START_NOT_STICKY
        }
        this.mStartID = startId

        // 注册事件总线监听器
        if (!this.eventBus.isRegistered(this)) {
            this.eventBus.register(this)
        }

        // 确定待启动的网络 ID
        val targetNetworkId: Long
        if (intent.hasExtra(ZT1_NETWORK_ID)) {
            // Intent 中指定了目标网络，直接使用此 ID
            targetNetworkId = intent.getLongExtra(ZT1_NETWORK_ID, 0)
        } else {
            // 默认启用最近一次启动的网络
            val lastActivatedNetworkId = getNetworkRepository().getLastActivatedNetworkId()
            if (lastActivatedNetworkId == null) {
                Log.e(TAG, "Couldn't find last activated connection")
                return Service.START_NOT_STICKY
            }
            targetNetworkId = lastActivatedNetworkId
            Log.i(TAG, "Got Always On request for ZeroTier")
        }
        if (targetNetworkId == 0L) {
            Log.e(TAG, "Network ID not provided to service")
            stopSelf(startId)
            return Service.START_NOT_STICKY
        }
        this.networkId = targetNetworkId

        // 检查当前的网络环境
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val useCellularData =
            preferences.getBoolean(Constants.PREF_NETWORK_USE_CELLULAR_DATA, false)
        this.disableIPv6 = preferences.getBoolean(Constants.PREF_NETWORK_DISABLE_IPV6, false)
        val currentNetworkInfo = NetworkInfoUtils.getNetworkInfoCurrentConnection(this)

        if (currentNetworkInfo == NetworkInfoUtils.CurrentConnection.CONNECTION_NONE) {
            // 未连接网络
            Toast.makeText(this, R.string.toast_no_network, Toast.LENGTH_SHORT).show()
            stopSelf(this.mStartID)
            return Service.START_NOT_STICKY
        } else if (currentNetworkInfo == NetworkInfoUtils.CurrentConnection.CONNECTION_MOBILE &&
            !useCellularData
        ) {
            // 使用移动网络，但未在设置中允许移动网络访问
            Toast.makeText(this, R.string.toast_mobile_data, Toast.LENGTH_LONG).show()
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
                    this.udpCom = UdpCom(this, this.svrSocket)
                    this.tunTapAdapter = TunTapAdapter(this, networkId)

                    // 创建节点对象并初始化
                    val dataStore = this.dataStore
                    this.node = Node(System.currentTimeMillis())
                    val result = this.node!!.init(
                        dataStore, dataStore, this.udpCom!!, this, this.tunTapAdapter!!, this, null
                    )

                    if (result == ResultCode.RESULT_OK) {
                        Log.d(TAG, "ZeroTierOne Node Initialized")
                    } else {
                        Log.e(TAG, "Error starting ZT1 Node: $result")
                        return Service.START_NOT_STICKY
                    }
                    this.onNodeStatusRequest(null)

                    // 持久化当前节点信息
                    val address = this.node!!.address()
                    (application as ZerotierFixApplication)
                        .appContainer
                        .appNodeRepository
                        .saveNodeId(address)

                    this.eventBus.post(NodeIDEvent(address))
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
                return Service.START_NOT_STICKY
            }
        }
        joinNetwork(targetNetworkId)
        return Service.START_STICKY
    }

    /**
     * 停止 ZeroTier 服务并释放资源。
     */
    fun stopZeroTier() {
        val socket = this.svrSocket
        if (socket != null) {
            socket.close()
            this.svrSocket = null
        }
        val udp = this.udpThread
        if (udp != null && udp.isAlive) {
            udp.interrupt()
            try {
                udp.join()
            } catch (ignored: InterruptedException) {
            }
            this.udpThread = null
        }
        val adapter = this.tunTapAdapter
        if (adapter != null && adapter.isRunning()) {
            adapter.interrupt()
            try {
                adapter.join()
            } catch (ignored: InterruptedException) {
            }
            this.tunTapAdapter = null
        }
        val vpn = this.vpnThread
        if (vpn != null && vpn.isAlive) {
            vpn.interrupt()
            try {
                vpn.join()
            } catch (ignored: InterruptedException) {
            }
            this.vpnThread = null
        }
        val v4 = this.v4MulticastScanner
        if (v4 != null) {
            v4.interrupt()
            try {
                v4.join()
            } catch (ignored: InterruptedException) {
            }
            this.v4MulticastScanner = null
        }
        val v6 = this.v6MulticastScanner
        if (v6 != null) {
            v6.interrupt()
            try {
                v6.join()
            } catch (ignored: InterruptedException) {
            }
            this.v6MulticastScanner = null
        }
        val socket2 = this.vpnSocket
        if (socket2 != null) {
            try {
                socket2.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing VPN socket: $e", e)
            }
            this.vpnSocket = null
        }
        val node = this.node
        if (node != null) {
            this.eventBus.post(NodeDestroyedEvent())
            node.close()
            this.node = null
        }
        if (this.eventBus.isRegistered(this)) {
            this.eventBus.unregister(this)
        }
        this.notificationController.cancel()
        if (!stopSelfResult(this.mStartID)) {
            Log.e(TAG, "stopSelfResult() failed!")
        }
    }

    override fun onDestroy() {
        try {
            stopZeroTier()
            val socket = this.vpnSocket
            if (socket != null) {
                try {
                    socket.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing VPN socket: $e", e)
                }
                this.vpnSocket = null
            }
            stopSelf(this.mStartID)
            if (this.eventBus.isRegistered(this)) {
                this.eventBus.unregister(this)
            }
        } catch (e: Exception) {
            Log.e(TAG, e.toString(), e)
        } finally {
            super.onDestroy()
        }
    }

    override fun onRevoke() {
        stopZeroTier()
        val socket = this.vpnSocket
        if (socket != null) {
            try {
                socket.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing VPN socket: $e", e)
            }
            this.vpnSocket = null
        }
        stopSelf(this.mStartID)
        if (this.eventBus.isRegistered(this)) {
            this.eventBus.unregister(this)
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

    @Subscribe(threadMode = ThreadMode.POSTING)
    fun onStopEvent(stopEvent: StopEvent) {
        stopZeroTier()
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    fun onIsServiceRunningRequest(event: IsServiceRunningRequestEvent) {
        this.eventBus.post(IsServiceRunningReplyEvent(true))
    }

    /**
     * 加入 ZT 网络。
     *
     * @param networkId 网络 ID
     */
    fun joinNetwork(networkId: Long) {
        if (this.node == null) {
            Log.e(TAG, "Can't join network if ZeroTier isn't running")
            return
        }
        // 连接到新网络
        val result = this.node!!.join(networkId)
        if (result != ResultCode.RESULT_OK) {
            Log.e(
                TAG,
                "Failed to join network " + java.lang.Long.toHexString(networkId) + ": " + result
            )
            return
        }
        // 连接后事件
        this.eventBus.post(AfterJoinNetworkEvent())
    }

    /**
     * 离开 ZT 网络。
     *
     * @param networkId 网络 ID
     */
    fun leaveNetwork(networkId: Long) {
        if (this.node == null) {
            Log.e(TAG, "Can't leave network if ZeroTier isn't running")
            return
        }
        val result = this.node!!.leave(networkId)
        if (result != ResultCode.RESULT_OK) {
            Log.e(
                TAG,
                "Failed to leave network " + java.lang.Long.toHexString(networkId) + ": " + result
            )
            return
        }
        val networkConfigs = this.node!!.networkConfigs()
        if (networkConfigs != null && networkConfigs.isNotEmpty()) {
            return
        }
        stopZeroTier()
        val socket = this.vpnSocket
        if (socket != null) {
            try {
                socket.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing VPN socket", e)
            }
            this.vpnSocket = null
        }
        stopSelf(this.mStartID)
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onNetworkListRequest(requestNetworkListEvent: NetworkListRequestEvent) {
        val node2 = this.node ?: return
        val networks = node2.networkConfigs() ?: return
        if (networks.isEmpty()) {
            return
        }
        this.eventBus.post(
            NetworkListReplyEvent(VirtualNetworkConfigMapper.toDtoList(networks))
        )
    }

    /**
     * 请求节点状态事件回调。
     *
     * @param event 事件；内部可直接传 `null` 以主动刷新状态
     */
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onNodeStatusRequest(event: NodeStatusRequestEvent?) {
        // 返回节点状态
        val node = this.node
        if (node != null) {
            this.eventBus.post(
                NodeStatusEvent(NodeInfoMapper.toDto(node.status(), node.version))
            )
        }
    }

    /**
     * 请求 Peer 信息事件回调。
     */
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onRequestPeerInfo(event: PeerInfoRequestEvent) {
        val node = this.node
        if (node == null) {
            this.eventBus.post(PeerInfoReplyEvent(null))
            return
        }
        this.eventBus.post(PeerInfoReplyEvent(NodeInfoMapper.toDto(node.peers())))
    }

    /**
     * 请求网络配置事件回调。
     */
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onVirtualNetworkConfigRequest(event: VirtualNetworkConfigRequestEvent) {
        val node = this.node
        if (node == null) {
            this.eventBus.post(VirtualNetworkConfigReplyEvent(null))
            return
        }
        val config = node.networkConfig(event.networkId)
        this.eventBus.post(
            VirtualNetworkConfigReplyEvent(
                if (config == null) null else VirtualNetworkConfigMapper.toDto(config)
            )
        )
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onNetworkReconfigure(event: NetworkReconfigureEvent) {
        val isChanged = event.isChanged
        val network = event.network
        val networkConfig = event.virtualNetworkConfig
        val configUpdated = isChanged && updateTunnelConfig(network)
        val networkIsOk = networkConfig.status == NetworkStatus.OK

        if (configUpdated || !networkIsOk) {
            this.eventBus.post(VirtualNetworkConfigChangedEvent(networkConfig))
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onNetworkConfigChangedByUser(event: NetworkConfigChangedByUserEvent) {
        if (event.networkId != this.networkId) {
            return
        }
        val network = getNetworkRepository().getNetworkSummary(event.networkId)
        if (network == null) {
            Log.e(TAG, "Network not found: " + event.networkId)
            return
        }
        updateTunnelConfig(network)
    }

    /**
     * Zerotier 事件回调。
     *
     * @param event [Event] 枚举
     */
    override fun onEvent(event: Event) {
        Log.d(TAG, "Event: " + event.toString())
        // 更新节点状态
        if (this.node!!.isInited()) {
            this.eventBus.post(
                NodeStatusEvent(NodeInfoMapper.toDto(this.node!!.status(), this.node!!.version))
            )
        }
    }

    override fun onTrace(str: String) {
        Log.d(TAG, "Trace: $str")
    }

    /**
     * 当 ZT 网络配置发生更新。
     */
    override fun onNetworkConfigurationUpdated(
        networkId: Long,
        op: VirtualNetworkConfigOperation,
        config: VirtualNetworkConfig?
    ): Int {
        Log.i(TAG, "Virtual Network Config Operation: $op")
        DatabaseUtils.writeLock.lock()
        try {
            // 查找网络 ID 对应的配置
            val network = getNetworkRepository().getNetworkSummary(networkId)
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
                    this.eventBus.post(
                        NetworkReconfigureEvent(
                            isChanged,
                            network,
                            VirtualNetworkConfigMapper.toDto(cfg)
                        )
                    )
                }
                VirtualNetworkConfigOperation.VIRTUAL_NETWORK_CONFIG_OPERATION_DOWN,
                VirtualNetworkConfigOperation.VIRTUAL_NETWORK_CONFIG_OPERATION_DESTROY -> {
                    Log.d(TAG, "Network Down!")
                    clearVirtualNetworkConfig(networkId)
                }
                else -> {
                }
            }
            return 0
        } finally {
            DatabaseUtils.writeLock.unlock()
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
            getNetworkRepository().updateNetworkName(networkId, networkName)
        }
        return virtualNetworkConfig != virtualNetworkConfig2
    }

    /**
     * 关闭 ZeroTier 服务并释放 VPN socket。
     *
     * 供 UDP 通信失败等致命错误路径调用；可在任意线程调用。
     */
    fun shutdown() {
        stopZeroTier()
        val socket = this.vpnSocket
        if (socket != null) {
            try {
                socket.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing VPN socket", e)
            }
            this.vpnSocket = null
        }
        stopSelf(this.mStartID)
    }

    private fun updateTunnelConfig(network: NetworkSummary): Boolean {
        val networkId = network.networkId
        val networkConfig = getNetworkRepository().getNetworkConfig(networkId)
        if (networkConfig == null) {
            Log.e(TAG, "Network config not found: $networkId")
            return false
        }
        val virtualNetworkConfig = getVirtualNetworkConfig(networkId) ?: return false
        val adapter = this.tunTapAdapter!!

        // 重启 TUN TAP
        if (adapter.isRunning()) {
            adapter.interrupt()
            try {
                adapter.join()
            } catch (ignored: InterruptedException) {
            }
        }
        adapter.clearRouteMap()

        // 重启 VPN Socket
        if (this.vpnSocket != null) {
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

        // 配置 VPN
        Log.i(TAG, "Configuring VpnService.Builder")
        val builder = Builder()
        val assignedAddresses = virtualNetworkConfig.assignedAddresses
        Log.i(TAG, "address length: " + assignedAddresses.size)
        val isRouteViaZeroTier = networkConfig.isRouteViaZeroTier

        // 遍历 ZT 网络中当前设备的 IP 地址，组播配置
        for (vpnAddress in assignedAddresses) {
            Log.d(
                TAG,
                "Adding VPN Address: " + vpnAddress.address +
                    " Mac: " + SdkStringUtils.macAddressToString(virtualNetworkConfig.mac)
            )
            val rawAddress = vpnAddress.address.address

            if (!this.disableIPv6 || vpnAddress.address !is Inet6Address) {
                val address = vpnAddress.address
                val port = vpnAddress.port
                val route = InetAddressUtils.addressToRoute(address, port)
                if (route == null) {
                    Log.e(TAG, "NULL route calculated!")
                    continue
                }

                // 计算 VPN 地址相关的组播 MAC 与 ADI
                val multicastGroup: Long
                val multicastAdi: Long
                if (rawAddress.size == 4) {
                    // IPv4
                    multicastGroup = InetAddressUtils.BROADCAST_MAC_ADDRESS
                    multicastAdi = ByteBuffer.wrap(rawAddress).int.toLong()
                } else {
                    // IPv6
                    multicastGroup = ByteBuffer.wrap(
                        byteArrayOf(
                            0, 0, 0x33, 0x33, 0xFF.toByte(),
                            rawAddress[13], rawAddress[14], rawAddress[15]
                        )
                    ).long
                    multicastAdi = 0L
                }

                // 订阅组播并添加至 TUN TAP 路由
                val result = this.node!!.multicastSubscribe(
                    networkId, multicastGroup, multicastAdi
                )
                if (result != ResultCode.RESULT_OK) {
                    Log.e(TAG, "Error joining multicast group")
                } else {
                    Log.d(TAG, "Joined multicast group")
                }
                builder.addAddress(address, port)
                builder.addRoute(route, port)
                adapter.addRouteAndNetwork(Route(route, port), networkId)
            }
        }

        // 遍历网络的路由规则，将网络负责路由的地址路由至 VPN
        try {
            val v4Loopback = InetAddress.getByName("0.0.0.0")
            val v6Loopback = InetAddress.getByName("::")
            val routes = virtualNetworkConfig.routes
            if (routes.isNotEmpty()) {
                for (routeConfig in routes) {
                    val target = routeConfig.target
                    val via = routeConfig.via
                    val targetAddress = target.address
                    val targetPort = target.port
                    val viaAddress = InetAddressUtils.addressToRoute(targetAddress, targetPort)

                    val isIPv6Route =
                        (targetAddress is Inet6Address) || (viaAddress is Inet6Address)
                    val isDisabledV6Route = this.disableIPv6 && isIPv6Route
                    val shouldRouteToZerotier = viaAddress != null && (
                        isRouteViaZeroTier ||
                            (viaAddress != v4Loopback && viaAddress != v6Loopback)
                        )
                    if (!isDisabledV6Route && shouldRouteToZerotier) {
                        builder.addRoute(viaAddress!!, targetPort)
                        val route = Route(viaAddress, targetPort)
                        if (via != null) {
                            route.gateway = via.address
                        }
                        adapter.addRouteAndNetwork(route, networkId)
                    }
                }
            }
            builder.addRoute(InetAddress.getByName("224.0.0.0"), 4)
        } catch (e: Exception) {
            this.eventBus.post(VPNErrorEvent(e.localizedMessage))
            return false
        }

        if (Build.VERSION.SDK_INT >= 29) {
            builder.setMetered(false)
        }
        DnsConfigurator.addDnsServers(
            builder, networkConfig, virtualNetworkConfig, this.disableIPv6
        )

        // 配置 MTU
        var mtu = virtualNetworkConfig.mtu
        Log.i(TAG, "MTU from Network Config: $mtu")
        if (mtu == 0) {
            mtu = 2800
        }
        Log.i(TAG, "MTU Set: $mtu")
        builder.setMtu(mtu)

        builder.setSession(Constants.VPN_SESSION_NAME)

        // 设置部分 APP 不经过 VPN
        if (!isRouteViaZeroTier && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (app in DISALLOWED_APPS) {
                try {
                    builder.addDisallowedApplication(app)
                } catch (e3: Exception) {
                    Log.e(TAG, "Cannot disallow app", e3)
                }
            }
        }

        // 建立 VPN 连接
        this.vpnSocket = builder.establish()
        if (this.vpnSocket == null) {
            this.eventBus.post(
                VPNErrorEvent(getString(R.string.toast_vpn_application_not_prepared))
            )
            return false
        }
        this.input = FileInputStream(this.vpnSocket!!.fileDescriptor)
        this.output = FileOutputStream(this.vpnSocket!!.fileDescriptor)
        adapter.setVpnSocket(this.vpnSocket)
        adapter.setFileStreams(this.input!!, this.output!!)
        adapter.startThreads()

        // 状态栏提示
        this.notificationController.showConnected(network.networkIdStr)
        Log.i(TAG, "ZeroTier One Connected")

        // 旧版本 Android 多播处理
        if (Build.VERSION.SDK_INT < 29) {
            val v4 = this.v4MulticastScanner
            if (v4 != null && !v4.isAlive) {
                v4.start()
            }
            val v6 = this.v6MulticastScanner
            if (!this.disableIPv6 && v6 != null && !v6.isAlive) {
                v6.start()
            }
        }
        return true
    }

    /**
     * 入轨事件。
     */
    @Subscribe
    fun onOrbitMoonEvent(event: OrbitMoonEvent) {
        if (this.node == null) {
            Log.e(TAG, "Can't orbit network if ZeroTier isn't running")
            return
        }
        // 入轨
        for (moonOrbit in event.moonOrbits) {
            Log.i(TAG, "Orbiting moon: " + java.lang.Long.toHexString(moonOrbit.moonWorldId))
            this.orbitNetwork(moonOrbit.moonWorldId, moonOrbit.moonSeed)
        }
    }

    /**
     * 当前网络入轨 Moon。
     *
     * @param moonWorldId Moon 节点地址
     * @param moonSeed Moon 种子节点地址
     */
    fun orbitNetwork(moonWorldId: Long?, moonSeed: Long?) {
        if (this.node == null) {
            Log.e(TAG, "Can't orbit network if ZeroTier isn't running")
            return
        }
        // 入轨
        val result = this.node!!.orbit(moonWorldId!!, moonSeed!!)
        if (result != ResultCode.RESULT_OK) {
            Log.e(
                TAG,
                "Failed to orbit " + java.lang.Long.toHexString(moonWorldId!!) + ": " + result
            )
        }
    }

    inner class ZeroTierBinder : Binder() {

        fun getService(): ZeroTierOneService = this@ZeroTierOneService
    }

    companion object {
        const val MSG_JOIN_NETWORK = 1
        const val MSG_LEAVE_NETWORK = 2
        const val ZT1_NETWORK_ID = "com.zerotier.one.network_id"
        const val ZT1_USE_DEFAULT_ROUTE = "com.zerotier.one.use_default_route"

        private val DISALLOWED_APPS = arrayOf("com.android.vending")
        private const val TAG = "ZT1_Service"
    }
}
