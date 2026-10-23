package net.kaaass.zerotierfix.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

/**
 * 已加入的 ZeroTier 网络。
 */
@Entity(tableName = "NETWORK")
class Network(
    /**
     * 网络 ID（十进制），主键。
     */
    @PrimaryKey
    @ColumnInfo(name = "_id")
    var networkId: Long? = null,

    /**
     * 网络 ID 的 16 进制字符串。
     */
    @ColumnInfo(name = "NETWORK_ID_STR")
    var networkIdStr: String? = null,

    /**
     * 网络名称。
     */
    @ColumnInfo(name = "NETWORK_NAME")
    var networkName: String? = null,

    /**
     * 是否为最近一次激活的网络。
     */
    @ColumnInfo(name = "LAST_ACTIVATED")
    var lastActivated: Boolean = false,

    /**
     * 对应网络配置的主键。
     */
    @ColumnInfo(name = "NETWORK_CONFIG_ID")
    var networkConfigId: Long = 0L
) {
    /**
     * 关联的网络配置，由 [net.kaaass.zerotierfix.data.database.AppDatabase]
     * 的查询方法填充。
     *
     * 赋值时若配置带有非空主键，同步更新 [networkConfigId]。
     */
    @Ignore
    var networkConfig: NetworkConfig? = null
        set(value) {
            field = value
            val configId = value?.id
            if (configId != null) {
                networkConfigId = configId
            }
        }
}
