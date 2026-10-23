package net.kaaass.zerotierfix.vpn.config

import android.net.VpnService
import android.util.Log

import com.zerotier.sdk.VirtualNetworkConfig

import net.kaaass.zerotierfix.model.dto.NetworkConfigDto
import net.kaaass.zerotierfix.model.enums.DNSMode

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

/**
 * 根据用户配置向 VPN 构建器添加 DNS 设置。
 */
object DnsConfigurator {

    private const val TAG = "DnsConfigurator"

    /**
     * 添加网络下发或用户自定义的 DNS 服务器。
     *
     * @param builder VPN 构建器，不能为空
     * @param networkConfig 本地网络配置，不能为空
     * @param virtualNetworkConfig 网络实时配置，不能为空
     * @param disableIPv6 是否禁用 IPv6
     */
    @JvmStatic
    fun addDnsServers(
        builder: VpnService.Builder,
        networkConfig: NetworkConfigDto,
        virtualNetworkConfig: VirtualNetworkConfig,
        disableIPv6: Boolean
    ) {
        val dnsMode = DNSMode.fromInt(networkConfig.dnsMode)
        when (dnsMode) {
            DNSMode.NETWORK_DNS ->
                addNetworkDns(builder, virtualNetworkConfig, disableIPv6)
            DNSMode.CUSTOM_DNS ->
                addCustomDns(builder, networkConfig, disableIPv6)
            else -> {
            }
        }
    }

    /**
     * 添加网络下发的 DNS 配置。
     *
     * @param builder VPN 构建器
     * @param virtualNetworkConfig 网络实时配置
     * @param disableIPv6 是否禁用 IPv6
     */
    private fun addNetworkDns(
        builder: VpnService.Builder,
        virtualNetworkConfig: VirtualNetworkConfig,
        disableIPv6: Boolean
    ) {
        val dns = virtualNetworkConfig.dns ?: return
        builder.addSearchDomain(dns.domain)
        for (inetSocketAddress in dns.servers) {
            addServer(builder, inetSocketAddress.address, disableIPv6)
        }
    }

    /**
     * 添加用户自定义的 DNS 配置。
     *
     * @param builder VPN 构建器
     * @param networkConfig 本地网络配置
     * @param disableIPv6 是否禁用 IPv6
     */
    private fun addCustomDns(
        builder: VpnService.Builder,
        networkConfig: NetworkConfigDto,
        disableIPv6: Boolean
    ) {
        for (dnsServer in networkConfig.dnsServers) {
            try {
                addServer(builder, InetAddress.getByName(dnsServer), disableIPv6)
            } catch (e: Exception) {
                Log.e(TAG, "Exception parsing DNS server: " + e, e)
            }
        }
    }

    /**
     * 按地址族添加单个 DNS 服务器。
     *
     * @param builder VPN 构建器
     * @param address DNS 地址
     * @param disableIPv6 是否禁用 IPv6
     */
    private fun addServer(
        builder: VpnService.Builder,
        address: InetAddress,
        disableIPv6: Boolean
    ) {
        if (address is Inet4Address) {
            builder.addDnsServer(address)
        } else if (address is Inet6Address && !disableIPv6) {
            builder.addDnsServer(address)
        }
    }
}
