package net.kaaass.zerotierfix.model.dto

/**
 * 网络概要信息，用于网络列表和跨层传递。
 */
data class NetworkSummary(
    /**
     * 网络 ID（十进制）。
     */
    val networkId: Long,

    /**
     * 网络 ID 的 16 进制字符串，保持用户输入时的格式；可能为 `null`。
     */
    val networkIdStr: String?,

    /**
     * 网络名称；可能为 `null` 或空字符串。
     */
    val networkName: String?,

    /**
     * 是否为最近一次激活的网络。
     */
    val isLastActivated: Boolean
)
