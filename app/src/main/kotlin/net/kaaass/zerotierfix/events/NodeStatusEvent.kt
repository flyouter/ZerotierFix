package net.kaaass.zerotierfix.events

import net.kaaass.zerotierfix.model.dto.NodeStatusDto

/**
 * 节点状态事件。
 *
 * 在收到 [NodeStatusRequestEvent] 或 ZeroTier 事件时触发。
 */
data class NodeStatusEvent(
    /**
     * 节点自身状态，不能为空。
     */
    val status: NodeStatusDto
)
