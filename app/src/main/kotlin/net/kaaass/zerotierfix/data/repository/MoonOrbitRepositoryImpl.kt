package net.kaaass.zerotierfix.data.repository

import android.content.Context

import net.kaaass.zerotierfix.data.database.AppDatabase
import net.kaaass.zerotierfix.data.database.DatabaseUtils
import net.kaaass.zerotierfix.data.database.entity.MoonOrbit
import net.kaaass.zerotierfix.model.dto.MoonOrbitDto

import java.io.File

import dagger.hilt.android.qualifiers.ApplicationContext

import javax.inject.Inject
import javax.inject.Singleton

/**
 * [MoonOrbitRepository] 的 Room 实现。
 */
@Singleton
class MoonOrbitRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    @ApplicationContext context: Context
) : MoonOrbitRepository {

    private val context: Context = context.applicationContext

    override fun getAll(): List<MoonOrbitDto> {
        DatabaseUtils.readLock.lock()
        try {
            val orbits = database.moonOrbitDao().loadAll()
            val result = ArrayList<MoonOrbitDto>(orbits.size)
            for (orbit in orbits) {
                result.add(toDto(orbit))
            }
            return result
        } finally {
            DatabaseUtils.readLock.unlock()
        }
    }

    override fun exists(moonWorldId: Long): Boolean {
        DatabaseUtils.readLock.lock()
        try {
            return database.moonOrbitDao().countByWorldId(moonWorldId) > 0
        } finally {
            DatabaseUtils.readLock.unlock()
        }
    }

    override fun add(moonWorldId: Long, moonSeed: Long, fromFile: Boolean): Boolean {
        DatabaseUtils.writeLock.lock()
        try {
            var added = false
            database.runInTransaction {
                if (exists(moonWorldId)) {
                    return@runInTransaction
                }
                database.moonOrbitDao().insert(
                    MoonOrbit(moonWorldId, moonSeed, fromFile)
                )
                added = true
            }
            return added
        } finally {
            DatabaseUtils.writeLock.unlock()
        }
    }

    override fun remove(moonWorldId: Long, moonSeed: Long) {
        DatabaseUtils.writeLock.lock()
        try {
            val orbit = database.moonOrbitDao()
                .findByWorldAndSeed(moonWorldId, moonSeed)
            if (orbit == null) {
                return
            }
            database.moonOrbitDao().delete(orbit)
        } finally {
            DatabaseUtils.writeLock.unlock()
        }
        deleteCacheFile(moonWorldId)
    }

    override fun storeMoonFile(moonWorldId: Long, source: File): Boolean {
        val dest = moonFile(moonWorldId)
        val parent = dest.parentFile
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            return false
        }
        return source.renameTo(dest)
    }

    override fun hasCacheFile(moonWorldId: Long): Boolean = moonFile(moonWorldId).exists()

    override fun deleteCacheFile(moonWorldId: Long) {
        val file = moonFile(moonWorldId)
        if (file.exists()) {
            file.delete()
        }
    }

    /**
     * 将实体转换为 DTO，并填充缓存文件状态。
     *
     * @param orbit Moon 入轨实体
     * @return 入轨 DTO
     */
    private fun toDto(orbit: MoonOrbit): MoonOrbitDto = MoonOrbitDto(
        orbit.moonWorldId!!,
        orbit.moonSeed!!,
        orbit.fromFile,
        hasCacheFile(orbit.moonWorldId!!)
    )

    /**
     * 获取指定 Moon 的缓存文件路径。
     *
     * @param moonWorldId Moon 地址
     * @return 缓存文件
     */
    private fun moonFile(moonWorldId: Long): File =
        File(context.filesDir, String.format(MoonOrbitRepository.MOON_FILE_PATH, moonWorldId))
}
