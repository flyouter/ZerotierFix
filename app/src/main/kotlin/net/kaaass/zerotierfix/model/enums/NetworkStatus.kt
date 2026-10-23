package net.kaaass.zerotierfix.model.enums

/**
 * ZeroTier 网络状态。
 */
enum class NetworkStatus {

    /**
     * 正在请求网络配置。
     */
    REQUESTING_CONFIGURATION,

    /**
     * 网络连接正常。
     */
    OK,

    /**
     * 访问被拒绝。
     */
    ACCESS_DENIED,

    /**
     * 网络不存在。
     */
    NOT_FOUND,

    /**
     * 端口错误。
     */
    PORT_ERROR,

    /**
     * 客户端版本过旧。
     */
    CLIENT_TOO_OLD,

    /**
     * 需要认证。
     */
    AUTHENTICATION_REQUIRED
}
