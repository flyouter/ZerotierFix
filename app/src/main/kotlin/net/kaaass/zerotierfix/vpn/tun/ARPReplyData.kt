package net.kaaass.zerotierfix.vpn.tun

import java.net.InetAddress

/**
 * ARP 应答报文的所需数据。由于报文内容总是当前节点的 IP 与 MAC，因此仅记录应答报文目标的信息。
 */
data class ARPReplyData(
    /**
     * 应答目标 MAC 地址。
     */
    val destMac: Long,

    /**
     * 应答目标 IPv4 地址；解析失败时为 `null`。
     */
    val destAddress: InetAddress?
)
