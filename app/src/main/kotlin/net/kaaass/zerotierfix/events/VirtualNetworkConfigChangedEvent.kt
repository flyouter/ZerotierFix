package net.kaaass.zerotierfix.events

import net.kaaass.zerotierfix.model.dto.VirtualNetworkConfigDto

/**
 * ZT 网络配置成功更改事件。
 */
data class VirtualNetworkConfigChangedEvent(
    /**
     * 发生变化的虚拟网络配置，不能为空。
     */
    val virtualNetworkConfig: VirtualNetworkConfigDto
)
