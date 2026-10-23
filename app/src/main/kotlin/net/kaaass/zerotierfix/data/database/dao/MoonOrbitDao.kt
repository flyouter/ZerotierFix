package net.kaaass.zerotierfix.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import net.kaaass.zerotierfix.data.database.entity.MoonOrbit

/**
 * Moon 入轨信息数据访问对象。
 */
@Dao
interface MoonOrbitDao {

    /**
     * 查询全部入轨信息。
     *
     * @return 入轨信息列表
     */
    @Query("SELECT * FROM MOON_ORBIT")
    fun loadAll(): List<MoonOrbit>

    /**
     * 统计指定 Moon 地址的入轨记录数。
     *
     * @param moonWorldId Moon 地址
     * @return 记录数
     */
    @Query("SELECT COUNT(*) FROM MOON_ORBIT WHERE _id = :moonWorldId")
    fun countByWorldId(moonWorldId: Long): Long

    /**
     * 按 Moon 地址与种子查询入轨信息。
     *
     * @param moonWorldId Moon 地址
     * @param moonSeed Moon 种子
     * @return 入轨记录；不存在时返回 `null`
     */
    @Query("SELECT * FROM MOON_ORBIT WHERE _id = :moonWorldId AND MOON_SEED = :moonSeed")
    fun findByWorldAndSeed(moonWorldId: Long, moonSeed: Long): MoonOrbit?

    /**
     * 插入或替换入轨信息。
     *
     * @param moonOrbit 入轨信息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(moonOrbit: MoonOrbit)

    /**
     * 删除入轨信息。
     *
     * @param moonOrbit 入轨信息
     */
    @Delete
    fun delete(moonOrbit: MoonOrbit)
}
