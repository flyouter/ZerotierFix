package net.kaaass.zerotierfix.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

import net.kaaass.zerotierfix.R

/**
 * 底部导航的顶层目的地。
 *
 * @property route 导航路由
 * @property labelRes 标签资源
 * @property icon 图标
 */
enum class ZeroTierTopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    /** 网络列表。 */
    NETWORKS(ZeroTierRoutes.NETWORK_LIST, R.string.tab_networks, Icons.Outlined.Home),

    /** Peer 列表。 */
    PEERS(ZeroTierRoutes.PEER, R.string.tab_peers, Icons.Outlined.Person),

    /** 设置。 */
    SETTINGS(ZeroTierRoutes.SETTINGS, R.string.tab_settings, Icons.Outlined.Settings);

    companion object {
        /**
         * 判断路由是否为顶层页面。
         *
         * @param route 当前路由；`null` 表示尚未确定
         * @return 是顶层页面返回 `true`
         */
        fun isTopLevel(route: String?): Boolean = entries.any { it.route == route }
    }
}
