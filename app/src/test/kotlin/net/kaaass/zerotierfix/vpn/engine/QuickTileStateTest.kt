package net.kaaass.zerotierfix.vpn.engine

import net.kaaass.zerotierfix.model.dto.NetworkSummary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [selectQuickTileTarget] 与 [mapQuickTileStatus] 的单元测试。
 */
class QuickTileStateTest {

    @Test
    fun `没有网络时目标为空`() {
        assertNull(selectQuickTileTarget(emptyList(), null))
        assertNull(selectQuickTileTarget(emptyList(), 1L))
    }

    @Test
    fun `只有一个网络时忽略上次连接记录`() {
        val networks = listOf(network(2L))

        assertEquals(2L, selectQuickTileTarget(networks, 1L))
        assertEquals(2L, selectQuickTileTarget(networks, null))
    }

    @Test
    fun `多个网络时优先上次连接的网络`() {
        val networks = listOf(network(1L), network(2L), network(3L))

        assertEquals(2L, selectQuickTileTarget(networks, 2L))
    }

    @Test
    fun `多个网络且没有上次记录时选择第一个`() {
        val networks = listOf(network(1L), network(2L), network(3L))

        assertEquals(1L, selectQuickTileTarget(networks, null))
    }

    @Test
    fun `多个网络且上次记录不在列表时选择第一个`() {
        val networks = listOf(network(1L), network(2L))

        assertEquals(1L, selectQuickTileTarget(networks, 99L))
    }

    @Test
    fun `网络列表未加载时显示未连接`() {
        assertEquals(
            QuickTileStatus.INACTIVE,
            mapQuickTileStatus(
                networksLoaded = false,
                networkCount = 0,
                serviceState = ServiceState.STOPPED,
                desiredConnected = null
            )
        )
    }

    @Test
    fun `没有网络且服务未运行时不可用`() {
        assertEquals(
            QuickTileStatus.UNAVAILABLE,
            mapQuickTileStatus(true, 0, ServiceState.STOPPED, null)
        )
        assertEquals(
            QuickTileStatus.UNAVAILABLE,
            mapQuickTileStatus(true, 0, ServiceState.UNKNOWN, null)
        )
        assertEquals(
            QuickTileStatus.UNAVAILABLE,
            mapQuickTileStatus(true, 0, ServiceState.STOPPED, true)
        )
    }

    @Test
    fun `没有网络但服务仍在运行时允许关闭`() {
        assertEquals(
            QuickTileStatus.ACTIVE,
            mapQuickTileStatus(true, 0, ServiceState.RUNNING, null)
        )
        assertEquals(
            QuickTileStatus.INACTIVE,
            mapQuickTileStatus(true, 0, ServiceState.RUNNING, false)
        )
        assertEquals(
            QuickTileStatus.CONNECTING,
            mapQuickTileStatus(true, 0, ServiceState.STARTING, null)
        )
    }

    @Test
    fun `期望连接时立即显示连接中或已连接`() {
        assertEquals(
            QuickTileStatus.CONNECTING,
            mapQuickTileStatus(true, 1, ServiceState.STOPPED, true)
        )
        assertEquals(
            QuickTileStatus.CONNECTING,
            mapQuickTileStatus(true, 1, ServiceState.STARTING, true)
        )
        assertEquals(
            QuickTileStatus.ACTIVE,
            mapQuickTileStatus(true, 1, ServiceState.RUNNING, true)
        )
    }

    @Test
    fun `期望断开时立即显示未连接`() {
        assertEquals(
            QuickTileStatus.INACTIVE,
            mapQuickTileStatus(true, 1, ServiceState.RUNNING, false)
        )
        assertEquals(
            QuickTileStatus.INACTIVE,
            mapQuickTileStatus(true, 1, ServiceState.STARTING, false)
        )
        assertEquals(
            QuickTileStatus.INACTIVE,
            mapQuickTileStatus(true, 1, ServiceState.STOPPED, false)
        )
    }

    @Test
    fun `没有显式意图时跟随服务状态`() {
        assertEquals(
            QuickTileStatus.ACTIVE,
            mapQuickTileStatus(true, 1, ServiceState.RUNNING, null)
        )
        assertEquals(
            QuickTileStatus.CONNECTING,
            mapQuickTileStatus(true, 1, ServiceState.STARTING, null)
        )
        assertEquals(
            QuickTileStatus.INACTIVE,
            mapQuickTileStatus(true, 1, ServiceState.STOPPED, null)
        )
        assertEquals(
            QuickTileStatus.INACTIVE,
            mapQuickTileStatus(true, 1, ServiceState.UNKNOWN, null)
        )
    }

    private fun network(networkId: Long) = NetworkSummary(
        networkId = networkId,
        networkIdStr = null,
        networkName = null,
        isLastActivated = false
    )
}
