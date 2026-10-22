package net.kaaass.zerotierfix.data.database.dao;

import net.kaaass.zerotierfix.data.database.entity.*;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MoonOrbitDao {

    @Query("SELECT * FROM MOON_ORBIT")
    List<MoonOrbit> loadAll();

    @Query("SELECT COUNT(*) FROM MOON_ORBIT WHERE _id = :moonWorldId")
    long countByWorldId(long moonWorldId);

    @Query("SELECT * FROM MOON_ORBIT WHERE _id = :moonWorldId AND MOON_SEED = :moonSeed")
    MoonOrbit findByWorldAndSeed(long moonWorldId, long moonSeed);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MoonOrbit moonOrbit);

    @Delete
    void delete(MoonOrbit moonOrbit);
}
