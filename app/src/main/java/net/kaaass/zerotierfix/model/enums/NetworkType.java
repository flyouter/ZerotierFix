package net.kaaass.zerotierfix.model.enums;

/**
 * ZeroTier 网络类型。
 */
public enum NetworkType {

    /**
     * 未知类型。
     */
    UNKNOWN,

    /**
     * 私有网络，需要授权才能加入。
     */
    PRIVATE,

    /**
     * 公开网络，任何人都可以加入。
     */
    PUBLIC
}
