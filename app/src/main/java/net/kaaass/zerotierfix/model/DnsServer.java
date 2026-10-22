package net.kaaass.zerotierfix.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "DNS_SERVER")
public class DnsServer {

    @PrimaryKey
    @ColumnInfo(name = "_id")
    private Long id;

    @ColumnInfo(name = "NETWORK_ID")
    private Long networkId;

    @ColumnInfo(name = "NAMESERVER")
    private String nameserver;

    public DnsServer(Long id, Long networkId, String nameserver) {
        this.id = id;
        this.networkId = networkId;
        this.nameserver = nameserver;
    }

    public DnsServer() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNetworkId() {
        return this.networkId;
    }

    public void setNetworkId(Long networkId) {
        this.networkId = networkId;
    }

    public String getNameserver() {
        return this.nameserver;
    }

    public void setNameserver(String nameserver) {
        this.nameserver = nameserver;
    }
}
