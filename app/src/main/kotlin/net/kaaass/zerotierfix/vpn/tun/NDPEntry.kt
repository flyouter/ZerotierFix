package net.kaaass.zerotierfix.vpn.tun

import java.net.InetAddress

/**
 * NDP 表项。记录 MAC 与 IPv6 地址的对应关系及记录时间。
 */
class NDPEntry internal constructor(
    /**
     * MAC 地址。
     */
    val mac: Long,

    /**
     * IPv6 地址。
     */
    val address: InetAddress
) {
    /**
     * 最近一次更新时间（毫秒时间戳）。
     */
    var time: Long = 0L
        private set

    init {
        updateTime()
    }

    /**
     * 刷新表项时间。
     */
    fun updateTime() {
        this.time = System.currentTimeMillis()
    }
}
