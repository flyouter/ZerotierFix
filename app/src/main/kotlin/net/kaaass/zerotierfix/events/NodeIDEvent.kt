package net.kaaass.zerotierfix.events

/**
 * 节点 ID 变更事件。
 */
data class NodeIDEvent(
    /**
     * 节点地址（十进制）。
     */
    val nodeId: Long
)
