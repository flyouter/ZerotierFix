package net.kaaass.zerotierfix.events;

import net.kaaass.zerotierfix.model.dto.PeerInfoDto;

import java.util.List;

import lombok.Value;

/**
 * 应答节点信息事件。
 */
@Value
public class PeerInfoReplyEvent {

    /**
     * Peer 信息列表；服务未运行时为 {@code null}。
     */
    List<PeerInfoDto> peers;
}
