package net.kaaass.zerotierfix.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "APP_NODE")
public class AppNode {

    @PrimaryKey
    @ColumnInfo(name = "_id")
    private Long nodeId;

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
