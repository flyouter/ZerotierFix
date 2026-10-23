package net.kaaass.zerotierfix.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 本机 ZeroTier 节点信息。
 */
@Entity(tableName = "APP_NODE")
class AppNode(
    /**
     * 节点地址（十进制）。
     */
    @PrimaryKey
    @ColumnInfo(name = "_id")
    var nodeId: Long? = null,

    /**
     * 节点地址的 16 进制字符串表示。
     */
    @ColumnInfo(name = "NODE_ID_STR")
    var nodeIdStr: String? = null
)
