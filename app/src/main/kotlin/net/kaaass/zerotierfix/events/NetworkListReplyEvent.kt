package net.kaaass.zerotierfix.events

import net.kaaass.zerotierfix.model.dto.VirtualNetworkConfigDto

/**
 * 应答当前已连接网络列表事件。
 */
data class NetworkListReplyEvent(
    /**
     * 已连接网络的实时配置列表，不能为空。
     */
    val networks: List<VirtualNetworkConfigDto>
)
