package net.kaaass.zerotierfix.vpn.config;

import com.zerotier.sdk.VirtualNetworkConfig;
import com.zerotier.sdk.VirtualNetworkDNS;
import com.zerotier.sdk.VirtualNetworkStatus;
import com.zerotier.sdk.VirtualNetworkType;

import net.kaaass.zerotierfix.model.dto.VirtualNetworkConfigDto;
import net.kaaass.zerotierfix.model.dto.VirtualNetworkDnsDto;
import net.kaaass.zerotierfix.model.enums.NetworkStatus;
import net.kaaass.zerotierfix.model.enums.NetworkType;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 将 ZeroTier SDK 的虚拟网络配置转换为跨层 DTO。
 *
 * <p>SDK 到领域枚举的映射集中在此处，避免枚举依赖 SDK 类型。</p>
 */
public final class VirtualNetworkConfigMapper {

    private VirtualNetworkConfigMapper() {
    }

    /**
     * 转换单个虚拟网络配置。
     *
     * @param config SDK 配置，不能为空
     * @return 虚拟网络配置 DTO
     */
    public static VirtualNetworkConfigDto toDto(VirtualNetworkConfig config) {
        List<InetSocketAddress> addresses = config.getAssignedAddresses() == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(
                        Arrays.asList(config.getAssignedAddresses())));
        return new VirtualNetworkConfigDto(
                config.getNwid(),
                config.getName(),
                toStatus(config.getStatus()),
                toType(config.getType()),
                config.getMac(),
                config.getMtu(),
                config.isBroadcastEnabled(),
                config.isBridge(),
                addresses,
                toDns(config.getDns())
        );
    }

    /**
     * 转换虚拟网络配置数组。
     *
     * @param configs SDK 配置数组，不能为空
     * @return 配置 DTO 列表
     */
    public static List<VirtualNetworkConfigDto> toDtoList(VirtualNetworkConfig[] configs) {
        List<VirtualNetworkConfigDto> result = new ArrayList<>(configs.length);
        for (VirtualNetworkConfig config : configs) {
            result.add(toDto(config));
        }
        return result;
    }

    /**
     * 转换网络 DNS 配置。
     *
     * @param dns SDK DNS 配置，可为 {@code null}
     * @return DNS DTO；{@code dns} 为 {@code null} 时返回 {@code null}
     */
    private static VirtualNetworkDnsDto toDns(VirtualNetworkDNS dns) {
        if (dns == null) {
            return null;
        }
        List<InetSocketAddress> servers = dns.getServers() == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(dns.getServers()));
        return new VirtualNetworkDnsDto(dns.getDomain(), servers);
    }

    /**
     * 转换网络状态。
     *
     * @param status SDK 状态
     * @return 领域状态
     */
    private static NetworkStatus toStatus(VirtualNetworkStatus status) {
        switch (status) {
            case NETWORK_STATUS_REQUESTING_CONFIGURATION:
                return NetworkStatus.REQUESTING_CONFIGURATION;
            case NETWORK_STATUS_OK:
                return NetworkStatus.OK;
            case NETWORK_STATUS_ACCESS_DENIED:
                return NetworkStatus.ACCESS_DENIED;
            case NETWORK_STATUS_NOT_FOUND:
                return NetworkStatus.NOT_FOUND;
            case NETWORK_STATUS_PORT_ERROR:
                return NetworkStatus.PORT_ERROR;
            case NETWORK_STATUS_CLIENT_TOO_OLD:
                return NetworkStatus.CLIENT_TOO_OLD;
            case NETWORK_STATUS_AUTHENTICATION_REQUIRED:
                return NetworkStatus.AUTHENTICATION_REQUIRED;
            default:
                throw new IllegalArgumentException("Unhandled network status: " + status);
        }
    }

    /**
     * 转换网络类型。
     *
     * @param type SDK 类型
     * @return 领域类型
     */
    private static NetworkType toType(VirtualNetworkType type) {
        switch (type) {
            case NETWORK_TYPE_PRIVATE:
                return NetworkType.PRIVATE;
            case NETWORK_TYPE_PUBLIC:
                return NetworkType.PUBLIC;
            default:
                return NetworkType.UNKNOWN;
        }
    }
}
