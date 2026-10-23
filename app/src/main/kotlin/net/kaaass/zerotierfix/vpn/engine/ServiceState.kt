package net.kaaass.zerotierfix.vpn.engine

/**
 * ZeroTier 服务生命周期状态。
 */
enum class ServiceState {

    /**
     * 尚未探测。进程刚启动时使用，UI 应按离线显示。
     */
    UNKNOWN,

    /**
     * 服务未运行。
     */
    STOPPED,

    /**
     * 已请求启动，等待服务进入运行状态。
     */
    STARTING,

    /**
     * 服务已运行。
     */
    RUNNING
}

/**
 * 与服务 Binder 的连接状态。
 */
enum class BinderState {

    /**
     * 未绑定。
     */
    DISCONNECTED,

    /**
     * 正在绑定。
     */
    CONNECTING,

    /**
     * 已绑定。
     */
    CONNECTED
}
