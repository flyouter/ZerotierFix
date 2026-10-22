package net.kaaass.zerotierfix.events;

import lombok.Value;

/**
 * 节点 ID 变更事件。
 */
@Value
public class NodeIDEvent {

    /**
     * 节点地址（十进制）。
     */
    long nodeId;
}
