package net.kaaass.zerotierfix.data.database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Moon 入轨信息实体
 *
 * @author kaaass
 */
@Entity(tableName = "MOON_ORBIT")
public class MoonOrbit {

    /**
     * Moon 地址
     */
    @PrimaryKey
    @ColumnInfo(name = "_id")
    private Long moonWorldId;

    /**
     * Moon 种子
     */
    @ColumnInfo(name = "MOON_SEED")
    private Long moonSeed;

    /**
     * Moon 是否从文件导入
     */
    @ColumnInfo(name = "FROM_FILE")
    private boolean fromFile = false;

    public MoonOrbit(Long moonWorldId, Long moonSeed, boolean fromFile) {
        this.moonWorldId = moonWorldId;
        this.moonSeed = moonSeed;
        this.fromFile = fromFile;
    }

    public MoonOrbit() {
    }

    public Long getMoonWorldId() {
        return this.moonWorldId;
    }

    public void setMoonWorldId(Long moonWorldId) {
        this.moonWorldId = moonWorldId;
    }

    public Long getMoonSeed() {
        return this.moonSeed;
    }

    public void setMoonSeed(Long moonSeed) {
        this.moonSeed = moonSeed;
    }

    public boolean getFromFile() {
        return this.fromFile;
    }

    public void setFromFile(boolean fromFile) {
        this.fromFile = fromFile;
    }
}
