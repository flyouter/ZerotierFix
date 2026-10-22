package net.kaaass.zerotierfix.events;

import net.kaaass.zerotierfix.model.dto.NodeStatusDto;

import lombok.Value;

/**
 * 节点状态事件。
 *
 * <p>在收到 {@link NodeStatusRequestEvent} 或 ZeroTier 事件时触发。</p>
 */
@Value
public class NodeStatusEvent {

    /**
     * 节点自身状态，不能为空。
     */
    NodeStatusDto status;
}
