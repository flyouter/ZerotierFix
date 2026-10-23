package net.kaaass.zerotierfix.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

import net.kaaass.zerotierfix.features.detail.NetworkDetailRoute
import net.kaaass.zerotierfix.features.join.JoinNetworkRoute
import net.kaaass.zerotierfix.features.moon.MoonOrbitRoute
import net.kaaass.zerotierfix.features.networklist.NetworkListRoute
import net.kaaass.zerotierfix.features.peer.PeerListRoute
import net.kaaass.zerotierfix.features.settings.SettingsRoute

/**
 * 应用导航图。
 *
 * @param navController 导航控制器
 */
@Composable
fun ZeroTierNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = ZeroTierRoutes.NETWORK_LIST
    ) {
        composable(ZeroTierRoutes.NETWORK_LIST) {
            NetworkListRoute(
                onAddNetwork = { navController.navigate(ZeroTierRoutes.JOIN) },
                onOpenNetwork = { networkId ->
                    navController.navigate(ZeroTierRoutes.detail(networkId))
                },
                onOpenSettings = { navController.navigate(ZeroTierRoutes.SETTINGS) },
                onOpenMoon = { navController.navigate(ZeroTierRoutes.MOON) },
                onOpenPeers = { navController.navigate(ZeroTierRoutes.PEER) }
            )
        }
        composable(ZeroTierRoutes.JOIN) {
            JoinNetworkRoute(onBack = { navController.popBackStack() })
        }
        composable(
            route = ZeroTierRoutes.DETAIL,
            arguments = listOf(
                navArgument(ZeroTierRoutes.DETAIL_ARG_NETWORK_ID) {
                    type = NavType.LongType
                }
            )
        ) {
            NetworkDetailRoute(onBack = { navController.popBackStack() })
        }
        composable(ZeroTierRoutes.MOON) {
            MoonOrbitRoute(onBack = { navController.popBackStack() })
        }
        composable(ZeroTierRoutes.PEER) {
            PeerListRoute(onBack = { navController.popBackStack() })
        }
        composable(ZeroTierRoutes.SETTINGS) {
            SettingsRoute(onBack = { navController.popBackStack() })
        }
    }
}
