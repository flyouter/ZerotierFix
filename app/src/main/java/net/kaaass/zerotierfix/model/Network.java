package net.kaaass.zerotierfix.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "NETWORK")
public class Network {
    @PrimaryKey
    @ColumnInfo(name = "_id")
    private Long networkId;

    @ColumnInfo(name = "NETWORK_ID_STR")
    private String networkIdStr;

    @ColumnInfo(name = "NETWORK_NAME")
    private String networkName;

    @Deprecated
    @ColumnInfo(name = "USE_DEFAULT_ROUTE")
    private boolean useDefaultRoute;

    @ColumnInfo(name = "LAST_ACTIVATED")
    private boolean lastActivated;

    @ColumnInfo(name = "NETWORK_CONFIG_ID")
    private long networkConfigId;

    @Ignore
    @Deprecated
    private boolean connected;

    /**
     * 关联的网络配置，由 {@link AppDatabase} 的查询方法填充。
     */
    @Ignore
    private NetworkConfig networkConfig;

    public Network(Long networkId, String networkIdStr, String networkName, boolean useDefaultRoute,
            boolean lastActivated, long networkConfigId) {
        this.networkId = networkId;
        this.networkIdStr = networkIdStr;
        this.networkName = networkName;
        this.useDefaultRoute = useDefaultRoute;
        this.lastActivated = lastActivated;
        this.networkConfigId = networkConfigId;
    }

    public Network() {
    }

    public Long getNetworkId() {
        return this.networkId;
    }

    public void setNetworkId(Long networkId) {
        this.networkId = networkId;
    }

    public String getNetworkIdStr() {
        return this.networkIdStr;
    }

    public void setNetworkIdStr(String networkIdStr) {
        this.networkIdStr = networkIdStr;
    }

    public String getNetworkName() {
        return this.networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    @Deprecated
    public boolean getUseDefaultRoute() {
        return this.useDefaultRoute;
    }

    @Deprecated
    public void setUseDefaultRoute(boolean useDefaultRoute) {
        this.useDefaultRoute = useDefaultRoute;
    }

    public boolean getLastActivated() {
        return this.lastActivated;
    }

    public void setLastActivated(boolean lastActivated) {
        this.lastActivated = lastActivated;
    }

    public long getNetworkConfigId() {
        return this.networkConfigId;
    }

    public void setNetworkConfigId(long networkConfigId) {
        this.networkConfigId = networkConfigId;
    }

    @Deprecated
    public boolean getConnected() {
        return this.connected;
    }

    @Deprecated
    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public NetworkConfig getNetworkConfig() {
        return this.networkConfig;
    }

    public void setNetworkConfig(NetworkConfig networkConfig) {
        this.networkConfig = networkConfig;
        if (networkConfig != null && networkConfig.getId() != null) {
            this.networkConfigId = networkConfig.getId();
        }
    }
}
