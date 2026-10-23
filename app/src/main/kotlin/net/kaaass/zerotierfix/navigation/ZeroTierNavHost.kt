package net.kaaass.zerotierfix.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset
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

/** 页面转场时长（毫秒）。与 Material 3 短时长转场对齐。 */
private const val NAV_TRANSITION_DURATION_MS = 300

/**
 * 应用导航图。
 *
 * 转场使用 Material 3 的前进/返回滑动 + 淡入淡出，替代 Navigation Compose
 * 默认的 700ms 纯淡入淡出（两个页面长时间叠加，观感不自然）。
 *
 * @param navController 导航控制器
 */
@Composable
fun ZeroTierNavHost(navController: NavHostController = rememberNavController()) {
    val duration = tween<IntOffset>(NAV_TRANSITION_DURATION_MS)
    val fade = tween<Float>(NAV_TRANSITION_DURATION_MS)
    NavHost(
        navController = navController,
        startDestination = ZeroTierRoutes.NETWORK_LIST,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = duration
            ) + fadeIn(fade)
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = duration
            ) + fadeOut(fade)
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = duration
            ) + fadeIn(fade)
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = duration
            ) + fadeOut(fade)
        }
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
