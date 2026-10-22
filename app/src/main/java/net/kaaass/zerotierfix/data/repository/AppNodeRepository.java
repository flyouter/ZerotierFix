package net.kaaass.zerotierfix.data.repository;

/**
 * 本机 ZeroTier 节点信息仓库。
 */
public interface AppNodeRepository {

    /**
     * 获取本机节点 ID 的 16 进制字符串。
     *
     * @return 节点 ID 字符串；尚未初始化时返回 {@code null}
     */
    String getNodeIdStr();

    /**
     * 保存本机节点 ID。
     *
     * <p>已有记录时更新，否则插入。</p>
     *
     * @param nodeId 节点 ID（十进制）
     */
    void saveNodeId(long nodeId);
}
