package net.kaaass.zerotierfix.model.dto;

import lombok.Value;

/**
 * 本机 ZeroTier 节点状态。
 */
@Value
public class NodeStatusDto {

    /**
     * 是否在线。
     */
    boolean online;

    /**
     * 节点地址（十进制）。
     */
    long address;

    /**
     * 客户端版本号，格式为 {@code major.minor.revision}。
     */
    String clientVersion;
}
