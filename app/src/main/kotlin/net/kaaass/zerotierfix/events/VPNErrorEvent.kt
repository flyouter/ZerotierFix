package net.kaaass.zerotierfix.events

/**
 * VPN 错误事件。
 */
data class VPNErrorEvent(
    /**
     * 错误信息；底层异常无本地化信息时可能为 `null`。
     */
    val message: String?
)
