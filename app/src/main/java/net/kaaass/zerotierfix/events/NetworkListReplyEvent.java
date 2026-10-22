package net.kaaass.zerotierfix.events;

import net.kaaass.zerotierfix.model.dto.VirtualNetworkConfigDto;

import java.util.List;

import lombok.Value;

/**
 * 应答当前已连接网络列表事件。
 */
@Value
public class NetworkListReplyEvent {

    /**
     * 已连接网络的实时配置列表，不能为空。
     */
    List<VirtualNetworkConfigDto> networks;
}
