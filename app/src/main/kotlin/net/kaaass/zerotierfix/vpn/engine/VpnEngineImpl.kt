package net.kaaass.zerotierfix.vpn.engine

import dagger.Lazy

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
 * 命令层使用 [Lazy] 延迟构造：只读取状态的页面不会触发服务绑定。
 */
@Singleton
class VpnEngineImpl @Inject constructor(
    private val store: VpnStateStore,
    private val commands: Lazy<VpnServiceCommands>
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
        commands.get().startService(networkId)

    override suspend fun leaveAndStop(networkId: Long?) {
        if (networkId != null) {
            commands.get().leave(networkId)
        }
        commands.get().stop()
    }

    override suspend fun stop() {
        commands.get().stop()
    }

    override suspend fun orbit(moons: List<MoonOrbitDto>) {
        commands.get().orbit(moons)
    }

    override suspend fun loadPeers(): VpnCommandResult<List<PeerInfoDto>> =
        commands.get().loadPeers()

    override suspend fun reconfigure(networkId: Long) {
        commands.get().reconfigure(networkId)
    }

    override suspend fun virtualNetworkConfig(
        networkId: Long
    ): VpnCommandResult<VirtualNetworkConfigDto> = commands.get().virtualNetworkConfig(networkId)

    override suspend fun refreshState() {
        commands.get().refreshState()
    }
}
