package net.kaaass.zerotierfix.events

import net.kaaass.zerotierfix.model.dto.NetworkSummary
import net.kaaass.zerotierfix.model.dto.VirtualNetworkConfigDto

/**
 * 更新网络配置的结果事件。
 */
data class NetworkReconfigureEvent(
    /**
     * 本地配置是否发生变化。
     */
    val isChanged: Boolean,

    /**
     * 发生变化的网络概要。
     */
    val network: NetworkSummary,

    /**
     * 最新的虚拟网络配置，不能为空。
     */
    val virtualNetworkConfig: VirtualNetworkConfigDto
)
