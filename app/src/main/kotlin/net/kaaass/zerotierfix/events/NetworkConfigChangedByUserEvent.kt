package net.kaaass.zerotierfix.events

/**
 * 用户更改 ZT 网络配置事件。
 */
data class NetworkConfigChangedByUserEvent(
    /**
     * 发生配置变更的网络 ID（十进制）。
     */
    val networkId: Long
)
