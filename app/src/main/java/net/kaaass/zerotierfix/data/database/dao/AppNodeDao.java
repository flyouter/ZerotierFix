package net.kaaass.zerotierfix.data.database.dao;

import net.kaaass.zerotierfix.data.database.entity.*;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AppNodeDao {

    @Query("SELECT * FROM APP_NODE")
    List<AppNode> loadAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AppNode node);

    @Update
    void update(AppNode node);
}
