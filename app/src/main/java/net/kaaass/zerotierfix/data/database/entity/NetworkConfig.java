package net.kaaass.zerotierfix.data.database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 网络的本地配置。
 */
@Entity(tableName = "NETWORK_CONFIG")
public class NetworkConfig {

    /**
     * 网络配置主键，与网络 ID 相同。
     */
    @PrimaryKey
    @ColumnInfo(name = "_id")
    private Long id;

    /**
     * 是否通过 ZeroTier 路由默认流量。
     */
    @ColumnInfo(name = "ROUTE_VIA_ZERO_TIER")
    private boolean routeViaZeroTier;

    /**
     * DNS 模式，取值见 {@link net.kaaass.zerotierfix.model.enums.DNSMode}。
     */
    @ColumnInfo(name = "DNS_MODE")
    private int dnsMode;

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

    public boolean getRouteViaZeroTier() {
        return this.routeViaZeroTier;
    }

    public void setRouteViaZeroTier(boolean routeViaZeroTier) {
        this.routeViaZeroTier = routeViaZeroTier;
    }

    public int getDnsMode() {
        return this.dnsMode;
    }

    public void setDnsMode(int dnsMode) {
        this.dnsMode = dnsMode;
    }
}
