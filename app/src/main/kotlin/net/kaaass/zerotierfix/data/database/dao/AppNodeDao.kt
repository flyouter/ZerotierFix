package net.kaaass.zerotierfix.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

import net.kaaass.zerotierfix.data.database.entity.AppNode

/**
 * 本机节点信息数据访问对象。
 */
@Dao
interface AppNodeDao {

    /**
     * 查询全部本机节点记录。
     *
     * @return 节点记录列表
     */
    @Query("SELECT * FROM APP_NODE")
    fun loadAll(): List<AppNode>

    /**
     * 插入或替换节点记录。
     *
     * @param node 节点记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(node: AppNode)

    /**
     * 更新节点记录。
     *
     * @param node 节点记录
     */
    @Update
    fun update(node: AppNode)
}
