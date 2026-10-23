package net.kaaass.zerotierfix.data.repository

/**
 * 本机 ZeroTier 节点信息仓库。
 */
interface AppNodeRepository {

    /**
     * 获取本机节点 ID 的 16 进制字符串。
     *
     * @return 节点 ID 字符串；尚未初始化时返回 `null`
     */
    fun getNodeIdStr(): String?

    /**
     * 保存本机节点 ID。
     *
     * 已有记录时更新，否则插入。
     *
     * @param nodeId 节点 ID（十进制）
     */
    fun saveNodeId(nodeId: Long)
}
