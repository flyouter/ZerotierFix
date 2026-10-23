package net.kaaass.zerotierfix.vpn.engine

import net.kaaass.zerotierfix.model.dto.VirtualNetworkConfigDto
import net.kaaass.zerotierfix.model.enums.NetworkStatus
import net.kaaass.zerotierfix.model.enums.NetworkType

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [VpnStateStore] 的状态读写测试。
 */
class VpnStateStoreTest {

    /**
     * 初始状态必须能区分“未知”和“已停止”。
     */
    @Test
    fun initialState() {
        val store = VpnStateStore()
        assertEquals(ServiceState.UNKNOWN, store.serviceState.value)
        assertEquals(BinderState.DISCONNECTED, store.binderState.value)
        assertNull(store.nodeStatus.value)
        assertNull(store.connectedNetworks.value)
        assertNull(store.virtualNetworkConfigs.value)
    }

    /**
     * 网络配置按网络 ID 写入与移除。
     */
    @Test
    fun setAndRemoveVirtualNetworkConfig() {
        val store = VpnStateStore()
        val config = sampleConfig(1L)
        store.setVirtualNetworkConfig(1L, config)
        assertEquals(config, store.virtualNetworkConfigs.value?.get(1L))

        store.setVirtualNetworkConfig(2L, sampleConfig(2L))
        assertEquals(2, store.virtualNetworkConfigs.value?.size)

        store.removeVirtualNetworkConfig(1L)
        assertNull(store.virtualNetworkConfigs.value?.get(1L))
        assertEquals(1, store.virtualNetworkConfigs.value?.size)
    }

    /**
     * 移除不存在的配置时保持 `null`，不把“未加载”变成空集合。
     */
    @Test
    fun removeFromUnloadedKeepsNull() {
        val store = VpnStateStore()
        store.removeVirtualNetworkConfig(1L)
        assertNull(store.virtualNetworkConfigs.value)
    }

    /**
     * 构造测试用网络配置。
     */
    private fun sampleConfig(networkId: Long): VirtualNetworkConfigDto = VirtualNetworkConfigDto(
        networkId = networkId,
        networkName = "test",
        status = NetworkStatus.OK,
        type = NetworkType.PRIVATE,
        mac = 0L,
        mtu = 2800,
        isBroadcastEnabled = false,
        isBridge = false,
        assignedAddresses = emptyList(),
        dns = null
    )
}
