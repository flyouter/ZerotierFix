package net.kaaass.zerotierfix.vpn.engine

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

import net.kaaass.zerotierfix.model.dto.NodeStatusDto
import net.kaaass.zerotierfix.model.dto.VirtualNetworkConfigDto

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 进程级 VPN 状态持有者。
 *
 * 由 [net.kaaass.zerotierfix.service.ZeroTierOneService] 写入，UI 与引擎门面读取。
 * 所有方法均可在任意线程调用；StateFlow / SharedFlow 自带同步。
 *
 * `connectedNetworks` 与 `virtualNetworkConfigs` 使用 `null` 表示“尚未加载”，
 * 以便区分“服务没有应答”和“确实没有网络/配置”。
 */
@Singleton
class VpnStateStore @Inject constructor() {

    private val _serviceState = MutableStateFlow(ServiceState.UNKNOWN)

    /** 服务生命周期状态。 */
    val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()

    private val _binderState = MutableStateFlow(BinderState.DISCONNECTED)

    /** Binder 连接状态。 */
    val binderState: StateFlow<BinderState> = _binderState.asStateFlow()

    private val _nodeId = MutableStateFlow<String?>(null)

    /** 本机节点 ID 字符串；未知时为 `null`。 */
    val nodeId: StateFlow<String?> = _nodeId.asStateFlow()

    private val _nodeStatus = MutableStateFlow<NodeStatusDto?>(null)

    /** 节点在线状态；服务未运行或尚未初始化时为 `null`。 */
    val nodeStatus: StateFlow<NodeStatusDto?> = _nodeStatus.asStateFlow()

    private val _connectedNetworks = MutableStateFlow<List<VirtualNetworkConfigDto>?>(null)

    /**
     * 服务当前连接的网络配置列表。
     *
     * `null` 表示尚未从服务取得数据；空列表不会由服务写入。
     */
    val connectedNetworks: StateFlow<List<VirtualNetworkConfigDto>?> =
        _connectedNetworks.asStateFlow()

    private val _virtualNetworkConfigs = MutableStateFlow<Map<Long, VirtualNetworkConfigDto>?>(null)

    /** 各网络的最新实时配置；`null` 表示尚未加载。 */
    val virtualNetworkConfigs: StateFlow<Map<Long, VirtualNetworkConfigDto>?> =
        _virtualNetworkConfigs.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 8)

    /**
     * VPN 错误消息。
     *
     * 无订阅者时丢弃，与原 EventBus 的页面级订阅行为一致。
     */
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    private val _statusNotices =
        MutableSharedFlow<VirtualNetworkConfigDto>(extraBufferCapacity = 8)

    /**
     * 网络状态提示。
     *
     * 仅在原 `VirtualNetworkConfigChangedEvent` 的发布条件成立时发射，
     * 用于保持列表页 Toast 的触发频率不变。
     */
    val statusNotices: SharedFlow<VirtualNetworkConfigDto> = _statusNotices.asSharedFlow()

    /**
     * 更新服务状态。
     *
     * @param state 新状态
     */
    fun setServiceState(state: ServiceState) {
        _serviceState.value = state
    }

    /**
     * 仅当当前状态为 [expected] 时原子更新为 [next]。
     *
     * 服务在主线程写状态，命令层在单并发调度器写状态，二者不在同一串行域；
     * 条件归一化必须使用原子比较，避免覆盖并发写入的 `STARTING/RUNNING`。
     *
     * @return 是否完成替换
     */
    fun compareAndSetServiceState(
        expected: ServiceState,
        next: ServiceState
    ): Boolean = _serviceState.compareAndSet(expected, next)

    /**
     * 更新 Binder 状态。
     *
     * @param state 新状态
     */
    fun setBinderState(state: BinderState) {
        _binderState.value = state
    }

    /**
     * 更新本机节点 ID。
     *
     * @param nodeId 节点 ID 字符串；未知时传 `null`
     */
    fun setNodeId(nodeId: String?) {
        _nodeId.value = nodeId
    }

    /**
     * 更新节点状态。
     *
     * @param status 节点状态；服务停止或未初始化时传 `null`
     */
    fun setNodeStatus(status: NodeStatusDto?) {
        _nodeStatus.value = status
    }

    /**
     * 更新已连接网络列表。
     *
     * @param networks 网络配置列表；传 `null` 表示尚未加载
     */
    fun setConnectedNetworks(networks: List<VirtualNetworkConfigDto>?) {
        _connectedNetworks.value = networks
    }

    /**
     * 写入指定网络的实时配置。
     *
     * @param networkId 网络 ID
     * @param config 网络配置
     */
    fun setVirtualNetworkConfig(networkId: Long, config: VirtualNetworkConfigDto) {
        _virtualNetworkConfigs.update { current ->
            (current ?: emptyMap()) + (networkId to config)
        }
    }

    /**
     * 移除指定网络的实时配置。
     *
     * @param networkId 网络 ID
     */
    fun removeVirtualNetworkConfig(networkId: Long) {
        _virtualNetworkConfigs.update { current -> current?.minus(networkId) }
    }

    /**
     * 清空所有网络的实时配置，恢复到“尚未加载”。
     *
     * 服务停止后这些配置不再有效；不清空会让只读缓存的页面展示过期的
     * IP、DNS 与在线状态。
     */
    fun clearVirtualNetworkConfigs() {
        _virtualNetworkConfigs.value = null
    }

    /**
     * 发出 VPN 错误消息。
     *
     * @param message 错误消息
     */
    fun emitError(message: String) {
        _errors.tryEmit(message)
    }

    /**
     * 发出网络状态提示。
     *
     * @param config 发生变化的网络配置
     */
    fun emitStatusNotice(config: VirtualNetworkConfigDto) {
        _statusNotices.tryEmit(config)
    }
}
