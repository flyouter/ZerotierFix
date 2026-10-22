package net.kaaass.zerotierfix.events;

import net.kaaass.zerotierfix.model.dto.NetworkSummary;
import net.kaaass.zerotierfix.model.dto.VirtualNetworkConfigDto;

import lombok.Value;

/**
 * 更新网络配置的结果事件。
 */
@Value
public class NetworkReconfigureEvent {

    /**
     * 本地配置是否发生变化。
     */
    boolean changed;

    /**
     * 发生变化的网络概要。
     */
    NetworkSummary network;

    /**
     * 最新的虚拟网络配置，不能为空。
     */
    VirtualNetworkConfigDto virtualNetworkConfig;
}
