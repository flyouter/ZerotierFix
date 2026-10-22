package net.kaaass.zerotierfix.events;

import net.kaaass.zerotierfix.model.dto.VirtualNetworkConfigDto;

import lombok.Value;

/**
 * ZT 网络配置成功更改事件。
 */
@Value
public class VirtualNetworkConfigChangedEvent {

    /**
     * 发生变化的虚拟网络配置，不能为空。
     */
    VirtualNetworkConfigDto virtualNetworkConfig;
}
