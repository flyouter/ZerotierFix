package net.kaaass.zerotierfix.vpn.config;

import android.net.VpnService;
import android.util.Log;

import com.zerotier.sdk.VirtualNetworkConfig;

import net.kaaass.zerotierfix.model.dto.NetworkConfigDto;
import net.kaaass.zerotierfix.model.enums.DNSMode;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

/**
 * 根据用户配置向 VPN 构建器添加 DNS 设置。
 */
public final class DnsConfigurator {

    private static final String TAG = "DnsConfigurator";

    private DnsConfigurator() {
    }

    /**
     * 添加网络下发或用户自定义的 DNS 服务器。
     *
     * @param builder                VPN 构建器，不能为空
     * @param networkConfig          本地网络配置，不能为空
     * @param virtualNetworkConfig   网络实时配置，不能为空
     * @param disableIPv6            是否禁用 IPv6
     */
    public static void addDnsServers(VpnService.Builder builder,
                                     NetworkConfigDto networkConfig,
                                     VirtualNetworkConfig virtualNetworkConfig,
                                     boolean disableIPv6) {
        var dnsMode = DNSMode.fromInt(networkConfig.getDnsMode());
        switch (dnsMode) {
            case NETWORK_DNS:
                addNetworkDns(builder, virtualNetworkConfig, disableIPv6);
                break;
            case CUSTOM_DNS:
                addCustomDns(builder, networkConfig, disableIPv6);
                break;
            default:
                break;
        }
    }

    /**
     * 添加网络下发的 DNS 配置。
     *
     * @param builder              VPN 构建器
     * @param virtualNetworkConfig 网络实时配置
     * @param disableIPv6          是否禁用 IPv6
     */
    private static void addNetworkDns(VpnService.Builder builder,
                                      VirtualNetworkConfig virtualNetworkConfig,
                                      boolean disableIPv6) {
        if (virtualNetworkConfig.getDns() == null) {
            return;
        }
        builder.addSearchDomain(virtualNetworkConfig.getDns().getDomain());
        for (var inetSocketAddress : virtualNetworkConfig.getDns().getServers()) {
            addServer(builder, inetSocketAddress.getAddress(), disableIPv6);
        }
    }

    /**
     * 添加用户自定义的 DNS 配置。
     *
     * @param builder        VPN 构建器
     * @param networkConfig  本地网络配置
     * @param disableIPv6    是否禁用 IPv6
     */
    private static void addCustomDns(VpnService.Builder builder,
                                     NetworkConfigDto networkConfig,
                                     boolean disableIPv6) {
        for (String dnsServer : networkConfig.getDnsServers()) {
            try {
                addServer(builder, InetAddress.getByName(dnsServer), disableIPv6);
            } catch (Exception e) {
                Log.e(TAG, "Exception parsing DNS server: " + e, e);
            }
        }
    }

    /**
     * 按地址族添加单个 DNS 服务器。
     *
     * @param builder     VPN 构建器
     * @param address     DNS 地址
     * @param disableIPv6 是否禁用 IPv6
     */
    private static void addServer(VpnService.Builder builder, InetAddress address,
                                  boolean disableIPv6) {
        if (address instanceof Inet4Address) {
            builder.addDnsServer(address);
        } else if (address instanceof Inet6Address && !disableIPv6) {
            builder.addDnsServer(address);
        }
    }
}
