package net.kaaass.zerotierfix.vpn.route

import net.kaaass.zerotierfix.util.InetAddressUtils

import java.net.InetAddress

/**
 * 路由记录数据类。
 */
data class Route @JvmOverloads constructor(
    /**
     * 路由网络地址。
     */
    val address: InetAddress,

    /**
     * 路由前缀长度。
     */
    val prefix: Int,

    /**
     * 下一跳地址；无网关时为 `null`。
     */
    var gateway: InetAddress? = null
) {

    /**
     * 判断地址是否属于本路由。
     *
     * @param inetAddress 待判断地址
     * @return 属于本路由返回 `true`
     */
    fun belongsToRoute(inetAddress: InetAddress): Boolean =
        this.address == InetAddressUtils.addressToRoute(inetAddress, this.prefix)
}
