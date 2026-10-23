package net.kaaass.zerotierfix.model.dto

/**
 * 本机 ZeroTier 节点状态。
 */
data class NodeStatusDto(
    /**
     * 是否在线。
     */
    val isOnline: Boolean,

    /**
     * 节点地址（十进制）。
     */
    val address: Long,

    /**
     * 客户端版本号，格式为 `major.minor.revision`。
     */
    val clientVersion: String
)
