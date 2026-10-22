package net.kaaass.zerotierfix.data.repository;

import net.kaaass.zerotierfix.model.dto.JoinNetworkRequest;
import net.kaaass.zerotierfix.model.dto.NetworkConfigDto;
import net.kaaass.zerotierfix.model.dto.NetworkSummary;

import java.util.List;

/**
 * 网络数据仓库。
 *
 * <p>方法为同步调用，内部通过 {@code DatabaseUtils} 的读写锁保护数据库访问；
 * 调用方负责选择合适的线程。返回值均为 DTO，不暴露 Room 实体。</p>
 */
public interface NetworkRepository {

    /**
     * 获取全部网络，按网络 ID 升序。
     *
     * @return 网络概要列表；无数据时返回空列表
     */
    List<NetworkSummary> getAllNetworks();

    /**
     * 获取指定网络的概要。
     *
     * @param networkId 网络 ID（十进制）
     * @return 网络概要；不存在时返回 {@code null}
     */
    NetworkSummary getNetworkSummary(long networkId);

    /**
     * 获取指定网络的本地配置。
     *
     * @param networkId 网络 ID（十进制）
     * @return 网络配置；网络或配置不存在时返回 {@code null}
     */
    NetworkConfigDto getNetworkConfig(long networkId);

    /**
     * 获取最近一次激活的网络 ID。
     *
     * @return 网络 ID；不存在或数据不一致时返回 {@code null}
     */
    Long getLastActivatedNetworkId();

    /**
     * 加入网络。
     *
     * <p>在同一事务内写入网络、网络配置和自定义 DNS。网络已存在时不写入。</p>
     *
     * @param request 加入请求，不能为空
     * @return 成功写入返回 {@code true}；网络已存在返回 {@code false}
     */
    boolean joinNetwork(JoinNetworkRequest request);

    /**
     * 将指定网络设为唯一激活网络。
     *
     * @param networkId 网络 ID（十进制）
     */
    void setLastActivated(long networkId);

    /**
     * 更新网络名称。
     *
     * @param networkId   网络 ID（十进制）
     * @param networkName 新名称；为空时忽略
     */
    void updateNetworkName(long networkId, String networkName);

    /**
     * 更新是否通过 ZeroTier 路由默认流量。
     *
     * @param networkId         网络 ID（十进制）
     * @param routeViaZeroTier 是否通过 ZeroTier 路由
     */
    void updateRouteViaZeroTier(long networkId, boolean routeViaZeroTier);

    /**
     * 删除网络及其配置、DNS 和地址记录。
     *
     * @param networkId 网络 ID（十进制）
     */
    void deleteNetwork(long networkId);
}
