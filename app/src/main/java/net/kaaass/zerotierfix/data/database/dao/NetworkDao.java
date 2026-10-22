package net.kaaass.zerotierfix.data.database.dao;

import net.kaaass.zerotierfix.data.database.entity.*;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface NetworkDao {

    @Query("SELECT * FROM NETWORK ORDER BY _id ASC")
    List<Network> loadAllOrdered();

    @Query("SELECT * FROM NETWORK WHERE _id = :networkId")
    Network findById(long networkId);

    @Query("SELECT * FROM NETWORK WHERE LAST_ACTIVATED = 1")
    List<Network> findLastActivated();

    @Query("SELECT COUNT(*) FROM NETWORK WHERE _id = :networkId")
    int countById(long networkId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Network network);

    @Update
    void update(Network network);

    @Delete
    void delete(Network network);
}
