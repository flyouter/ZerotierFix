package net.kaaass.zerotierfix.model.dto

import androidx.compose.runtime.Immutable

import net.kaaass.zerotierfix.model.enums.PeerRole

/**
 * ZeroTier 对等节点信息。
 */
@Immutable
data class PeerInfoDto(
    /**
     * 节点地址（十进制）。
     */
    val address: Long,

    /**
     * 节点角色。
     */
    val role: PeerRole,

    /**
     * 主版本号。
     */
    val versionMajor: Int,

    /**
     * 次版本号。
     */
    val versionMinor: Int,

    /**
     * 修订版本号。
     */
    val versionRevision: Int,

    /**
     * 延迟（毫秒）。
     */
    val latency: Int,

    /**
     * 首选物理路径的字符串形式；无首选路径时为 `null`。
     */
    val preferredPath: String?
)
