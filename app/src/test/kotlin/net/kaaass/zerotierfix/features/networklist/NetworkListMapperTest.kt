package net.kaaass.zerotierfix.features.networklist

import net.kaaass.zerotierfix.model.dto.NetworkSummary
import net.kaaass.zerotierfix.model.dto.VirtualNetworkConfigDto
import net.kaaass.zerotierfix.model.enums.NetworkStatus
import net.kaaass.zerotierfix.model.enums.NetworkType

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

import java.net.InetSocketAddress

/**
 * [buildNetworkItems] 的单元测试。
 */
class NetworkListMapperTest {

    @Test
    fun `没有实时配置时状态与地址为空`() {
        val items = buildNetworkItems(
            summaries = listOf(summary(1L)),
            configs = null,
            disableIpv6 = false
        )

        assertEquals(1, items.size)
        assertNull(items.first().runtimeStatus)
        assertTrue(items.first().ipAddresses.isEmpty())
    }

    @Test
    fun `实时配置按网络 ID 关联`() {
        val items = buildNetworkItems(
            summaries = listOf(summary(1L), summary(2L)),
            configs = mapOf(1L to config(1L)),
            disableIpv6 = false
        )

        assertEquals(NetworkStatus.OK, items[0].runtimeStatus)
        assertEquals(listOf("10.0.0.2/24"), items[0].ipAddresses)
        assertNull(items[1].runtimeStatus)
    }

    @Test
    fun `启用 IPv6 过滤时排除 IPv6 地址`() {
        val items = buildNetworkItems(
            summaries = listOf(summary(1L)),
            configs = mapOf(1L to config(1L, includeIpv6 = true)),
            disableIpv6 = true
        )

        assertEquals(listOf("10.0.0.2/24"), items.first().ipAddresses)
    }

    @Test
    fun `网络名优先使用数据库名称`() {
        val items = buildNetworkItems(
            summaries = listOf(summary(1L)),
            configs = mapOf(1L to config(1L)),
            disableIpv6 = false
        )

        assertEquals("Network 1", items.first().displayName)
    }

    @Test
    fun `数据库名称为空时回退到引擎名称`() {
        val items = buildNetworkItems(
            summaries = listOf(summary(1L).copy(networkName = null)),
            configs = mapOf(1L to config(1L)),
            disableIpv6 = false
        )

        assertEquals("Network 1", items.first().displayName)
    }

    @Test
    fun `保留数据库列表顺序`() {
        val items = buildNetworkItems(
            summaries = listOf(summary(3L), summary(1L), summary(2L)),
            configs = emptyMap(),
            disableIpv6 = false
        )

        assertEquals(listOf(3L, 1L, 2L), items.map { it.summary.networkId })
    }

    private fun summary(networkId: Long): NetworkSummary = NetworkSummary(
        networkId = networkId,
        networkIdStr = networkId.toString(16).padStart(16, '0'),
        networkName = "Network $networkId",
        isLastActivated = false
    )

    private fun config(networkId: Long, includeIpv6: Boolean = false): VirtualNetworkConfigDto {
        val addresses = buildList {
            add(InetSocketAddress("10.0.0.2", 24))
            if (includeIpv6) {
                add(InetSocketAddress("fd00::2", 88))
            }
        }
        return VirtualNetworkConfigDto(
            networkId = networkId,
            networkName = "Network $networkId",
            status = NetworkStatus.OK,
            type = NetworkType.PRIVATE,
            mac = 0x112233445566L,
            mtu = 2800,
            isBroadcastEnabled = true,
            isBridge = false,
            assignedAddresses = addresses,
            dns = null
        )
    }
}
