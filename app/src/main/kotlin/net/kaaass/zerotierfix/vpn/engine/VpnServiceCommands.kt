package net.kaaass.zerotierfix.vpn.engine

import net.kaaass.zerotierfix.model.dto.MoonOrbitDto
import net.kaaass.zerotierfix.model.dto.PeerInfoDto
import net.kaaass.zerotierfix.model.dto.VirtualNetworkConfigDto

/**
 * 对 ZeroTier 服务的命令接口。
 *
 * 接口定义在引擎层，由 `service/AndroidVpnServiceCommands` 实现，避免
 * `vpn` 包直接依赖 `service` 类型。所有 `suspend` 方法均在单一后台调度器上
 * 串行执行，调用方无需自行加锁。
 */
interface VpnServiceCommands {

    /**
     * 探测服务是否正在运行，并把结果写入 [VpnStateStore]。
     */
    suspend fun probe()

    /**
     * 启动服务并指定要连接的网络。
     *
     * @param networkId 网络 ID
     * @return 启动请求是否成功发出
     */
    suspend fun startService(networkId: Long): Boolean

    /**
     * 停止服务、解除绑定并释放资源。
     */
    suspend fun stop()

    /**
     * 加入指定网络。
     *
     * @param networkId 网络 ID
     * @return 执行结果
     */
    suspend fun join(networkId: Long): VpnCommandResult<Unit>

    /**
     * 离开指定网络。
     *
     * @param networkId 网络 ID
     * @return 执行结果
     */
    suspend fun leave(networkId: Long): VpnCommandResult<Unit>

    /**
     * 入轨指定 Moon 列表。
     *
     * @param moons Moon 信息列表
     */
    suspend fun orbit(moons: List<MoonOrbitDto>)

    /**
     * 获取 Peer 列表。
     *
     * @return 服务未运行、节点未就绪或 Peer 列表
     */
    suspend fun loadPeers(): VpnCommandResult<List<PeerInfoDto>>

    /**
     * 请求服务重新配置指定网络的隧道。
     *
     * @param networkId 网络 ID
     */
    suspend fun reconfigure(networkId: Long)

    /**
     * 获取指定网络的实时配置快照。
     *
     * @param networkId 网络 ID
     * @return 服务未运行、节点未就绪或配置
     */
    suspend fun virtualNetworkConfig(networkId: Long): VpnCommandResult<VirtualNetworkConfigDto>

    /**
     * 刷新节点状态与已连接网络列表，等价于原 `onResume` 的请求。
     */
    suspend fun refreshState()
}
