package net.kaaass.zerotierfix.vpn.node

import com.zerotier.sdk.NodeStatus
import com.zerotier.sdk.Peer
import com.zerotier.sdk.PeerPhysicalPath
import com.zerotier.sdk.Version

import net.kaaass.zerotierfix.model.dto.NodeStatusDto
import net.kaaass.zerotierfix.model.dto.PeerInfoDto
import net.kaaass.zerotierfix.model.enums.PeerRole
import net.kaaass.zerotierfix.util.StringUtils

import java.util.ArrayList

/**
 * 将 ZeroTier SDK 的节点与 Peer 信息转换为跨层 DTO。
 */
object NodeInfoMapper {

    /**
     * 转换节点状态。
     *
     * @param status 节点状态，不能为空
     * @param version 客户端版本，不能为空
     * @return 节点状态 DTO
     */
    @JvmStatic
    fun toDto(status: NodeStatus, version: Version): NodeStatusDto = NodeStatusDto(
        status.isOnline,
        status.address,
        StringUtils.versionString(
            version.major, version.minor, version.revision
        )
    )

    /**
     * 转换 Peer 列表。
     *
     * @param peers SDK Peer 数组，可为 `null`
     * @return Peer DTO 列表；`peers` 为 `null` 时返回 `null`
     */
    @JvmStatic
    fun toDto(peers: Array<Peer>?): List<PeerInfoDto>? {
        if (peers == null) {
            return null
        }
        val result = ArrayList<PeerInfoDto>(peers.size)
        for (peer in peers) {
            result.add(
                PeerInfoDto(
                    peer.address,
                    toRole(peer.role),
                    peer.versionMajor,
                    peer.versionMinor,
                    peer.versionRev,
                    peer.latency,
                    preferredPath(peer)
                )
            )
        }
        return result
    }

    /**
     * 获取 Peer 首选物理路径。
     *
     * @param peer Peer 信息
     * @return 首选路径字符串；不存在时返回 `null`
     */
    private fun preferredPath(peer: Peer): String? {
        val paths = peer.paths ?: return null
        for (path in paths) {
            if (path.isPreferred) {
                return StringUtils.toString(path.address)
            }
        }
        return null
    }

    /**
     * 转换 Peer 角色。
     *
     * @param role SDK 角色
     * @return 领域角色
     */
    private fun toRole(role: com.zerotier.sdk.PeerRole): PeerRole = when (role) {
        com.zerotier.sdk.PeerRole.PEER_ROLE_LEAF -> PeerRole.LEAF
        com.zerotier.sdk.PeerRole.PEER_ROLE_MOON -> PeerRole.MOON
        com.zerotier.sdk.PeerRole.PEER_ROLE_PLANET -> PeerRole.PLANET
        else -> PeerRole.LEAF
    }
}
