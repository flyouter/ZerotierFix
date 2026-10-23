package net.kaaass.zerotierfix.vpn.engine

/**
 * VPN 命令执行结果。
 *
 * 用显式结果区分“服务未运行”“节点未就绪”和“执行失败”，避免调用方把
 * binder 为空误判为执行成功。
 */
sealed interface VpnCommandResult<out T> {

    /**
     * 命令执行成功。
     *
     * @property value 命令返回值
     */
    data class Success<T>(val value: T) : VpnCommandResult<T>

    /**
     * ZeroTier 服务未运行或无法绑定。
     */
    data object ServiceNotRunning : VpnCommandResult<Nothing>

    /**
     * 服务已运行，但 ZeroTier 节点尚未初始化。
     */
    data object NodeNotReady : VpnCommandResult<Nothing>

    /**
     * 命令执行过程中发生异常。
     *
     * @property throwable 原始异常；可能为 `null`
     */
    data class Failure(val throwable: Throwable? = null) : VpnCommandResult<Nothing>
}
