package net.kaaass.zerotierfix.navigation

/**
 * 应用内导航路由。
 */
object ZeroTierRoutes {

    /** 网络列表（起始页）。 */
    const val NETWORK_LIST = "network_list"

    /** 添加网络页。 */
    const val JOIN = "join"

    /** 网络详情页。 */
    const val DETAIL = "detail/{networkId}"

    /** 网络详情页路径参数名。 */
    const val DETAIL_ARG_NETWORK_ID = "networkId"

    /** Moon 入轨页。 */
    const val MOON = "moon"

    /** Peer 列表页。 */
    const val PEER = "peer"

    /** 设置页。 */
    const val SETTINGS = "settings"

    /**
     * 构造网络详情页路由。
     *
     * @param networkId 网络 ID
     * @return 路由字符串
     */
    fun detail(networkId: Long): String = "detail/$networkId"
}
