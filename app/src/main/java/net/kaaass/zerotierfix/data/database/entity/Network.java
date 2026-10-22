package net.kaaass.zerotierfix.data.database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 已加入的 ZeroTier 网络。
 */
@Entity(tableName = "NETWORK")
public class Network {

    /**
     * 网络 ID（十进制），主键。
     */
    @PrimaryKey
    @ColumnInfo(name = "_id")
    private Long networkId;

    /**
     * 网络 ID 的 16 进制字符串。
     */
    @ColumnInfo(name = "NETWORK_ID_STR")
    private String networkIdStr;

    /**
     * 网络名称。
     */
    @ColumnInfo(name = "NETWORK_NAME")
    private String networkName;

    /**
     * 是否为最近一次激活的网络。
     */
    @ColumnInfo(name = "LAST_ACTIVATED")
    private boolean lastActivated;

    /**
     * 对应网络配置的主键。
     */
    @ColumnInfo(name = "NETWORK_CONFIG_ID")
    private long networkConfigId;

    /**
     * 关联的网络配置，由 {@link net.kaaass.zerotierfix.data.database.AppDatabase}
     * 的查询方法填充。
     */
    @Ignore
    private NetworkConfig networkConfig;

    public Network(Long networkId, String networkIdStr, String networkName,
                   boolean lastActivated, long networkConfigId) {
        this.networkId = networkId;
        this.networkIdStr = networkIdStr;
        this.networkName = networkName;
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
