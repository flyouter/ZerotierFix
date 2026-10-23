package net.kaaass.zerotierfix.model.dto

/**
 * Moon 入轨信息，用于界面展示与跨层传递。
 */
data class MoonOrbitDto(
    /**
     * Moon 地址（world ID）。
     */
    val moonWorldId: Long,

    /**
     * Moon 种子节点地址。
     */
    val moonSeed: Long,

    /**
     * 是否从本地文件导入。
     */
    val isFromFile: Boolean,

    /**
     * 本地是否已经存在对应的 moon 缓存文件。
     */
    val isCacheFile: Boolean
)
