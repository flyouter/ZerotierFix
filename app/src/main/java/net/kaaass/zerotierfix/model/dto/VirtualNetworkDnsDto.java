package net.kaaass.zerotierfix.model.dto;

import java.net.InetSocketAddress;
import java.util.List;

import lombok.Value;

/**
 * ZeroTier 网络下发的 DNS 配置。
 */
@Value
public class VirtualNetworkDnsDto {

    /**
     * DNS 搜索域；可能为空字符串。
     */
    String domain;

    /**
     * DNS 服务器地址列表；不为 {@code null}。
     */
    List<InetSocketAddress> servers;
}
