package net.kaaass.zerotierfix.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import net.kaaass.zerotierfix.data.database.entity.DnsServer

/**
 * DNS 服务器数据访问对象。
 */
@Dao
interface DnsServerDao {

    /**
     * 查询指定网络的 DNS 服务器。
     *
     * @param networkId 网络 ID
     * @return DNS 服务器列表
     */
    @Query("SELECT * FROM DNS_SERVER WHERE NETWORK_ID = :networkId")
    fun findByNetworkId(networkId: Long): List<DnsServer>

    /**
     * 插入或替换 DNS 服务器。
     *
     * @param dnsServer DNS 服务器记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(dnsServer: DnsServer)

    /**
     * 删除指定网络的全部 DNS 服务器。
     *
     * @param networkId 网络 ID
     */
    @Query("DELETE FROM DNS_SERVER WHERE NETWORK_ID = :networkId")
    fun deleteByNetworkId(networkId: Long)
}
