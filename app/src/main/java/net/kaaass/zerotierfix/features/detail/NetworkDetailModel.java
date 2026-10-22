package net.kaaass.zerotierfix.features.detail;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import net.kaaass.zerotierfix.ZerotierFixApplication;
import net.kaaass.zerotierfix.data.repository.NetworkRepository;
import net.kaaass.zerotierfix.events.NetworkConfigChangedByUserEvent;
import net.kaaass.zerotierfix.events.VirtualNetworkConfigChangedEvent;
import net.kaaass.zerotierfix.events.VirtualNetworkConfigReplyEvent;
import net.kaaass.zerotierfix.events.VirtualNetworkConfigRequestEvent;
import net.kaaass.zerotierfix.model.dto.NetworkConfigDto;
import net.kaaass.zerotierfix.model.dto.NetworkSummary;
import net.kaaass.zerotierfix.model.dto.VirtualNetworkConfigDto;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * 网络详情的 ViewModel
 */
public class NetworkDetailModel extends AndroidViewModel {
    private static final String TAG = "NetworkDetailViewModel";
    private final MutableLiveData<NetworkSummary> network = new MutableLiveData<>();
    private final MutableLiveData<NetworkConfigDto> networkConfig = new MutableLiveData<>();
    private final MutableLiveData<VirtualNetworkConfigDto> virtualNetworkConfig = new MutableLiveData<>();
    private final EventBus eventBus = EventBus.getDefault();
    private long networkId = -1;

    public NetworkDetailModel(@NonNull Application application) {
        super(application);
        // 注册 EventBus
        this.eventBus.register(this);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // 取消注册 EventBus
        this.eventBus.unregister(this);
    }

    /**
     * 获得指定网络的全部网络信息
     */
    public void doRetrieveDetail(long networkId) {
        this.networkId = networkId;
        doRetrieveNetworkAndConfig();
        doRetrieveVirtualNetworkConfig();
    }

    /**
     * 获得持久化的网络配置
     */
    private void doRetrieveNetworkAndConfig() {
        // 在 DB 中查询网络对象
        NetworkRepository repository = getNetworkRepository();
        NetworkSummary network = repository.getNetworkSummary(this.networkId);
        if (network == null) {
            Log.e(TAG, "Network not found!");
            return;
        }
        NetworkConfigDto networkConfig = repository.getNetworkConfig(this.networkId);
        if (networkConfig == null) {
            Log.e(TAG, "Network config not found!");
            return;
        }

        // 更新 LiveData
        this.network.setValue(network);
        this.networkConfig.setValue(networkConfig);
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
     * 向 ZT 请求网络配置
     */
    private void doRetrieveVirtualNetworkConfig() {
        this.eventBus.post(new VirtualNetworkConfigRequestEvent(this.networkId));
    }

    /**
     * 更新是否通过 ZeroTier 进行路由的网络设置
     */
    public void doUpdateRouteViaZeroTier(boolean routeViaZeroTier) {
        var networkConfig = this.networkConfig.getValue();
        if (networkConfig == null) {
            Log.e(TAG, "Network config not found!");
            return;
        }

        // 更新数据库
        getNetworkRepository().updateRouteViaZeroTier(this.networkId, routeViaZeroTier);

        // 更新 LiveData
        this.networkConfig.setValue(new NetworkConfigDto(
                routeViaZeroTier,
                networkConfig.getDnsMode(),
                networkConfig.getDnsServers()
        ));

        // 触发事件
        this.eventBus.post(new NetworkConfigChangedByUserEvent(this.networkId));
    }

    /**
     * 处理网络配置回复事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onVirtualNetworkConfigReply(VirtualNetworkConfigReplyEvent event) {
        var config = event.getVirtualNetworkConfig();
        if (config == null) {
            Log.e(TAG, "Virtual network config not found!");
            return;
        }
        if (config.getNetworkId() != this.networkId) {
            return;
        }
        // 更新 LiveData
        this.virtualNetworkConfig.setValue(config);
        // 网络名称可能已更新，用实时配置中的名称刷新界面
        NetworkSummary current = this.network.getValue();
        if (current != null && config.getNetworkName() != null
                && !config.getNetworkName().isEmpty()) {
            this.network.setValue(new NetworkSummary(
                    current.getNetworkId(),
                    current.getNetworkIdStr(),
                    config.getNetworkName(),
                    current.isLastActivated()
            ));
        }
    }

    /**
     * 处理网络配置更新事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onVirtualNetworkConfigChanged(VirtualNetworkConfigChangedEvent event) {
        // 和 VirtualNetworkConfigReplyEvent 一样处理
        this.onVirtualNetworkConfigReply(new VirtualNetworkConfigReplyEvent(event.getVirtualNetworkConfig()));
    }

    /**
     * 获取网络概要。
     *
     * @return 网络概要 LiveData
     */
    public LiveData<NetworkSummary> getNetwork() {
        return network;
    }

    /**
     * 获取网络本地配置。
     *
     * @return 网络配置 LiveData
     */
    public LiveData<NetworkConfigDto> getNetworkConfig() {
        return networkConfig;
    }

    /**
     * 获取网络实时配置。
     *
     * @return 虚拟网络配置 LiveData
     */
    public LiveData<VirtualNetworkConfigDto> getVirtualNetworkConfig() {
        return virtualNetworkConfig;
    }
}
