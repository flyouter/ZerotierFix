package net.kaaass.zerotierfix.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 网络自定义 DNS 服务器。
 */
@Entity(tableName = "DNS_SERVER")
class DnsServer(
    /**
     * 自增主键。
     */
    @PrimaryKey
    @ColumnInfo(name = "_id")
    var id: Long? = null,

    /**
     * 所属网络 ID（十进制）。
     */
    @ColumnInfo(name = "NETWORK_ID")
    var networkId: Long? = null,

    /**
     * DNS 服务器地址。
     */
    @ColumnInfo(name = "NAMESERVER")
    var nameserver: String? = null
)
