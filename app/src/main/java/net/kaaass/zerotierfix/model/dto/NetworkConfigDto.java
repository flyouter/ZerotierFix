package net.kaaass.zerotierfix.model.dto;

import java.util.List;

import lombok.Value;

/**
 * 网络本地配置，用于跨层传递。
 */
@Value
public class NetworkConfigDto {

    /**
     * 是否通过 ZeroTier 路由默认流量。
     */
    boolean routeViaZeroTier;

    /**
     * DNS 模式，取值见 {@link net.kaaass.zerotierfix.model.enums.DNSMode}。
     */
    int dnsMode;

    /**
     * 自定义 DNS 服务器地址列表；不为 {@code null}，无配置时为空列表。
     */
    List<String> dnsServers;
}
