package net.kaaass.zerotierfix.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 网络的本地配置。
 */
@Entity(tableName = "NETWORK_CONFIG")
class NetworkConfig(
    /**
     * 网络配置主键，与网络 ID 相同。
     */
    @PrimaryKey
    @ColumnInfo(name = "_id")
    var id: Long? = null,

    /**
     * 是否通过 ZeroTier 路由默认流量。
     */
    @ColumnInfo(name = "ROUTE_VIA_ZERO_TIER")
    var routeViaZeroTier: Boolean = false,

    /**
     * DNS 模式，取值见 [net.kaaass.zerotierfix.model.enums.DNSMode]。
     */
    @ColumnInfo(name = "DNS_MODE")
    var dnsMode: Int = 0
)
