package net.kaaass.zerotierfix.vpn.engine

import com.zerotier.sdk.VirtualNetworkConfig

/**
 * `vpn/node` 与 `vpn/tun` 所需的最小平台能力。
 *
 * 由 [net.kaaass.zerotierfix.service.ZeroTierOneService] 实现，避免
 * `vpn` 包反向依赖 `service` 包。
 */
interface VpnRuntime {

    /**
     * 获取服务内存中的指定网络实时配置。
     *
     * @param networkId 网络 ID
     * @return 配置；尚未收到时返回 `null`
     */
    fun getVirtualNetworkConfig(networkId: Long): VirtualNetworkConfig?

    /**
     * 设置下一次后台任务截止时间。
     *
     * @param deadline 截止时间（毫秒时间戳）
     */
    fun setNextBackgroundTaskDeadline(deadline: Long)

    /**
     * 因致命错误关闭服务。
     */
    fun shutdown()
}
