package net.kaaass.zerotierfix.data.database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

import net.kaaass.zerotierfix.model.enums.NetworkStatus;
import net.kaaass.zerotierfix.model.enums.NetworkType;

@Entity(tableName = "NETWORK_CONFIG")
public class NetworkConfig {
    /**
     * 网络配置主键，与网络 ID 相同。
     */
    @PrimaryKey
    @ColumnInfo(name = "_id")
    private Long id;

    /**
     * 旧版网络类型，已废弃。
     */
    @Deprecated
    @TypeConverters(NetworkTypeConverter.class)
    @ColumnInfo(name = "TYPE")
    private NetworkType type;

    /**
     * 旧版网络状态，已废弃。
     */
    @Deprecated
    @TypeConverters(NetworkStatusConverter.class)
    @ColumnInfo(name = "STATUS")
    private NetworkStatus status;

    /**
     * 旧版 MAC 地址，已废弃。
     */
    @Deprecated
    @ColumnInfo(name = "MAC")
    private String mac;

    /**
     * 旧版 MTU，已废弃。
     */
    @Deprecated
    @ColumnInfo(name = "MTU")
    private String mtu;

    /**
     * 旧版广播标记，已废弃。
     */
    @Deprecated
    @ColumnInfo(name = "BROADCAST")
    private boolean broadcast;

    /**
     * 旧版桥接标记，已废弃。
     */
    @Deprecated
    @ColumnInfo(name = "BRIDGING")
    private boolean bridging;

    /**
     * 是否通过 ZeroTier 路由默认流量。
     */
    @ColumnInfo(name = "ROUTE_VIA_ZERO_TIER")
    private boolean routeViaZeroTier;

    /**
     * 旧版自定义 DNS 标记，已废弃，使用 {@link #dnsMode}。
     */
    @Deprecated
    @ColumnInfo(name = "USE_CUSTOM_DNS")
    private boolean useCustomDNS;

    /**
     * DNS 模式，取值见 {@link net.kaaass.zerotierfix.model.enums.DNSMode}。
     */
    @ColumnInfo(name = "DNS_MODE")
    private int dnsMode;

    public NetworkConfig(Long id, NetworkType type, NetworkStatus status, String mac, String mtu,
            boolean broadcast, boolean bridging, boolean routeViaZeroTier, boolean useCustomDNS, int dnsMode) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.mac = mac;
        this.mtu = mtu;
        this.broadcast = broadcast;
        this.bridging = bridging;
        this.routeViaZeroTier = routeViaZeroTier;
        this.useCustomDNS = useCustomDNS;
        this.dnsMode = dnsMode;
    }

    public NetworkConfig(Long id, boolean routeViaZeroTier, int dnsMode) {
        this.id = id;
        this.routeViaZeroTier = routeViaZeroTier;
        this.dnsMode = dnsMode;
    }

    public NetworkConfig() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Deprecated
    public NetworkType getType() {
        return this.type;
    }

    @Deprecated
    public void setType(NetworkType type) {
        this.type = type;
    }

    @Deprecated
    public NetworkStatus getStatus() {
        return this.status;
    }

    @Deprecated
    public void setStatus(NetworkStatus status) {
        this.status = status;
    }

    @Deprecated
    public String getMac() {
        return this.mac;
    }

    @Deprecated
    public void setMac(String mac) {
        this.mac = mac;
    }

    @Deprecated
    public String getMtu() {
        return this.mtu;
    }

    @Deprecated
    public void setMtu(String mtu) {
        this.mtu = mtu;
    }

    @Deprecated
    public boolean getBroadcast() {
        return this.broadcast;
    }

    @Deprecated
    public void setBroadcast(boolean broadcast) {
        this.broadcast = broadcast;
    }

    @Deprecated
    public boolean getBridging() {
        return this.bridging;
    }

    @Deprecated
    public void setBridging(boolean bridging) {
        this.bridging = bridging;
    }

    public boolean getRouteViaZeroTier() {
        return this.routeViaZeroTier;
    }

    public void setRouteViaZeroTier(boolean routeViaZeroTier) {
        this.routeViaZeroTier = routeViaZeroTier;
    }

    @Deprecated
    public boolean getUseCustomDNS() {
        return this.useCustomDNS;
    }

    @Deprecated
    public void setUseCustomDNS(boolean useCustomDNS) {
        this.useCustomDNS = useCustomDNS;
    }

    public int getDnsMode() {
        return this.dnsMode;
    }

    public void setDnsMode(int dnsMode) {
        this.dnsMode = dnsMode;
    }

    public static class NetworkTypeConverter {
        @TypeConverter
        public static NetworkType convertToEntityProperty(Integer num) {
            if (num == null) {
                return null;
            }
            return NetworkType.fromInt(num);
        }

        @TypeConverter
        public static Integer convertToDatabaseValue(NetworkType networkType) {
            if (networkType == null) {
                return null;
            }
            return networkType.toInt();
        }
    }

    public static class NetworkStatusConverter {
        @TypeConverter
        public static NetworkStatus convertToEntityProperty(Integer num) {
            if (num == null) {
                return null;
            }
            return NetworkStatus.fromInt(num);
        }

        @TypeConverter
        public static Integer convertToDatabaseValue(NetworkStatus networkStatus) {
            if (networkStatus == null) {
                return null;
            }
            return networkStatus.toInt();
        }
    }
}
