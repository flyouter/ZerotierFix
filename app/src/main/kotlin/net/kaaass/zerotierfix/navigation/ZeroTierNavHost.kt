package net.kaaass.zerotierfix.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

import net.kaaass.zerotierfix.features.detail.NetworkDetailRoute
import net.kaaass.zerotierfix.features.join.JoinNetworkRoute
import net.kaaass.zerotierfix.features.join.NetworkScanRoute
import net.kaaass.zerotierfix.features.moon.MoonOrbitRoute
import net.kaaass.zerotierfix.features.networklist.NetworkListRoute
import net.kaaass.zerotierfix.features.peer.PeerListRoute
import net.kaaass.zerotierfix.features.settings.SettingsRoute

/** 二级页面转场时长（毫秒）。 */
private const val NAV_TRANSITION_DURATION_MS = 300

/** 顶层 Tab 切换的淡入时长（毫秒）。 */
private const val TOP_LEVEL_ENTER_MS = 220

/** 顶层 Tab 切换的淡出时长（毫秒）。 */
private const val TOP_LEVEL_EXIT_MS = 160

/**
 * 应用导航图。
 *
 * 紧凑屏幕使用底部栏，宽屏（横屏 / 平板）使用左侧导航栏；两者都与 NavHost 平级，
 * 避免嵌套 Scaffold。二级页面自动隐藏导航栏。
 *
 * @param navController 导航控制器
 */
@Composable
fun ZeroTierNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = ZeroTierTopLevelDestination.isTopLevel(currentRoute)

    val slideDuration = remember { tween<IntOffset>(NAV_TRANSITION_DURATION_MS) }
    val slideFade = remember { tween<Float>(NAV_TRANSITION_DURATION_MS) }
    val topEnter = remember { tween<Float>(TOP_LEVEL_ENTER_MS) }
    val topExit = remember { tween<Float>(TOP_LEVEL_EXIT_MS) }

    val onSelectTopLevel: (ZeroTierTopLevelDestination) -> Unit = { destination ->
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    // 使用 Configuration 宽度，避免 BoxWithConstraints 在首帧引入额外的 SubcomposeLayout。
    val useRail = LocalConfiguration.current.screenWidthDp >= WIDE_LAYOUT_MIN_WIDTH_DP
    Row(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = isTopLevel && useRail,
            enter = fadeIn(tween(160)) + expandHorizontally(tween(180)),
            exit = fadeOut(tween(120)) + shrinkHorizontally(tween(160))
        ) {
            ZerotierNavigationRail(
                selectedRoute = currentRoute,
                onSelect = onSelectTopLevel
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.weight(1f)) {
                NavHost(
                    navController = navController,
                    startDestination = ZeroTierRoutes.NETWORK_LIST,
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = {
                        if (isTopLevelTransition(initialState, targetState)) {
                            fadeIn(topEnter)
                        } else {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = slideDuration
                            ) + fadeIn(slideFade)
                        }
                    },
                    exitTransition = {
                        if (isTopLevelTransition(initialState, targetState)) {
                            fadeOut(topExit)
                        } else {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = slideDuration
                            ) + fadeOut(slideFade)
                        }
                    },
                    popEnterTransition = {
                        if (isTopLevelTransition(initialState, targetState)) {
                            fadeIn(topEnter)
                        } else {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = slideDuration
                            ) + fadeIn(slideFade)
                        }
                    },
                    popExitTransition = {
                        if (isTopLevelTransition(initialState, targetState)) {
                            fadeOut(topExit)
                        } else {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = slideDuration
                            ) + fadeOut(slideFade)
                        }
                    }
                ) {
                    composable(ZeroTierRoutes.NETWORK_LIST) {
                        NetworkListRoute(
                            onAddNetwork = { navController.navigate(ZeroTierRoutes.JOIN) },
                            onOpenNetwork = { networkId ->
                                navController.navigate(ZeroTierRoutes.detail(networkId))
                            },
                            onOpenMoon = { navController.navigate(ZeroTierRoutes.MOON) }
                        )
                    }
                    composable(ZeroTierRoutes.JOIN) { entry ->
                        JoinNetworkRoute(
                            onBack = { navController.popBackStack() },
                            onScanRequested = { navController.navigate(ZeroTierRoutes.SCAN) },
                            savedStateHandle = entry.savedStateHandle
                        )
                    }
                    composable(
                        route = ZeroTierRoutes.SCAN,
                        enterTransition = { fadeIn(tween(150)) },
                        popExitTransition = { fadeOut(tween(150)) }
                    ) {
                        NetworkScanRoute(
                            onBack = { navController.popBackStack() },
                            onScanned = { networkId ->
                                navController.previousBackStackEntry
                                    ?.takeIf { it.destination.route == ZeroTierRoutes.JOIN }
                                    ?.savedStateHandle
                                    ?.set(ZeroTierRoutes.SCAN_RESULT_KEY, networkId)
                                navController.popBackStack()
                            }
                        )
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
                        PeerListRoute()
                    }
                    composable(ZeroTierRoutes.SETTINGS) {
                        SettingsRoute()
                    }
                }
            }
            AnimatedVisibility(
                visible = isTopLevel && !useRail,
                enter = fadeIn(tween(160)) + expandVertically(
                    animationSpec = tween(180),
                    expandFrom = Alignment.Top
                ),
                exit = fadeOut(tween(120)) + shrinkVertically(
                    animationSpec = tween(160),
                    shrinkTowards = Alignment.Top
                )
            ) {
                ZerotierBottomBar(
                    selectedRoute = currentRoute,
                    onSelect = onSelectTopLevel
                )
            }
        }
    }
}

/**
 * 判断转场双方是否都是顶层页面。
 */
private fun isTopLevelTransition(
    initialState: NavBackStackEntry,
    targetState: NavBackStackEntry
): Boolean = ZeroTierTopLevelDestination.isTopLevel(initialState.destination.route) &&
    ZeroTierTopLevelDestination.isTopLevel(targetState.destination.route)
