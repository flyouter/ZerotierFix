package net.kaaass.zerotierfix.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

import net.kaaass.zerotierfix.data.database.entity.NetworkConfig

/**
 * 网络本地配置数据访问对象。
 */
@Dao
interface NetworkConfigDao {

    /**
     * 按主键查询网络配置。
     *
     * @param id 配置主键
     * @return 网络配置；不存在时返回 `null`
     */
    @Query("SELECT * FROM NETWORK_CONFIG WHERE _id = :id")
    fun findById(id: Long): NetworkConfig?

    /**
     * 插入或替换网络配置。
     *
     * @param networkConfig 网络配置
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(networkConfig: NetworkConfig)

    /**
     * 更新网络配置。
     *
     * @param networkConfig 网络配置
     */
    @Update
    fun update(networkConfig: NetworkConfig)

    /**
     * 按主键删除网络配置。
     *
     * @param id 配置主键
     */
    @Query("DELETE FROM NETWORK_CONFIG WHERE _id = :id")
    fun deleteById(id: Long)
}
