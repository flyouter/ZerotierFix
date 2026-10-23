package net.kaaass.zerotierfix.vpn.engine

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

import net.kaaass.zerotierfix.model.dto.MoonOrbitDto
import net.kaaass.zerotierfix.model.dto.NodeStatusDto
import net.kaaass.zerotierfix.model.dto.PeerInfoDto
import net.kaaass.zerotierfix.model.dto.VirtualNetworkConfigDto

/**
 * ZeroTier 引擎门面，供 UI 层使用。
 *
 * 只暴露状态 Flow 与命令，不暴露 Service、Binder、SDK 类型。
 */
interface VpnEngine {

    /** 服务生命周期状态。 */
    val serviceState: StateFlow<ServiceState>

    /** Binder 连接状态。 */
    val binderState: StateFlow<BinderState>

    /** 本机节点 ID。 */
    val nodeId: StateFlow<String?>

    /** 节点在线状态。 */
    val nodeStatus: StateFlow<NodeStatusDto?>

    /** 服务当前连接的网络配置列表；`null` 表示尚未加载。 */
    val connectedNetworks: StateFlow<List<VirtualNetworkConfigDto>?>

    /** 各网络实时配置；`null` 表示尚未加载。 */
    val virtualNetworkConfigs: StateFlow<Map<Long, VirtualNetworkConfigDto>?>

    /** VPN 错误消息。 */
    val errors: SharedFlow<String>

    /** 网络状态提示。 */
    val statusNotices: SharedFlow<VirtualNetworkConfigDto>

    /**
     * 启动服务并连接指定网络。
     *
     * @param networkId 网络 ID
     * @return 启动请求是否成功发出
     */
    suspend fun start(networkId: Long): Boolean

    /**
     * 离开当前网络并停止服务。
     *
     * @param networkId 当前连接的网络 ID；没有时传 `null`
     */
    suspend fun leaveAndStop(networkId: Long?)

    /**
     * 停止服务。
     */
    suspend fun stop()

    /**
     * 入轨 Moon。
     *
     * @param moons Moon 信息列表
     */
    suspend fun orbit(moons: List<MoonOrbitDto>)

    /**
     * 获取 Peer 列表。
     *
     * @return 命令结果
     */
    suspend fun loadPeers(): VpnCommandResult<List<PeerInfoDto>>

    /**
     * 请求重新配置指定网络。
     *
     * @param networkId 网络 ID
     */
    suspend fun reconfigure(networkId: Long)

    /**
     * 获取指定网络的实时配置快照。
     *
     * @param networkId 网络 ID
     * @return 命令结果
     */
    suspend fun virtualNetworkConfig(networkId: Long): VpnCommandResult<VirtualNetworkConfigDto>

    /**
     * 刷新节点状态与已连接网络列表。
     */
    suspend fun refreshState()
}
