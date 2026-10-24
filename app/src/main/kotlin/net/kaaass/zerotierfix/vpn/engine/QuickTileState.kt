package net.kaaass.zerotierfix.vpn.engine

import net.kaaass.zerotierfix.model.dto.NetworkSummary

/**
 * 磁贴当前正在执行的操作。
 */
enum class QuickTileOperation {

    /** 没有进行中的操作。 */
    IDLE,

    /** 正在连接网络。 */
    CONNECTING,

    /** 正在断开网络。 */
    DISCONNECTING
}

/**
 * 磁贴的逻辑状态，由控制器计算后交给 [net.kaaass.zerotierfix.service.ZeroTierTileService] 渲染。
 */
enum class QuickTileStatus {

    /** 没有任何已添加网络，磁贴不可用。 */
    UNAVAILABLE,

    /** 未连接。 */
    INACTIVE,

    /** 正在连接。 */
    CONNECTING,

    /** 已连接。 */
    ACTIVE
}

/**
 * 磁贴状态快照。
 *
 * @property networksLoaded 网络列表是否已从数据库加载完成
 * @property networkCount 已添加的网络数量
 * @property targetNetworkId 点击磁贴时将要连接的网络 ID；没有网络时为 `null`
 * @property status 磁贴逻辑状态
 */
data class QuickTileState(
    val networksLoaded: Boolean = false,
    val networkCount: Int = 0,
    val targetNetworkId: Long? = null,
    val status: QuickTileStatus = QuickTileStatus.INACTIVE
)

/**
 * 选择磁贴点击时应连接的网络。
 *
 * 规则：
 * - 没有网络：返回 `null`
 * - 恰好一个网络：返回该网络，忽略上次连接记录
 * - 多个网络：优先上次连接的网络；记录缺失、已删除或不在列表时返回列表第一个
 *
 * [networks] 的顺序沿用 [net.kaaass.zerotierfix.data.repository.NetworkRepository.getAllNetworks]
 * 的 `_id ASC` 顺序。
 *
 * @param networks 已添加的网络列表
 * @param lastActivatedNetworkId 上次连接的网络 ID
 * @return 目标网络 ID；没有网络时返回 `null`
 */
fun selectQuickTileTarget(
    networks: List<NetworkSummary>,
    lastActivatedNetworkId: Long?
): Long? {
    if (networks.isEmpty()) {
        return null
    }
    if (networks.size == 1) {
        return networks.first().networkId
    }
    if (lastActivatedNetworkId != null) {
        networks.firstOrNull { it.networkId == lastActivatedNetworkId }?.let {
            return it.networkId
        }
    }
    return networks.first().networkId
}

/**
 * 根据网络列表、服务状态与进行中的操作计算磁贴逻辑状态。
 *
 * [QuickTileOperation.CONNECTING] 与 [QuickTileOperation.DISCONNECTING] 优先于服务状态，
 * 使磁贴在点击后立即反馈，而不是等待服务状态变化。
 *
 * 服务仍在运行或启动中时，即使数据库中没有网络也返回可操作状态，避免用户
 * 无法关闭残留的 VPN 连接。
 *
 * @param networksLoaded 网络列表是否已加载
 * @param networkCount 已添加的网络数量
 * @param serviceState 当前服务状态
 * @param operation 进行中的操作
 * @return 磁贴逻辑状态
 */
fun mapQuickTileStatus(
    networksLoaded: Boolean,
    networkCount: Int,
    serviceState: ServiceState,
    operation: QuickTileOperation
): QuickTileStatus {
    if (!networksLoaded) {
        return QuickTileStatus.INACTIVE
    }
    when (operation) {
        QuickTileOperation.CONNECTING -> return QuickTileStatus.CONNECTING
        QuickTileOperation.DISCONNECTING -> return QuickTileStatus.INACTIVE
        QuickTileOperation.IDLE -> Unit
    }

    val serviceActive = serviceState == ServiceState.RUNNING ||
        serviceState == ServiceState.STARTING
    if (networkCount == 0 && !serviceActive) {
        return QuickTileStatus.UNAVAILABLE
    }

    return when (serviceState) {
        ServiceState.STARTING -> QuickTileStatus.CONNECTING
        ServiceState.RUNNING -> QuickTileStatus.ACTIVE
        ServiceState.STOPPED, ServiceState.UNKNOWN -> QuickTileStatus.INACTIVE
    }
}
