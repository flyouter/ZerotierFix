package net.kaaass.zerotierfix.model.dto;

import net.kaaass.zerotierfix.model.enums.PeerRole;

import lombok.Value;

/**
 * ZeroTier 对等节点信息。
 */
@Value
public class PeerInfoDto {

    /**
     * 节点地址（十进制）。
     */
    long address;

    /**
     * 节点角色。
     */
    PeerRole role;

    /**
     * 主版本号。
     */
    int versionMajor;

    /**
     * 次版本号。
     */
    int versionMinor;

    /**
     * 修订版本号。
     */
    int versionRevision;

    /**
     * 延迟（毫秒）。
     */
    int latency;

    /**
     * 首选物理路径的字符串形式；无首选路径时为 {@code null}。
     */
    String preferredPath;
}
