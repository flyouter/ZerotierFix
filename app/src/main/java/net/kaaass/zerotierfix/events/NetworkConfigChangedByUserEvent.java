package net.kaaass.zerotierfix.events;

import lombok.Value;

/**
 * 用户更改 ZT 网络配置事件。
 */
@Value
public class NetworkConfigChangedByUserEvent {

    /**
     * 发生配置变更的网络 ID（十进制）。
     */
    long networkId;
}
