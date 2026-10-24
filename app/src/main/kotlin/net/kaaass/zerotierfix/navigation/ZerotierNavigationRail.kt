package net.kaaass.zerotierfix.navigation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * 宽屏下的左侧导航栏。
 *
 * 只承载三个顶层页面；与底部栏共享目的地与选中逻辑。
 *
 * @param selectedRoute 当前路由
 * @param onSelect 选择目的地回调
 * @param modifier 修饰符
 */
@Composable
fun ZerotierNavigationRail(
    selectedRoute: String?,
    onSelect: (ZeroTierTopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        ZeroTierTopLevelDestination.entries.forEach { destination ->
            val selected = destination.route == selectedRoute
            NavigationRailItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        onSelect(destination)
                    }
                },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(destination.labelRes)) },
                alwaysShowLabel = true,
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
