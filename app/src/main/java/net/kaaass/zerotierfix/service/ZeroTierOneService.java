package net.kaaass.zerotierfix.service;

import android.content.Intent;
import android.net.VpnService;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.zerotier.sdk.Event;
import com.zerotier.sdk.EventListener;
import com.zerotier.sdk.Node;
import com.zerotier.sdk.ResultCode;
import com.zerotier.sdk.VirtualNetworkConfig;
import com.zerotier.sdk.VirtualNetworkConfigListener;
import com.zerotier.sdk.VirtualNetworkConfigOperation;

import net.kaaass.zerotierfix.R;
import net.kaaass.zerotierfix.ZerotierFixApplication;
import net.kaaass.zerotierfix.events.AfterJoinNetworkEvent;
import net.kaaass.zerotierfix.events.IsServiceRunningReplyEvent;
import net.kaaass.zerotierfix.events.IsServiceRunningRequestEvent;
import net.kaaass.zerotierfix.events.ManualDisconnectEvent;
import net.kaaass.zerotierfix.events.NetworkConfigChangedByUserEvent;
import net.kaaass.zerotierfix.events.NetworkListReplyEvent;
import net.kaaass.zerotierfix.events.NetworkListRequestEvent;
import net.kaaass.zerotierfix.events.NetworkReconfigureEvent;
import net.kaaass.zerotierfix.events.NodeDestroyedEvent;
import net.kaaass.zerotierfix.events.NodeIDEvent;
import net.kaaass.zerotierfix.events.NodeStatusEvent;
import net.kaaass.zerotierfix.events.NodeStatusRequestEvent;
import net.kaaass.zerotierfix.events.OrbitMoonEvent;
import net.kaaass.zerotierfix.events.PeerInfoReplyEvent;
import net.kaaass.zerotierfix.events.PeerInfoRequestEvent;
import net.kaaass.zerotierfix.events.StopEvent;
import net.kaaass.zerotierfix.events.VPNErrorEvent;
import net.kaaass.zerotierfix.events.VirtualNetworkConfigChangedEvent;
import net.kaaass.zerotierfix.events.VirtualNetworkConfigReplyEvent;
import net.kaaass.zerotierfix.events.VirtualNetworkConfigRequestEvent;
import net.kaaass.zerotierfix.data.repository.NetworkRepository;
import net.kaaass.zerotierfix.model.dto.MoonOrbitDto;
import net.kaaass.zerotierfix.model.dto.NetworkConfigDto;
import net.kaaass.zerotierfix.model.dto.NetworkSummary;
import net.kaaass.zerotierfix.model.dto.VirtualNetworkConfigDto;
import net.kaaass.zerotierfix.model.enums.NetworkStatus;
import net.kaaass.zerotierfix.vpn.config.DnsConfigurator;
import net.kaaass.zerotierfix.vpn.config.VirtualNetworkConfigMapper;
import net.kaaass.zerotierfix.vpn.node.NodeInfoMapper;
import net.kaaass.zerotierfix.vpn.node.UdpCom;
import net.kaaass.zerotierfix.vpn.route.Route;
import net.kaaass.zerotierfix.vpn.store.DataStore;
import net.kaaass.zerotierfix.vpn.tun.TunTapAdapter;
import net.kaaass.zerotierfix.util.Constants;
import net.kaaass.zerotierfix.data.database.DatabaseUtils;
import net.kaaass.zerotierfix.util.InetAddressUtils;
import net.kaaass.zerotierfix.util.NetworkInfoUtils;
import net.kaaass.zerotierfix.util.StringUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.DatagramSocket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// TODO: clear up
public class ZeroTierOneService extends VpnService implements Runnable, EventListener, VirtualNetworkConfigListener {
    public static final int MSG_JOIN_NETWORK = 1;
    public static final int MSG_LEAVE_NETWORK = 2;
    public static final String ZT1_NETWORK_ID = "com.zerotier.one.network_id";
    public static final String ZT1_USE_DEFAULT_ROUTE = "com.zerotier.one.use_default_route";
    private static final String[] DISALLOWED_APPS = {"com.android.vending"};
    private static final String TAG = "ZT1_Service";
    private final IBinder mBinder = new ZeroTierBinder();
    private final DataStore dataStore = new DataStore(this);
    private final EventBus eventBus = EventBus.getDefault();
    private final Map<Long, VirtualNetworkConfig> virtualNetworkConfigMap = new HashMap();
    FileInputStream in;
    FileOutputStream out;
    DatagramSocket svrSocket;
    ParcelFileDescriptor vpnSocket;
    private int bindCount = 0;
    private boolean disableIPv6 = false;
    private int mStartID = -1;
    private long networkId = 0;
    private long nextBackgroundTaskDeadline = 0;
    private final VpnNotificationController notificationController =
            new VpnNotificationController(this);
    private Node node;
    private TunTapAdapter tunTapAdapter;
    private UdpCom udpCom;
    private Thread udpThread;
    private Thread v4MulticastScanner = new Thread() {
        /* class com.zerotier.one.service.ZeroTierOneService.AnonymousClass1 */
        List<String> subscriptions = new ArrayList<>();

        @Override
        public void run() {
            Log.d(ZeroTierOneService.TAG, "IPv4 Multicast Scanner Thread Started.");
            while (!isInterrupted()) {
                try {
                    List<String> groups = NetworkInfoUtils.listMulticastGroupOnInterface("tun0", false);

                    ArrayList<String> arrayList2 = new ArrayList<>(this.subscriptions);
                    ArrayList<String> arrayList3 = new ArrayList<>(groups);
                    arrayList3.removeAll(arrayList2);
                    for (String str : arrayList3) {
                        try {
                            byte[] hexStringToByteArray = StringUtils.hexStringToBytes(str);
                            for (int i = 0; i < hexStringToByteArray.length / 2; i++) {
                                byte b = hexStringToByteArray[i];
                                hexStringToByteArray[i] = hexStringToByteArray[(hexStringToByteArray.length - i) - 1];
                                hexStringToByteArray[(hexStringToByteArray.length - i) - 1] = b;
                            }
                            ResultCode multicastSubscribe = ZeroTierOneService.this.node.multicastSubscribe(ZeroTierOneService.this.networkId, TunTapAdapter.multicastAddressToMAC(InetAddress.getByAddress(hexStringToByteArray)));
                            if (multicastSubscribe != ResultCode.RESULT_OK) {
                                Log.e(ZeroTierOneService.TAG, "Error when calling multicastSubscribe: " + multicastSubscribe);
                            }
                        } catch (Exception e) {
                            Log.e(ZeroTierOneService.TAG, e.toString(), e);
                        }
                    }
                    arrayList2.removeAll(new ArrayList<>(groups));
                    for (String str2 : arrayList2) {
                        try {
                            byte[] hexStringToByteArray2 = StringUtils.hexStringToBytes(str2);
                            for (int i2 = 0; i2 < hexStringToByteArray2.length / 2; i2++) {
                                byte b2 = hexStringToByteArray2[i2];
                                hexStringToByteArray2[i2] = hexStringToByteArray2[(hexStringToByteArray2.length - i2) - 1];
                                hexStringToByteArray2[(hexStringToByteArray2.length - i2) - 1] = b2;
                            }
                            ResultCode multicastUnsubscribe = ZeroTierOneService.this.node.multicastUnsubscribe(ZeroTierOneService.this.networkId, TunTapAdapter.multicastAddressToMAC(InetAddress.getByAddress(hexStringToByteArray2)));
                            if (multicastUnsubscribe != ResultCode.RESULT_OK) {
                                Log.e(ZeroTierOneService.TAG, "Error when calling multicastUnsubscribe: " + multicastUnsubscribe);
                            }
                        } catch (Exception e) {
                            Log.e(ZeroTierOneService.TAG, e.toString(), e);
                        }
                    }
                    this.subscriptions = groups;
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.d(ZeroTierOneService.TAG, "V4 Multicast Scanner Thread Interrupted", e);
                    break;
                }
            }
            Log.d(ZeroTierOneService.TAG, "IPv4 Multicast Scanner Thread Ended.");
        }
    };
    private Thread v6MulticastScanner = new Thread() {
        /* class com.zerotier.one.service.ZeroTierOneService.AnonymousClass2 */
        List<String> subscriptions = new ArrayList<>();

        @Override
        public void run() {
            Log.d(ZeroTierOneService.TAG, "IPv6 Multicast Scanner Thread Started.");
            while (!isInterrupted()) {
                try {
                    List<String> groups = NetworkInfoUtils.listMulticastGroupOnInterface("tun0", true);

                    ArrayList<String> arrayList2 = new ArrayList<>(this.subscriptions);
                    ArrayList<String> arrayList3 = new ArrayList<>(groups);
                    arrayList3.removeAll(arrayList2);
                    for (String str : arrayList3) {
                        try {
                            ResultCode multicastSubscribe = ZeroTierOneService.this.node.multicastSubscribe(ZeroTierOneService.this.networkId, TunTapAdapter.multicastAddressToMAC(InetAddress.getByAddress(StringUtils.hexStringToBytes(str))));
                            if (multicastSubscribe != ResultCode.RESULT_OK) {
                                Log.e(ZeroTierOneService.TAG, "Error when calling multicastSubscribe: " + multicastSubscribe);
                            }
                        } catch (Exception e) {
                            Log.e(ZeroTierOneService.TAG, e.toString(), e);
                        }
                    }
                    arrayList2.removeAll(new ArrayList<>(groups));
                    for (String str2 : arrayList2) {
                        try {
                            ResultCode multicastUnsubscribe = ZeroTierOneService.this.node.multicastUnsubscribe(ZeroTierOneService.this.networkId, TunTapAdapter.multicastAddressToMAC(InetAddress.getByAddress(StringUtils.hexStringToBytes(str2))));
                            if (multicastUnsubscribe != ResultCode.RESULT_OK) {
                                Log.e(ZeroTierOneService.TAG, "Error when calling multicastUnsubscribe: " + multicastUnsubscribe);
                            }
                        } catch (Exception e) {
                            Log.e(ZeroTierOneService.TAG, e.toString(), e);
                        }
                    }
                    this.subscriptions = groups;
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.d(ZeroTierOneService.TAG, "V6 Multicast Scanner Thread Interrupted", e);
                    break;
                }
            }
            Log.d(ZeroTierOneService.TAG, "IPv6 Multicast Scanner Thread Ended.");
        }
    };
    private Thread vpnThread;

    public VirtualNetworkConfig getVirtualNetworkConfig(long j) {
        VirtualNetworkConfig virtualNetworkConfig;
        synchronized (this.virtualNetworkConfigMap) {
            virtualNetworkConfig = this.virtualNetworkConfigMap.get(Long.valueOf(j));
        }
        return virtualNetworkConfig;
    }

    public VirtualNetworkConfig setVirtualNetworkConfig(long j, VirtualNetworkConfig virtualNetworkConfig) {
        VirtualNetworkConfig put;
        synchronized (this.virtualNetworkConfigMap) {
            put = this.virtualNetworkConfigMap.put(Long.valueOf(j), virtualNetworkConfig);
        }
        return put;
    }

    public VirtualNetworkConfig clearVirtualNetworkConfig(long j) {
        VirtualNetworkConfig remove;
        synchronized (this.virtualNetworkConfigMap) {
            remove = this.virtualNetworkConfigMap.remove(Long.valueOf(j));
        }
        return remove;
    }

    private void logBindCount() {
        Log.i(TAG, "Bind Count: " + this.bindCount);
    }

    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Bound by: " + getPackageManager().getNameForUid(Binder.getCallingUid()));
        this.bindCount++;
        logBindCount();
        return this.mBinder;
    }

    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Unbound by: " + getPackageManager().getNameForUid(Binder.getCallingUid()));
        this.bindCount--;
        logBindCount();
        return false;
    }

    /* access modifiers changed from: protected */
    public void setNextBackgroundTaskDeadline(long j) {
        synchronized (this) {
            this.nextBackgroundTaskDeadline = j;
        }
    }

    /**
     * 获取网络数据仓库。
     *
     * @return 网络仓库，非空
     */
    private NetworkRepository getNetworkRepository() {
        return ((ZerotierFixApplication) getApplication())
                .getAppContainer()
                .getNetworkRepository();
    }

    /**
     * 启动 ZT 服务，连接至给定网络或最近连接的网络
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        long networkId;
        Log.d(TAG, "onStartCommand");
        if (startId == 3) {
            Log.i(TAG, "Authorizing VPN");
            return START_NOT_STICKY;
        } else if (intent == null) {
            Log.e(TAG, "NULL intent.  Cannot start");
            return START_NOT_STICKY;
        }
        this.mStartID = startId;

        // 注册事件总线监听器
        if (!this.eventBus.isRegistered(this)) {
            this.eventBus.register(this);
        }

        // 确定待启动的网络 ID
        if (intent.hasExtra(ZT1_NETWORK_ID)) {
            // Intent 中指定了目标网络，直接使用此 ID
            networkId = intent.getLongExtra(ZT1_NETWORK_ID, 0);
        } else {
            // 默认启用最近一次启动的网络
            Long lastActivatedNetworkId = getNetworkRepository().getLastActivatedNetworkId();
            if (lastActivatedNetworkId == null) {
                Log.e(TAG, "Couldn't find last activated connection");
                return START_NOT_STICKY;
            }
            networkId = lastActivatedNetworkId;
            Log.i(TAG, "Got Always On request for ZeroTier");
        }
        if (networkId == 0) {
            Log.e(TAG, "Network ID not provided to service");
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        this.networkId = networkId;

        // 检查当前的网络环境
        var preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean useCellularData = preferences.getBoolean(Constants.PREF_NETWORK_USE_CELLULAR_DATA, false);
        this.disableIPv6 = preferences.getBoolean(Constants.PREF_NETWORK_DISABLE_IPV6, false);
        var currentNetworkInfo = NetworkInfoUtils.getNetworkInfoCurrentConnection(this);

        if (currentNetworkInfo == NetworkInfoUtils.CurrentConnection.CONNECTION_NONE) {
            // 未连接网络
            Toast.makeText(this, R.string.toast_no_network, Toast.LENGTH_SHORT).show();
            stopSelf(this.mStartID);
            return START_NOT_STICKY;
        } else if (currentNetworkInfo == NetworkInfoUtils.CurrentConnection.CONNECTION_MOBILE &&
                !useCellularData) {
            // 使用移动网络，但未在设置中允许移动网络访问
            Toast.makeText(this, R.string.toast_mobile_data, Toast.LENGTH_LONG).show();
            stopSelf(this.mStartID);
            return START_NOT_STICKY;
        }

        // 启动 ZT 服务
        synchronized (this) {
            try {
                // 创建本地 ZT 服务 Socket，监听本地端口
                if (this.svrSocket == null) {
                    this.svrSocket = new DatagramSocket(null);
                    this.svrSocket.setReuseAddress(true);
                    this.svrSocket.setSoTimeout(1000);
                    this.svrSocket.bind(new InetSocketAddress(9994));
                }
                if (!protect(this.svrSocket)) {
                    Log.e(TAG, "Error protecting UDP socket from feedback loop.");
                }

                // 创建本地节点
                if (this.node == null) {
                    this.udpCom = new UdpCom(this, this.svrSocket);
                    this.tunTapAdapter = new TunTapAdapter(this, networkId);

                    // 创建节点对象并初始化
                    var dataStore = this.dataStore;
                    this.node = new Node(System.currentTimeMillis());
                    var result = this.node.init(dataStore, dataStore, this.udpCom, this, this.tunTapAdapter, this, null);

                    if (result == ResultCode.RESULT_OK) {
                        Log.d(TAG, "ZeroTierOne Node Initialized");
                    } else {
                        Log.e(TAG, "Error starting ZT1 Node: " + result);
                        return START_NOT_STICKY;
                    }
                    this.onNodeStatusRequest(null);

                    // 持久化当前节点信息
                    long address = this.node.address();
                    ((ZerotierFixApplication) getApplication())
                            .getAppContainer()
                            .getAppNodeRepository()
                            .saveNodeId(address);

                    this.eventBus.post(new NodeIDEvent(address));
                    this.udpCom.setNode(this.node);
                    this.tunTapAdapter.setNode(this.node);

                    // 启动 UDP 消息处理线程
                    var thread = new Thread(this.udpCom, "UDP Communication Thread");
                    this.udpThread = thread;
                    thread.start();
                }

                // 创建并启动 VPN 服务线程
                if (this.vpnThread == null) {
                    var thread = new Thread(this, "ZeroTier Service Thread");
                    this.vpnThread = thread;
                    thread.start();
                }

                // 启动 UDP 消息处理线程
                if (!this.udpThread.isAlive()) {
                    this.udpThread.start();
                }
            } catch (Exception e) {
                Log.e(TAG, e.toString(), e);
                return START_NOT_STICKY;
            }
        }
        joinNetwork(networkId);
        return START_STICKY;
    }

    public void stopZeroTier() {
        if (this.svrSocket != null) {
            this.svrSocket.close();
            this.svrSocket = null;
        }
        if (this.udpThread != null && this.udpThread.isAlive()) {
            this.udpThread.interrupt();
            try {
                this.udpThread.join();
            } catch (InterruptedException ignored) {
            }
            this.udpThread = null;
        }
        if (this.tunTapAdapter != null && this.tunTapAdapter.isRunning()) {
            this.tunTapAdapter.interrupt();
            try {
                this.tunTapAdapter.join();
            } catch (InterruptedException ignored) {
            }
            this.tunTapAdapter = null;
        }
        if (this.vpnThread != null && this.vpnThread.isAlive()) {
            this.vpnThread.interrupt();
            try {
                this.vpnThread.join();
            } catch (InterruptedException ignored) {
            }
            this.vpnThread = null;
        }
        if (this.v4MulticastScanner != null) {
            this.v4MulticastScanner.interrupt();
            try {
                this.v4MulticastScanner.join();
            } catch (InterruptedException ignored) {
            }
            this.v4MulticastScanner = null;
        }
        if (this.v6MulticastScanner != null) {
            this.v6MulticastScanner.interrupt();
            try {
                this.v6MulticastScanner.join();
            } catch (InterruptedException ignored) {
            }
            this.v6MulticastScanner = null;
        }
        if (this.vpnSocket != null) {
            try {
                this.vpnSocket.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing VPN socket: " + e, e);
            }
            this.vpnSocket = null;
        }
        if (this.node != null) {
            this.eventBus.post(new NodeDestroyedEvent());
            this.node.close();
            this.node = null;
        }
        if (this.eventBus.isRegistered(this)) {
            this.eventBus.unregister(this);
        }
        this.notificationController.cancel();
        if (!stopSelfResult(this.mStartID)) {
            Log.e(TAG, "stopSelfResult() failed!");
        }
    }

    public void onDestroy() {
        try {
            stopZeroTier();
            if (this.vpnSocket != null) {
                try {
                    this.vpnSocket.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing VPN socket: " + e, e);
                }
                this.vpnSocket = null;
            }
            stopSelf(this.mStartID);
            if (this.eventBus.isRegistered(this)) {
                this.eventBus.unregister(this);
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
        } finally {
            super.onDestroy();
        }
    }

    public void onRevoke() {
        stopZeroTier();
        if (this.vpnSocket != null) {
            try {
                this.vpnSocket.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing VPN socket: " + e, e);
            }
            this.vpnSocket = null;
        }
        stopSelf(this.mStartID);
        if (this.eventBus.isRegistered(this)) {
            this.eventBus.unregister(this);
        }
        super.onRevoke();
    }

    public void run() {
        Log.d(TAG, "ZeroTierOne Service Started");
        Log.d(TAG, "This Node Address: " + com.zerotier.sdk.util.StringUtils.addressToString(this.node.address()));
        while (!Thread.interrupted()) {
            try {
                // 在后台任务截止期前循环进行后台任务
                var taskDeadline = this.nextBackgroundTaskDeadline;
                long currentTime = System.currentTimeMillis();
                int cmp = Long.compare(taskDeadline, currentTime);
                if (cmp <= 0) {
                    long[] newDeadline = {0};
                    var taskResult = this.node.processBackgroundTasks(currentTime, newDeadline);
                    synchronized (this) {
                        this.nextBackgroundTaskDeadline = newDeadline[0];
                    }
                    if (taskResult != ResultCode.RESULT_OK) {
                        Log.e(TAG, "Error on processBackgroundTasks: " + taskResult.toString());
                        shutdown();
                    }
                }
                Thread.sleep(cmp > 0 ? taskDeadline - currentTime : 100);
            } catch (InterruptedException ignored) {
                break;
            } catch (Exception e) {
                Log.e(TAG, e.toString(), e);
            }
        }
        Log.d(TAG, "ZeroTierOne Service Ended");
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onStopEvent(StopEvent stopEvent) {
        stopZeroTier();
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onManualDisconnect(ManualDisconnectEvent manualDisconnectEvent) {
        stopZeroTier();
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onIsServiceRunningRequest(IsServiceRunningRequestEvent event) {
        this.eventBus.post(new IsServiceRunningReplyEvent(true));
    }

    /**
     * 加入 ZT 网络
     */
    public void joinNetwork(long networkId) {
        if (this.node == null) {
            Log.e(TAG, "Can't join network if ZeroTier isn't running");
            return;
        }
        // 连接到新网络
        var result = this.node.join(networkId);
        if (result != ResultCode.RESULT_OK) {
            Log.e(TAG, "Failed to join network " + Long.toHexString(networkId) + ": " + result);
            return;
        }
        // 连接后事件
        this.eventBus.post(new AfterJoinNetworkEvent());
    }

    /**
     * 离开 ZT 网络
     */
    public void leaveNetwork(long networkId) {
        if (this.node == null) {
            Log.e(TAG, "Can't leave network if ZeroTier isn't running");
            return;
        }
        var result = this.node.leave(networkId);
        if (result != ResultCode.RESULT_OK) {
            Log.e(TAG, "Failed to leave network " + Long.toHexString(networkId) + ": " + result);
            return;
        }
        var networkConfigs = this.node.networkConfigs();
        if (networkConfigs != null && networkConfigs.length != 0) {
            return;
        }
        stopZeroTier();
        if (this.vpnSocket != null) {
            try {
                this.vpnSocket.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing VPN socket", e);
            }
            this.vpnSocket = null;
        }
        stopSelf(this.mStartID);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onNetworkListRequest(NetworkListRequestEvent requestNetworkListEvent) {
        VirtualNetworkConfig[] networks;
        Node node2 = this.node;
        if (node2 != null && (networks = node2.networkConfigs()) != null && networks.length > 0) {
            this.eventBus.post(new NetworkListReplyEvent(
                    VirtualNetworkConfigMapper.toDtoList(networks)));
        }
    }

    /**
     * 请求节点状态事件回调
     *
     * @param event 事件
     */
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onNodeStatusRequest(NodeStatusRequestEvent event) {
        // 返回节点状态
        if (this.node != null) {
            this.eventBus.post(new NodeStatusEvent(
                    NodeInfoMapper.toDto(this.node.status(), this.node.getVersion())));
        }
    }

    /**
     * 请求 Peer 信息事件回调
     */
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onRequestPeerInfo(PeerInfoRequestEvent event) {
        if (this.node == null) {
            this.eventBus.post(new PeerInfoReplyEvent(null));
            return;
        }
        this.eventBus.post(new PeerInfoReplyEvent(NodeInfoMapper.toDto(this.node.peers())));
    }

    /**
     * 请求网络配置事件回调
     */
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onVirtualNetworkConfigRequest(VirtualNetworkConfigRequestEvent event) {
        if (this.node == null) {
            this.eventBus.post(new VirtualNetworkConfigReplyEvent(null));
            return;
        }
        var config = this.node.networkConfig(event.getNetworkId());
        this.eventBus.post(new VirtualNetworkConfigReplyEvent(
                config == null ? null : VirtualNetworkConfigMapper.toDto(config)));
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onNetworkReconfigure(NetworkReconfigureEvent event) {
        boolean isChanged = event.isChanged();
        var network = event.getNetwork();
        VirtualNetworkConfigDto networkConfig = event.getVirtualNetworkConfig();
        boolean configUpdated = isChanged && network != null && updateTunnelConfig(network);
        boolean networkIsOk = networkConfig.getStatus() == NetworkStatus.OK;

        if (configUpdated || !networkIsOk) {
            this.eventBus.post(new VirtualNetworkConfigChangedEvent(networkConfig));
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onNetworkConfigChangedByUser(NetworkConfigChangedByUserEvent event) {
        if (event.getNetworkId() != this.networkId) {
            return;
        }
        NetworkSummary network = getNetworkRepository().getNetworkSummary(event.getNetworkId());
        if (network == null) {
            Log.e(TAG, "Network not found: " + event.getNetworkId());
            return;
        }
        updateTunnelConfig(network);
    }

    /**
     * Zerotier 事件回调
     *
     * @param event {@link Event} enum
     */
    @Override
    public void onEvent(Event event) {
        Log.d(TAG, "Event: " + event.toString());
        // 更新节点状态
        if (this.node.isInited()) {
            this.eventBus.post(new NodeStatusEvent(
                    NodeInfoMapper.toDto(this.node.status(), this.node.getVersion())));
        }
    }

    @Override // com.zerotier.sdk.EventListener
    public void onTrace(String str) {
        Log.d(TAG, "Trace: " + str);
    }

    /**
     * 当 ZT 网络配置发生更新
     */
    @Override
    public int onNetworkConfigurationUpdated(long networkId, VirtualNetworkConfigOperation op, VirtualNetworkConfig config) {
        Log.i(TAG, "Virtual Network Config Operation: " + op);
        DatabaseUtils.writeLock.lock();
        try {
            // 查找网络 ID 对应的配置
            var network = getNetworkRepository().getNetworkSummary(networkId);
            if (network == null) {
                throw new IllegalStateException("Database is inconsistent");
            }
            // 根据当前网络状态确定更改配置的行为
            switch (op) {
                case VIRTUAL_NETWORK_CONFIG_OPERATION_UP:
                    Log.d(TAG, "Network Type: " + config.getType() + " Network Status: " + config.getStatus() + " Network Name: " + config.getName() + " ");
                    // 将网络配置的更新交给第一次 Update
                    break;
                case VIRTUAL_NETWORK_CONFIG_OPERATION_CONFIG_UPDATE:
                    Log.i(TAG, "Network Config Update!");
                    boolean isChanged = setVirtualNetworkConfigAndUpdateDatabase(network, config);
                    this.eventBus.post(new NetworkReconfigureEvent(
                            isChanged,
                            network,
                            VirtualNetworkConfigMapper.toDto(config)
                    ));
                    break;
                case VIRTUAL_NETWORK_CONFIG_OPERATION_DOWN:
                case VIRTUAL_NETWORK_CONFIG_OPERATION_DESTROY:
                    Log.d(TAG, "Network Down!");
                    clearVirtualNetworkConfig(networkId);
                    break;
            }
            return 0;
        } finally {
            DatabaseUtils.writeLock.unlock();
        }
    }

    private boolean setVirtualNetworkConfigAndUpdateDatabase(NetworkSummary network, VirtualNetworkConfig virtualNetworkConfig) {
        if ((DatabaseUtils.writeLock instanceof ReentrantReadWriteLock.WriteLock) && !((ReentrantReadWriteLock.WriteLock) DatabaseUtils.writeLock).isHeldByCurrentThread()) {
            throw new IllegalStateException("DatabaseUtils.writeLock not held");
        }
        long networkId = network.getNetworkId();
        VirtualNetworkConfig virtualNetworkConfig2 = getVirtualNetworkConfig(networkId);
        setVirtualNetworkConfig(networkId, virtualNetworkConfig);
        var networkName = virtualNetworkConfig.getName();
        if (networkName != null && !networkName.isEmpty()) {
            getNetworkRepository().updateNetworkName(networkId, networkName);
        }
        return !virtualNetworkConfig.equals(virtualNetworkConfig2);
    }

    /**
     * 关闭 ZeroTier 服务并释放 VPN socket。
     *
     * <p>供 UDP 通信失败等致命错误路径调用；可在任意线程调用。</p>
     */
    public void shutdown() {
        stopZeroTier();
        if (this.vpnSocket != null) {
            try {
                this.vpnSocket.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing VPN socket", e);
            }
            this.vpnSocket = null;
        }
        stopSelf(this.mStartID);
    }

    private boolean updateTunnelConfig(NetworkSummary network) {
        long networkId = network.getNetworkId();
        NetworkConfigDto networkConfig = getNetworkRepository().getNetworkConfig(networkId);
        if (networkConfig == null) {
            Log.e(TAG, "Network config not found: " + networkId);
            return false;
        }
        var virtualNetworkConfig = getVirtualNetworkConfig(networkId);
        if (virtualNetworkConfig == null) {
            return false;
        }

        // 重启 TUN TAP
        if (this.tunTapAdapter.isRunning()) {
            this.tunTapAdapter.interrupt();
            try {
                this.tunTapAdapter.join();
            } catch (InterruptedException ignored) {
            }
        }
        this.tunTapAdapter.clearRouteMap();

        // 重启 VPN Socket
        if (this.vpnSocket != null) {
            try {
                this.vpnSocket.close();
                this.in.close();
                this.out.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing VPN socket: " + e, e);
            }
            this.vpnSocket = null;
            this.in = null;
            this.out = null;
        }

        // 配置 VPN
        Log.i(TAG, "Configuring VpnService.Builder");
        var builder = new VpnService.Builder();
        var assignedAddresses = virtualNetworkConfig.getAssignedAddresses();
        Log.i(TAG, "address length: " + assignedAddresses.length);
        boolean isRouteViaZeroTier = networkConfig.isRouteViaZeroTier();

        // 遍历 ZT 网络中当前设备的 IP 地址，组播配置
        for (var vpnAddress : assignedAddresses) {
            Log.d(TAG, "Adding VPN Address: " + vpnAddress.getAddress()
                    + " Mac: " + com.zerotier.sdk.util.StringUtils.macAddressToString(virtualNetworkConfig.getMac()));
            byte[] rawAddress = vpnAddress.getAddress().getAddress();

            if (!this.disableIPv6 || !(vpnAddress.getAddress() instanceof Inet6Address)) {
                var address = vpnAddress.getAddress();
                var port = vpnAddress.getPort();
                var route = InetAddressUtils.addressToRoute(address, port);
                if (route == null) {
                    Log.e(TAG, "NULL route calculated!");
                    continue;
                }

                // 计算 VPN 地址相关的组播 MAC 与 ADI
                long multicastGroup;
                long multicastAdi;
                if (rawAddress.length == 4) {
                    // IPv4
                    multicastGroup = InetAddressUtils.BROADCAST_MAC_ADDRESS;
                    multicastAdi = ByteBuffer.wrap(rawAddress).getInt();
                } else {
                    // IPv6
                    multicastGroup = ByteBuffer.wrap(new byte[]{
                                    0, 0, 0x33, 0x33, (byte) 0xFF, rawAddress[13], rawAddress[14], rawAddress[15]})
                            .getLong();
                    multicastAdi = 0;
                }

                // 订阅组播并添加至 TUN TAP 路由
                var result = this.node.multicastSubscribe(networkId, multicastGroup, multicastAdi);
                if (result != ResultCode.RESULT_OK) {
                    Log.e(TAG, "Error joining multicast group");
                } else {
                    Log.d(TAG, "Joined multicast group");
                }
                builder.addAddress(address, port);
                builder.addRoute(route, port);
                this.tunTapAdapter.addRouteAndNetwork(new Route(route, port), networkId);
            }
        }

        // 遍历网络的路由规则，将网络负责路由的地址路由至 VPN
        try {
            var v4Loopback = InetAddress.getByName("0.0.0.0");
            var v6Loopback = InetAddress.getByName("::");
            if (virtualNetworkConfig.getRoutes().length > 0) {
                for (var routeConfig : virtualNetworkConfig.getRoutes()) {
                    var target = routeConfig.getTarget();
                    var via = routeConfig.getVia();
                    var targetAddress = target.getAddress();
                    var targetPort = target.getPort();
                    var viaAddress = InetAddressUtils.addressToRoute(targetAddress, targetPort);

                    boolean isIPv6Route = (targetAddress instanceof Inet6Address) || (viaAddress instanceof Inet6Address);
                    boolean isDisabledV6Route = this.disableIPv6 && isIPv6Route;
                    boolean shouldRouteToZerotier = viaAddress != null && (
                            isRouteViaZeroTier
                                    || (!viaAddress.equals(v4Loopback) && !viaAddress.equals(v6Loopback))
                    );
                    if (!isDisabledV6Route && shouldRouteToZerotier) {
                        builder.addRoute(viaAddress, targetPort);
                        Route route = new Route(viaAddress, targetPort);
                        if (via != null) {
                            route.setGateway(via.getAddress());
                        }
                        this.tunTapAdapter.addRouteAndNetwork(route, networkId);
                    }
                }
            }
            builder.addRoute(InetAddress.getByName("224.0.0.0"), 4);
        } catch (Exception e) {
            this.eventBus.post(new VPNErrorEvent(e.getLocalizedMessage()));
            return false;
        }

        if (Build.VERSION.SDK_INT >= 29) {
            builder.setMetered(false);
        }
        DnsConfigurator.addDnsServers(builder, networkConfig, virtualNetworkConfig, this.disableIPv6);

        // 配置 MTU
        int mtu = virtualNetworkConfig.getMtu();
        Log.i(TAG, "MTU from Network Config: " + mtu);
        if (mtu == 0) {
            mtu = 2800;
        }
        Log.i(TAG, "MTU Set: " + mtu);
        builder.setMtu(mtu);

        builder.setSession(Constants.VPN_SESSION_NAME);

        // 设置部分 APP 不经过 VPN
        if (!isRouteViaZeroTier && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (var app : DISALLOWED_APPS) {
                try {
                    builder.addDisallowedApplication(app);
                } catch (Exception e3) {
                    Log.e(TAG, "Cannot disallow app", e3);
                }
            }
        }

        // 建立 VPN 连接
        this.vpnSocket = builder.establish();
        if (this.vpnSocket == null) {
            this.eventBus.post(new VPNErrorEvent(getString(R.string.toast_vpn_application_not_prepared)));
            return false;
        }
        this.in = new FileInputStream(this.vpnSocket.getFileDescriptor());
        this.out = new FileOutputStream(this.vpnSocket.getFileDescriptor());
        this.tunTapAdapter.setVpnSocket(this.vpnSocket);
        this.tunTapAdapter.setFileStreams(this.in, this.out);
        this.tunTapAdapter.startThreads();

        // 状态栏提示
        this.notificationController.showConnected(network.getNetworkIdStr());
        Log.i(TAG, "ZeroTier One Connected");

        // 旧版本 Android 多播处理
        if (Build.VERSION.SDK_INT < 29) {
            if (this.v4MulticastScanner != null && !this.v4MulticastScanner.isAlive()) {
                this.v4MulticastScanner.start();
            }
            if (!this.disableIPv6 && this.v6MulticastScanner != null && !this.v6MulticastScanner.isAlive()) {
                this.v6MulticastScanner.start();
            }
        }
        return true;
    }

    /**
     * 入轨事件
     */
    @Subscribe
    public void onOrbitMoonEvent(OrbitMoonEvent event) {
        if (this.node == null) {
            Log.e(TAG, "Can't orbit network if ZeroTier isn't running");
            return;
        }
        // 入轨
        for (MoonOrbitDto moonOrbit : event.getMoonOrbits()) {
            Log.i(TAG, "Orbiting moon: " + Long.toHexString(moonOrbit.getMoonWorldId()));
            this.orbitNetwork(moonOrbit.getMoonWorldId(), moonOrbit.getMoonSeed());
        }
    }

    /**
     * 当前网络入轨 Moon
     *
     * @param moonWorldId Moon 节点地址
     * @param moonSeed    Moon 种子节点地址
     */
    public void orbitNetwork(Long moonWorldId, Long moonSeed) {
        if (this.node == null) {
            Log.e(TAG, "Can't orbit network if ZeroTier isn't running");
            return;
        }
        // 入轨
        ResultCode result = this.node.orbit(moonWorldId, moonSeed);
        if (result != ResultCode.RESULT_OK) {
            Log.e(TAG, "Failed to orbit " + Long.toHexString(moonWorldId) + ": " + result);
        }
    }

    public class ZeroTierBinder extends Binder {
        public ZeroTierBinder() {
        }

        public ZeroTierOneService getService() {
            return ZeroTierOneService.this;
        }
    }
}
