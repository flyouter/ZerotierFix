package net.kaaass.zerotierfix.events

/**
 * 请求指定 ZT 网络 ID 的网络配置事件。
 */
data class VirtualNetworkConfigRequestEvent(
    /**
     * 目标网络 ID（十进制）。
     */
    val networkId: Long
)
