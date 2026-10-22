package net.kaaass.zerotierfix.events;

import lombok.Data;

/**
 * 请求指定 ZT 网络 ID 的网络配置事件
 */
@Data
public class VirtualNetworkConfigRequestEvent {

    /**
     * 目标网络 ID（十进制）。
     */
    private final long networkId;
}
