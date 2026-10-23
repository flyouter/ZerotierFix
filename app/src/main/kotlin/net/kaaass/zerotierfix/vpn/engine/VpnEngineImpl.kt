package net.kaaass.zerotierfix.vpn.engine

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

import net.kaaass.zerotierfix.model.dto.MoonOrbitDto
import net.kaaass.zerotierfix.model.dto.NodeStatusDto
import net.kaaass.zerotierfix.model.dto.PeerInfoDto
import net.kaaass.zerotierfix.model.dto.VirtualNetworkConfigDto

import javax.inject.Inject
import javax.inject.Singleton

/**
 * [VpnEngine] 的默认实现。
 *
 * 状态直接转发 [VpnStateStore]，命令委托给 [VpnServiceCommands]。
 */
@Singleton
class VpnEngineImpl @Inject constructor(
    private val store: VpnStateStore,
    private val commands: VpnServiceCommands
) : VpnEngine {

    override val serviceState: StateFlow<ServiceState> = store.serviceState
    override val binderState: StateFlow<BinderState> = store.binderState
    override val nodeId: StateFlow<String?> = store.nodeId
    override val nodeStatus: StateFlow<NodeStatusDto?> = store.nodeStatus
    override val connectedNetworks: StateFlow<List<VirtualNetworkConfigDto>?> =
        store.connectedNetworks
    override val virtualNetworkConfigs: StateFlow<Map<Long, VirtualNetworkConfigDto>?> =
        store.virtualNetworkConfigs
    override val errors: SharedFlow<String> = store.errors
    override val statusNotices: SharedFlow<VirtualNetworkConfigDto> = store.statusNotices

    override suspend fun start(networkId: Long): Boolean =
        commands.startService(networkId)

    override suspend fun leaveAndStop(networkId: Long?) {
        if (networkId != null) {
            commands.leave(networkId)
        }
        commands.stop()
    }

    override suspend fun stop() {
        commands.stop()
    }

    override suspend fun orbit(moons: List<MoonOrbitDto>) {
        commands.orbit(moons)
    }

    override suspend fun loadPeers(): VpnCommandResult<List<PeerInfoDto>> =
        commands.loadPeers()

    override suspend fun reconfigure(networkId: Long) {
        commands.reconfigure(networkId)
    }

    override suspend fun virtualNetworkConfig(
        networkId: Long
    ): VpnCommandResult<VirtualNetworkConfigDto> = commands.virtualNetworkConfig(networkId)

    override suspend fun refreshState() {
        commands.refreshState()
    }
}
