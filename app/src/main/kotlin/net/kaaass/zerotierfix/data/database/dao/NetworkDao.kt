package net.kaaass.zerotierfix.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

import net.kaaass.zerotierfix.data.database.entity.Network

/**
 * 网络数据访问对象。
 */
@Dao
interface NetworkDao {

    /**
     * 查询全部网络，按网络 ID 升序。
     *
     * @return 网络列表
     */
    @Query("SELECT * FROM NETWORK ORDER BY _id ASC")
    fun loadAllOrdered(): List<Network>

    /**
     * 按网络 ID 查询网络。
     *
     * @param networkId 网络 ID
     * @return 网络；不存在时返回 `null`
     */
    @Query("SELECT * FROM NETWORK WHERE _id = :networkId")
    fun findById(networkId: Long): Network?

    /**
     * 查询所有标记为最近激活的网络。
     *
     * @return 网络列表
     */
    @Query("SELECT * FROM NETWORK WHERE LAST_ACTIVATED = 1")
    fun findLastActivated(): List<Network>

    /**
     * 统计指定网络 ID 的记录数。
     *
     * @param networkId 网络 ID
     * @return 记录数
     */
    @Query("SELECT COUNT(*) FROM NETWORK WHERE _id = :networkId")
    fun countById(networkId: Long): Int

    /**
     * 插入或替换网络。
     *
     * @param network 网络记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(network: Network)

    /**
     * 更新网络。
     *
     * @param network 网络记录
     */
    @Update
    fun update(network: Network)

    /**
     * 删除网络。
     *
     * @param network 网络记录
     */
    @Delete
    fun delete(network: Network)
}
