package net.kaaass.zerotierfix.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

import net.kaaass.zerotierfix.model.type.NetworkStatus;
import net.kaaass.zerotierfix.model.type.NetworkType;

@Entity(tableName = "NETWORK_CONFIG")
public class NetworkConfig {
    @PrimaryKey
    @ColumnInfo(name = "_id")
    private Long id;

    @Deprecated
    @TypeConverters(NetworkTypeConverter.class)
    @ColumnInfo(name = "TYPE")
    private NetworkType type;

    @Deprecated
    @TypeConverters(NetworkStatusConverter.class)
    @ColumnInfo(name = "STATUS")
    private NetworkStatus status;

    @Deprecated
    @ColumnInfo(name = "MAC")
    private String mac;

    @Deprecated
    @ColumnInfo(name = "MTU")
    private String mtu;

    @Deprecated
    @ColumnInfo(name = "BROADCAST")
    private boolean broadcast;

    @Deprecated
    @ColumnInfo(name = "BRIDGING")
    private boolean bridging;

    @ColumnInfo(name = "ROUTE_VIA_ZERO_TIER")
    private boolean routeViaZeroTier;

    @Deprecated
    @ColumnInfo(name = "USE_CUSTOM_DNS")
    private boolean useCustomDNS;

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
