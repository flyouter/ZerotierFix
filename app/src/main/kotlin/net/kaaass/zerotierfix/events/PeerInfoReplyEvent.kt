package net.kaaass.zerotierfix.events

import net.kaaass.zerotierfix.model.dto.PeerInfoDto

/**
 * 应答节点信息事件。
 */
data class PeerInfoReplyEvent(
    /**
     * Peer 信息列表；服务未运行时为 `null`。
     */
    val peers: List<PeerInfoDto>?
)
