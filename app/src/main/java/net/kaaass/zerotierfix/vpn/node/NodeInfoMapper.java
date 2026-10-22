package net.kaaass.zerotierfix.vpn.node;

import com.zerotier.sdk.NodeStatus;
import com.zerotier.sdk.Peer;
import com.zerotier.sdk.PeerPhysicalPath;
import com.zerotier.sdk.Version;

import net.kaaass.zerotierfix.model.dto.NodeStatusDto;
import net.kaaass.zerotierfix.model.dto.PeerInfoDto;
import net.kaaass.zerotierfix.model.enums.PeerRole;
import net.kaaass.zerotierfix.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 将 ZeroTier SDK 的节点与 Peer 信息转换为跨层 DTO。
 */
public final class NodeInfoMapper {

    private NodeInfoMapper() {
    }

    /**
     * 转换节点状态。
     *
     * @param status  节点状态，不能为空
     * @param version 客户端版本，不能为空
     * @return 节点状态 DTO
     */
    public static NodeStatusDto toDto(NodeStatus status, Version version) {
        return new NodeStatusDto(
                status.isOnline(),
                status.getAddress(),
                StringUtils.versionString(
                        version.getMajor(), version.getMinor(), version.getRevision())
        );
    }

    /**
     * 转换 Peer 列表。
     *
     * @param peers SDK Peer 数组，可为 {@code null}
     * @return Peer DTO 列表；{@code peers} 为 {@code null} 时返回 {@code null}
     */
    public static List<PeerInfoDto> toDto(Peer[] peers) {
        if (peers == null) {
            return null;
        }
        List<PeerInfoDto> result = new ArrayList<>(peers.length);
        for (Peer peer : peers) {
            result.add(new PeerInfoDto(
                    peer.getAddress(),
                    toRole(peer.getRole()),
                    peer.getVersionMajor(),
                    peer.getVersionMinor(),
                    peer.getVersionRev(),
                    peer.getLatency(),
                    preferredPath(peer)
            ));
        }
        return result;
    }

    /**
     * 获取 Peer 首选物理路径。
     *
     * @param peer Peer 信息
     * @return 首选路径字符串；不存在时返回 {@code null}
     */
    private static String preferredPath(Peer peer) {
        if (peer.getPaths() == null) {
            return null;
        }
        for (PeerPhysicalPath path : peer.getPaths()) {
            if (path.isPreferred()) {
                return StringUtils.toString(path.getAddress());
            }
        }
        return null;
    }

    /**
     * 转换 Peer 角色。
     *
     * @param role SDK 角色
     * @return 领域角色
     */
    private static PeerRole toRole(com.zerotier.sdk.PeerRole role) {
        switch (role) {
            case PEER_ROLE_LEAF:
                return PeerRole.LEAF;
            case PEER_ROLE_MOON:
                return PeerRole.MOON;
            case PEER_ROLE_PLANET:
                return PeerRole.PLANET;
            default:
                return PeerRole.LEAF;
        }
    }
}
