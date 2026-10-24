package net.kaaass.zerotierfix.features.networklist

import net.kaaass.zerotierfix.model.dto.NetworkSummary
import net.kaaass.zerotierfix.model.dto.VirtualNetworkConfigDto
import net.kaaass.zerotierfix.util.StringUtils

/**
 * 把数据库网络列表与引擎实时配置合并为 UI 条目。
 *
 * 纯函数，便于单元测试。输出顺序与 [summaries] 一致。
 *
 * @param summaries 数据库中的网络列表
 * @param configs 按网络 ID 索引的实时配置；`null` 表示尚未加载
 * @param disableIpv6 是否过滤 IPv6 地址
 * @return UI 条目列表
 */
fun buildNetworkItems(
    summaries: List<NetworkSummary>,
    configs: Map<Long, VirtualNetworkConfigDto>?,
    disableIpv6: Boolean
): List<NetworkListItem> = summaries.map { summary ->
    val config = configs?.get(summary.networkId)
    NetworkListItem(
        summary = summary,
        runtimeStatus = config?.status,
        ipAddresses = config?.assignedAddresses
            ?.mapNotNull { StringUtils.formatAddressWithPrefix(it, disableIpv6) }
            ?: emptyList(),
        displayName = summary.networkName?.takeIf { it.isNotEmpty() }
            ?: config?.networkName?.takeIf { it.isNotEmpty() }
    )
}
