package net.kaaass.zerotierfix.model;

import androidx.room.Dao;
import androidx.room.Query;

@Dao
public interface AssignedAddressDao {

    @Query("DELETE FROM ASSIGNED_ADDRESS WHERE NETWORK_ID = :networkId")
    void deleteByNetworkId(long networkId);
}
