package net.kaaass.zerotierfix.events;

import net.kaaass.zerotierfix.model.dto.VirtualNetworkConfigDto;

import lombok.Value;

/**
 * 应答获取指定 ZT 网络 ID 的网络配置事件。
 */
@Value
public class VirtualNetworkConfigReplyEvent {

    /**
     * 网络配置；服务未运行或网络不存在时为 {@code null}。
     */
    VirtualNetworkConfigDto virtualNetworkConfig;
}
