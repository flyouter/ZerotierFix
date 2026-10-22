package net.kaaass.zerotierfix.model.enums;

/**
 * 网络 DNS 模式，持久化值为 {@link #toInt()}。
 */
public enum DNSMode {

    /**
     * 不使用 DNS。
     */
    NO_DNS(0),

    /**
     * 使用 ZeroTier 网络下发的 DNS。
     */
    NETWORK_DNS(1),

    /**
     * 使用用户自定义的 DNS。
     */
    CUSTOM_DNS(2);

    /**
     * 持久化用的整数值。
     */
    private final int id;

    DNSMode(int id) {
        this.id = id;
    }

    /**
     * 将持久化值转换为枚举。
     *
     * @param id 持久化值
     * @return 对应枚举
     * @throws IllegalArgumentException 值未知时抛出
     */
    public static DNSMode fromInt(int id) {
        switch (id) {
            case 0:
                return NO_DNS;
            case 1:
                return NETWORK_DNS;
            case 2:
                return CUSTOM_DNS;
            default:
                throw new IllegalArgumentException("Unhandled DNS mode: " + id);
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
