package net.kaaass.zerotierfix.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Moon 入轨信息实体。
 */
@Entity(tableName = "MOON_ORBIT")
class MoonOrbit(
    /**
     * Moon 地址。
     */
    @PrimaryKey
    @ColumnInfo(name = "_id")
    var moonWorldId: Long? = null,

    /**
     * Moon 种子。
     */
    @ColumnInfo(name = "MOON_SEED")
    var moonSeed: Long? = null,

    /**
     * Moon 是否从文件导入。
     */
    @ColumnInfo(name = "FROM_FILE")
    var fromFile: Boolean = false
)
