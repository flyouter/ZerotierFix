package net.kaaass.zerotierfix.model.enums;

/**
 * ZeroTier 对等节点角色。
 */
public enum PeerRole {

    /**
     * 普通节点。
     */
    LEAF(0),

    /**
     * Moon 根节点。
     */
    MOON(1),

    /**
     * 行星根节点。
     */
    PLANET(2);

    /**
     * 持久化用的整数值。
     */
    private final int id;

    PeerRole(int id) {
        this.id = id;
    }

    /**
     * 将持久化值转换为枚举。
     *
     * @param id 持久化值
     * @return 对应枚举
     * @throws IllegalArgumentException 值未知时抛出
     */
    public static PeerRole fromInt(int id) {
        switch (id) {
            case 0:
                return LEAF;
            case 1:
                return MOON;
            case 2:
                return PLANET;
            default:
                throw new IllegalArgumentException("Unhandled peer role: " + id);
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
