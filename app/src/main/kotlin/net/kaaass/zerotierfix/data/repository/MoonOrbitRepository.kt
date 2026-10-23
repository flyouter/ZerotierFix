package net.kaaass.zerotierfix.data.repository

import net.kaaass.zerotierfix.model.dto.MoonOrbitDto

import java.io.File

/**
 * Moon 入轨信息仓库。
 *
 * 同时负责 moon 缓存文件的存放与删除；方法为同步调用。
 */
interface MoonOrbitRepository {

    companion object {
        /**
         * moon 缓存文件相对于应用 files 目录的路径格式。
         */
        const val MOON_FILE_PATH = "moons.d/%016x.moon"
    }

    /**
     * 获取全部入轨信息，并填充缓存文件状态。
     *
     * @return 入轨信息列表；无数据时返回空列表
     */
    fun getAll(): List<MoonOrbitDto>

    /**
     * 判断指定 Moon 是否已经入轨。
     *
     * @param moonWorldId Moon 地址
     * @return 已存在返回 `true`
     */
    fun exists(moonWorldId: Long): Boolean

    /**
     * 新增入轨信息。
     *
     * @param moonWorldId Moon 地址
     * @param moonSeed Moon 种子节点地址
     * @param fromFile 是否从文件导入
     * @return 新增成功返回 `true`；已存在返回 `false`
     */
    fun add(moonWorldId: Long, moonSeed: Long, fromFile: Boolean): Boolean

    /**
     * 删除入轨信息及其缓存文件。
     *
     * @param moonWorldId Moon 地址
     * @param moonSeed Moon 种子节点地址
     */
    fun remove(moonWorldId: Long, moonSeed: Long)

    /**
     * 将临时文件移动为指定 Moon 的缓存文件。
     *
     * @param moonWorldId Moon 地址
     * @param source 临时文件
     * @return 移动成功返回 `true`
     */
    fun storeMoonFile(moonWorldId: Long, source: File): Boolean

    /**
     * 判断指定 Moon 是否已有缓存文件。
     *
     * @param moonWorldId Moon 地址
     * @return 存在返回 `true`
     */
    fun hasCacheFile(moonWorldId: Long): Boolean

    /**
     * 删除指定 Moon 的缓存文件。
     *
     * @param moonWorldId Moon 地址
     */
    fun deleteCacheFile(moonWorldId: Long)
}
