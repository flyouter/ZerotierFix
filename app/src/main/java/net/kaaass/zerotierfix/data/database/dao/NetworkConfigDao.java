package net.kaaass.zerotierfix.data.database.dao;

import net.kaaass.zerotierfix.data.database.entity.*;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface NetworkConfigDao {

    @Query("SELECT * FROM NETWORK_CONFIG WHERE _id = :id")
    NetworkConfig findById(long id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(NetworkConfig networkConfig);

    @Update
    void update(NetworkConfig networkConfig);

    @Query("DELETE FROM NETWORK_CONFIG WHERE _id = :id")
    void deleteById(long id);
}
