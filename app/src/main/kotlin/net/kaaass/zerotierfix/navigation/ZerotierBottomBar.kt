package net.kaaass.zerotierfix.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * 应用底部导航栏。
 *
 * 只承载三个顶层页面；二级页面（加入网络、详情、Moon）不显示。
 *
 * @param selectedRoute 当前路由
 * @param onSelect 选择目的地回调
 * @param modifier 修饰符
 */
@Composable
fun ZerotierBottomBar(
    selectedRoute: String?,
    onSelect: (ZeroTierTopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        ZeroTierTopLevelDestination.entries.forEach { destination ->
            val selected = destination.route == selectedRoute
            NavigationBarItem(
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
                colors = NavigationBarItemDefaults.colors(
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
