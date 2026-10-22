package net.kaaass.zerotierfix.model;

import android.content.Context;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.File;

import lombok.Getter;

/**
 * Moon 入轨信息实体
 *
 * @author kaaass
 */
@Entity(tableName = "MOON_ORBIT")
public class MoonOrbit {

    /**
     * Moon 文件相对路径格式
     */
    public static final String MOON_FILE_PATH = "moons.d/%016x.moon";

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

    /**
     * Moon 文件是否已缓存
     */
    @Ignore
    @Getter
    private transient boolean cacheFile = false;

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

    /**
     * 判断 Moon 是否有缓存
     */
    public void checkCacheFile(Context context) {
        File moon = new File(context.getFilesDir(), String.format(MOON_FILE_PATH, moonWorldId));
        this.cacheFile = moon.exists();
    }

    /**
     * 删除 Moon 缓存
     */
    public void deleteCacheFile(Context context) {
        File moon = new File(context.getFilesDir(), String.format(MOON_FILE_PATH, moonWorldId));
        moon.delete();
    }
}
