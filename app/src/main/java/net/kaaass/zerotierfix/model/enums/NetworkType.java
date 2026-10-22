package net.kaaass.zerotierfix.model.enums;

/**
 * ZeroTier 网络类型，持久化值为 {@link #toInt()}。
 */
public enum NetworkType {

    /**
     * 未知类型。
     */
    UNKNOWN(0),

    /**
     * 私有网络，需要授权才能加入。
     */
    PRIVATE(1),

    /**
     * 公开网络，任何人都可以加入。
     */
    PUBLIC(2);

    /**
     * 持久化用的整数值。
     */
    private final int id;

    NetworkType(int id) {
        this.id = id;
    }

    /**
     * 将持久化值转换为枚举。
     *
     * @param id 持久化值
     * @return 对应枚举
     * @throws IllegalArgumentException 值未知时抛出
     */
    public static NetworkType fromInt(int id) {
        switch (id) {
            case 0:
                return UNKNOWN;
            case 1:
                return PRIVATE;
            case 2:
                return PUBLIC;
            default:
                throw new IllegalArgumentException("Unhandled network type: " + id);
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
