package net.kaaass.zerotierfix.data.repository;

import android.content.Context;

import net.kaaass.zerotierfix.data.database.AppDatabase;
import net.kaaass.zerotierfix.data.database.DatabaseUtils;
import net.kaaass.zerotierfix.data.database.entity.MoonOrbit;
import net.kaaass.zerotierfix.model.dto.MoonOrbitDto;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link MoonOrbitRepository} 的 Room 实现。
 */
public final class MoonOrbitRepositoryImpl implements MoonOrbitRepository {

    private final AppDatabase database;
    private final Context context;

    /**
     * 创建 Moon 入轨仓库。
     *
     * @param database 应用级 Room 数据库，不能为空
     * @param context  应用上下文，用于定位缓存文件
     */
    public MoonOrbitRepositoryImpl(AppDatabase database, Context context) {
        this.database = database;
        this.context = context.getApplicationContext();
    }

    @Override
    public List<MoonOrbitDto> getAll() {
        DatabaseUtils.readLock.lock();
        try {
            List<MoonOrbit> orbits = this.database.moonOrbitDao().loadAll();
            List<MoonOrbitDto> result = new ArrayList<>(orbits.size());
            for (MoonOrbit orbit : orbits) {
                result.add(toDto(orbit));
            }
            return result;
        } finally {
            DatabaseUtils.readLock.unlock();
        }
    }

    @Override
    public boolean exists(long moonWorldId) {
        DatabaseUtils.readLock.lock();
        try {
            return this.database.moonOrbitDao().countByWorldId(moonWorldId) > 0;
        } finally {
            DatabaseUtils.readLock.unlock();
        }
    }

    @Override
    public boolean add(long moonWorldId, long moonSeed, boolean fromFile) {
        DatabaseUtils.writeLock.lock();
        try {
            final boolean[] added = {false};
            this.database.runInTransaction(() -> {
                if (exists(moonWorldId)) {
                    return;
                }
                this.database.moonOrbitDao().insert(
                        new MoonOrbit(moonWorldId, moonSeed, fromFile)
                );
                added[0] = true;
            });
            return added[0];
        } finally {
            DatabaseUtils.writeLock.unlock();
        }
    }

    @Override
    public void remove(long moonWorldId, long moonSeed) {
        DatabaseUtils.writeLock.lock();
        try {
            MoonOrbit orbit = this.database.moonOrbitDao()
                    .findByWorldAndSeed(moonWorldId, moonSeed);
            if (orbit == null) {
                return;
            }
            this.database.moonOrbitDao().delete(orbit);
        } finally {
            DatabaseUtils.writeLock.unlock();
        }
        deleteCacheFile(moonWorldId);
    }

    @Override
    public boolean storeMoonFile(long moonWorldId, File source) {
        File dest = moonFile(moonWorldId);
        File parent = dest.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            return false;
        }
        return source.renameTo(dest);
    }

    @Override
    public boolean hasCacheFile(long moonWorldId) {
        return moonFile(moonWorldId).exists();
    }

    @Override
    public void deleteCacheFile(long moonWorldId) {
        File file = moonFile(moonWorldId);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 将实体转换为 DTO，并填充缓存文件状态。
     *
     * @param orbit Moon 入轨实体
     * @return 入轨 DTO
     */
    private MoonOrbitDto toDto(MoonOrbit orbit) {
        return new MoonOrbitDto(
                orbit.getMoonWorldId(),
                orbit.getMoonSeed(),
                orbit.getFromFile(),
                hasCacheFile(orbit.getMoonWorldId())
        );
    }

    /**
     * 获取指定 Moon 的缓存文件路径。
     *
     * @param moonWorldId Moon 地址
     * @return 缓存文件
     */
    private File moonFile(long moonWorldId) {
        return new File(this.context.getFilesDir(), String.format(MOON_FILE_PATH, moonWorldId));
    }
}
