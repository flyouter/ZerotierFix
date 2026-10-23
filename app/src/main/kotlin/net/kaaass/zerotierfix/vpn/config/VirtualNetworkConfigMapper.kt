package net.kaaass.zerotierfix.vpn.config

import com.zerotier.sdk.VirtualNetworkConfig
import com.zerotier.sdk.VirtualNetworkDNS
import com.zerotier.sdk.VirtualNetworkStatus
import com.zerotier.sdk.VirtualNetworkType

import net.kaaass.zerotierfix.model.dto.VirtualNetworkConfigDto
import net.kaaass.zerotierfix.model.dto.VirtualNetworkDnsDto
import net.kaaass.zerotierfix.model.enums.NetworkStatus
import net.kaaass.zerotierfix.model.enums.NetworkType

import java.net.InetSocketAddress
import java.util.ArrayList
import java.util.Collections

/**
 * 将 ZeroTier SDK 的虚拟网络配置转换为跨层 DTO。
 *
 * SDK 到领域枚举的映射集中在此处，避免枚举依赖 SDK 类型。
 */
object VirtualNetworkConfigMapper {

    /**
     * 转换单个虚拟网络配置。
     *
     * @param config SDK 配置，不能为空
     * @return 虚拟网络配置 DTO
     */
    @JvmStatic
    fun toDto(config: VirtualNetworkConfig): VirtualNetworkConfigDto {
        val rawAddresses = config.assignedAddresses
        val addresses: List<InetSocketAddress> = if (rawAddresses == null) {
            Collections.emptyList()
        } else {
            Collections.unmodifiableList(ArrayList(rawAddresses.asList()))
        }
        return VirtualNetworkConfigDto(
            config.nwid,
            config.name,
            toStatus(config.status),
            toType(config.type),
            config.mac,
            config.mtu,
            config.isBroadcastEnabled,
            config.isBridge,
            addresses,
            toDns(config.dns)
        )
    }

    /**
     * 转换虚拟网络配置数组。
     *
     * @param configs SDK 配置数组，不能为空
     * @return 配置 DTO 列表
     */
    @JvmStatic
    fun toDtoList(configs: Array<VirtualNetworkConfig>): List<VirtualNetworkConfigDto> {
        val result = ArrayList<VirtualNetworkConfigDto>(configs.size)
        for (config in configs) {
            result.add(toDto(config))
        }
        return result
    }

    /**
     * 转换网络 DNS 配置。
     *
     * @param dns SDK DNS 配置，可为 `null`
     * @return DNS DTO；`dns` 为 `null` 时返回 `null`
     */
    private fun toDns(dns: VirtualNetworkDNS?): VirtualNetworkDnsDto? {
        if (dns == null) {
            return null
        }
        val rawServers = dns.servers
        val servers: List<InetSocketAddress> = if (rawServers == null) {
            Collections.emptyList()
        } else {
            Collections.unmodifiableList(ArrayList(rawServers))
        }
        return VirtualNetworkDnsDto(dns.domain, servers)
    }

    /**
     * 转换网络状态。
     *
     * @param status SDK 状态
     * @return 领域状态
     */
    private fun toStatus(status: VirtualNetworkStatus): NetworkStatus = when (status) {
        VirtualNetworkStatus.NETWORK_STATUS_REQUESTING_CONFIGURATION ->
            NetworkStatus.REQUESTING_CONFIGURATION
        VirtualNetworkStatus.NETWORK_STATUS_OK -> NetworkStatus.OK
        VirtualNetworkStatus.NETWORK_STATUS_ACCESS_DENIED -> NetworkStatus.ACCESS_DENIED
        VirtualNetworkStatus.NETWORK_STATUS_NOT_FOUND -> NetworkStatus.NOT_FOUND
        VirtualNetworkStatus.NETWORK_STATUS_PORT_ERROR -> NetworkStatus.PORT_ERROR
        VirtualNetworkStatus.NETWORK_STATUS_CLIENT_TOO_OLD -> NetworkStatus.CLIENT_TOO_OLD
        VirtualNetworkStatus.NETWORK_STATUS_AUTHENTICATION_REQUIRED ->
            NetworkStatus.AUTHENTICATION_REQUIRED
        else -> throw IllegalArgumentException("Unhandled network status: $status")
    }

    /**
     * 转换网络类型。
     *
     * @param type SDK 类型
     * @return 领域类型
     */
    private fun toType(type: VirtualNetworkType): NetworkType = when (type) {
        VirtualNetworkType.NETWORK_TYPE_PRIVATE -> NetworkType.PRIVATE
        VirtualNetworkType.NETWORK_TYPE_PUBLIC -> NetworkType.PUBLIC
        else -> NetworkType.UNKNOWN
    }
}
