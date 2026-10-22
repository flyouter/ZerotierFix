package net.kaaass.zerotierfix.model.enums;

/**
 * ZeroTier 网络状态，持久化值为 {@link #toInt()}。
 */
public enum NetworkStatus {

    /**
     * 正在请求网络配置。
     */
    REQUESTING_CONFIGURATION(0),

    /**
     * 网络连接正常。
     */
    OK(1),

    /**
     * 访问被拒绝。
     */
    ACCESS_DENIED(2),

    /**
     * 网络不存在。
     */
    NOT_FOUND(3),

    /**
     * 端口错误。
     */
    PORT_ERROR(4),

    /**
     * 客户端版本过旧。
     */
    CLIENT_TOO_OLD(5),

    /**
     * 需要认证。
     */
    AUTHENTICATION_REQUIRED(6);

    /**
     * 持久化用的整数值。
     */
    private final int id;

    NetworkStatus(int id) {
        this.id = id;
    }

    /**
     * 将持久化值转换为枚举。
     *
     * @param id 持久化值
     * @return 对应枚举
     * @throws IllegalArgumentException 值未知时抛出
     */
    public static NetworkStatus fromInt(int id) {
        switch (id) {
            case 0:
                return REQUESTING_CONFIGURATION;
            case 1:
                return OK;
            case 2:
                return ACCESS_DENIED;
            case 3:
                return NOT_FOUND;
            case 4:
                return PORT_ERROR;
            case 5:
                return CLIENT_TOO_OLD;
            case 6:
                return AUTHENTICATION_REQUIRED;
            default:
                throw new IllegalArgumentException("Unhandled network status: " + id);
        }
    }

    /**
     * 获取持久化值。
     *
     * @return 持久化用的整数值
     */
    public int toInt() {
        return this.id;
    }
}
