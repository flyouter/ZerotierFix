package net.kaaass.zerotierfix.data.database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "APP_NODE")
public class AppNode {

    /**
     * 节点地址（十进制）。
     */
    @PrimaryKey
    @ColumnInfo(name = "_id")
    private Long nodeId;

    /**
     * 节点地址的 16 进制字符串表示。
     */
    @ColumnInfo(name = "NODE_ID_STR")
    private String nodeIdStr;

    public AppNode(Long nodeId, String nodeIdStr) {
        this.nodeId = nodeId;
        this.nodeIdStr = nodeIdStr;
    }

    public AppNode() {
    }

    public Long getNodeId() {
        return this.nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeIdStr() {
        return this.nodeIdStr;
    }

    public void setNodeIdStr(String nodeIdStr) {
        this.nodeIdStr = nodeIdStr;
    }
}
