package net.kaaass.zerotierfix.data.repository

import net.kaaass.zerotierfix.model.dto.JoinNetworkRequest
import net.kaaass.zerotierfix.model.dto.NetworkConfigDto
import net.kaaass.zerotierfix.model.dto.NetworkSummary

/**
 * 网络数据仓库。
 *
 * 方法为同步调用，内部通过 `DatabaseUtils` 的读写锁保护数据库访问；
 * 调用方负责选择合适的线程。返回值均为 DTO，不暴露 Room 实体。
 */
interface NetworkRepository {

    /**
     * 获取全部网络，按网络 ID 升序。
     *
     * @return 网络概要列表；无数据时返回空列表
     */
    fun getAllNetworks(): List<NetworkSummary>

    /**
     * 获取指定网络的概要。
     *
     * @param networkId 网络 ID（十进制）
     * @return 网络概要；不存在时返回 `null`
     */
    fun getNetworkSummary(networkId: Long): NetworkSummary?

    /**
     * 获取指定网络的本地配置。
     *
     * @param networkId 网络 ID（十进制）
     * @return 网络配置；网络或配置不存在时返回 `null`
     */
    fun getNetworkConfig(networkId: Long): NetworkConfigDto?

    /**
     * 获取最近一次激活的网络 ID。
     *
     * @return 网络 ID；不存在或数据不一致时返回 `null`
     */
    fun getLastActivatedNetworkId(): Long?

    /**
     * 加入网络。
     *
     * 在同一事务内写入网络、网络配置和自定义 DNS。网络已存在时不写入。
     *
     * @param request 加入请求，不能为空
     * @return 成功写入返回 `true`；网络已存在返回 `false`
     */
    fun joinNetwork(request: JoinNetworkRequest): Boolean

    /**
     * 将指定网络设为唯一激活网络。
     *
     * @param networkId 网络 ID（十进制）
     */
    fun setLastActivated(networkId: Long)

    /**
     * 更新网络名称。
     *
     * @param networkId 网络 ID（十进制）
     * @param networkName 新名称；为空时忽略
     */
    fun updateNetworkName(networkId: Long, networkName: String?)

    /**
     * 更新是否通过 ZeroTier 路由默认流量。
     *
     * @param networkId 网络 ID（十进制）
     * @param routeViaZeroTier 是否通过 ZeroTier 路由
     */
    fun updateRouteViaZeroTier(networkId: Long, routeViaZeroTier: Boolean)

    /**
     * 删除网络及其配置、DNS 和地址记录。
     *
     * @param networkId 网络 ID（十进制）
     */
    fun deleteNetwork(networkId: Long)
}
