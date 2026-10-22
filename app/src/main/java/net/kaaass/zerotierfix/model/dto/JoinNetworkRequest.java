package net.kaaass.zerotierfix.model.dto;

import java.util.List;

import lombok.Value;

/**
 * 加入 ZeroTier 网络的请求参数。
 */
@Value
public class JoinNetworkRequest {

    /**
     * 网络 ID（十进制）。
     */
    long networkId;

    /**
     * 网络 ID 的 16 进制字符串，保持用户输入格式。
     */
    String networkIdStr;

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
