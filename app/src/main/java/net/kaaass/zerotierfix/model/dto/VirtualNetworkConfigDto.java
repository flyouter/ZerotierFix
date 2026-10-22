package net.kaaass.zerotierfix.model.dto;

import net.kaaass.zerotierfix.model.enums.NetworkStatus;
import net.kaaass.zerotierfix.model.enums.NetworkType;

import java.net.InetSocketAddress;
import java.util.List;

import lombok.Value;

/**
 * ZeroTier 虚拟网络的实时配置，用于跨层传递。
 */
@Value
public class VirtualNetworkConfigDto {

    /**
     * 网络 ID（十进制）。
     */
    long networkId;

    /**
     * 网络名称；可能为 {@code null} 或空字符串。
     */
    String networkName;

    /**
     * 网络状态。
     */
    NetworkStatus status;

    /**
     * 网络类型。
     */
    NetworkType type;

    /**
     * 虚拟网卡 MAC 地址。
     */
    long mac;

    /**
     * 最大传输单元。
     */
    int mtu;

    /**
     * 是否启用广播。
     */
    boolean broadcastEnabled;

    /**
     * 是否启用桥接。
     */
    boolean bridge;

    /**
     * 分配给本机的地址列表；不为 {@code null}。
     */
    List<InetSocketAddress> assignedAddresses;

    /**
     * 网络下发的 DNS 配置；未配置时为 {@code null}。
     */
    VirtualNetworkDnsDto dns;
}
