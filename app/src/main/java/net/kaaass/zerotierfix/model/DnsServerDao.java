package net.kaaass.zerotierfix.model;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DnsServerDao {

    @Query("SELECT * FROM DNS_SERVER WHERE NETWORK_ID = :networkId")
    List<DnsServer> findByNetworkId(long networkId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DnsServer dnsServer);

    @Query("DELETE FROM DNS_SERVER WHERE NETWORK_ID = :networkId")
    void deleteByNetworkId(long networkId);
}
