package net.kaaass.zerotierfix.events

/**
 * 应答服务是否运行事件。
 */
data class IsServiceRunningReplyEvent(
    /**
     * 服务是否正在运行。
     */
    val isRunning: Boolean
)
