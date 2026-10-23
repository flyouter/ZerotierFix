package net.kaaass.zerotierfix.features.networklist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import net.kaaass.zerotierfix.R
import net.kaaass.zerotierfix.model.dto.NetworkSummary
import net.kaaass.zerotierfix.model.dto.NodeStatusDto
import net.kaaass.zerotierfix.ui.common.EmptyState
import net.kaaass.zerotierfix.ui.common.InfoRow
import net.kaaass.zerotierfix.ui.theme.PrimaryGreen
import net.kaaass.zerotierfix.ui.theme.ZerotierFixTheme

/** 状态栏文本颜色，沿用原 `light_grey_text`。 */
private val StatusBarTextColor = Color(0xFFDDDDDD)

/**
 * 网络列表页面。
 *
 * @param state 页面状态
 * @param onAddNetwork 添加网络
 * @param onOpenNetwork 打开网络详情
 * @param onOpenSettings 打开设置
 * @param onOpenMoon 打开 Moon 入轨页
 * @param onOpenPeers 打开 Peer 列表
 * @param onNetworkEnabledChange 切换网络开关
 * @param onDeleteNetwork 删除网络
 * @param onCopyNetworkId 复制网络 ID
 * @param onNotificationAlertOpenSettings 打开通知设置
 * @param onNotificationAlertDontShowAgain 不再提示通知权限
 * @param onNotificationAlertDismiss 关闭通知权限提示
 * @param modifier 修饰符
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkListScreen(
    state: NetworkListUiState,
    onAddNetwork: () -> Unit,
    onOpenNetwork: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMoon: () -> Unit,
    onOpenPeers: () -> Unit,
    onNetworkEnabledChange: (NetworkSummary, Boolean) -> Unit,
    onDeleteNetwork: (NetworkSummary) -> Unit,
    onCopyNetworkId: (NetworkSummary) -> Unit,
    onNotificationAlertOpenSettings: () -> Unit,
    onNotificationAlertDontShowAgain: () -> Unit,
    onNotificationAlertDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_peers)) },
                            onClick = {
                                menuExpanded = false
                                onOpenPeers()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_orbit)) },
                            onClick = {
                                menuExpanded = false
                                onOpenMoon()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.settings)) },
                            onClick = {
                                menuExpanded = false
                                onOpenSettings()
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNetwork) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.menu_join_network)
                )
            }
        },
        bottomBar = { NodeStatusBar(state) }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (state.networks.isEmpty()) {
                EmptyState(stringResource(R.string.click_to_add_network))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(state.networks, key = { it.networkId }) { network ->
                        NetworkItem(
                            network = network,
                            isChecked = state.connectedNetworkId == network.networkId,
                            onCheckedChange = { onNetworkEnabledChange(network, it) },
                            onClick = { onOpenNetwork(network.networkId) },
                            onCopy = { onCopyNetworkId(network) },
                            onDelete = { onDeleteNetwork(network) }
                        )
                    }
                }
            }
        }
    }

    if (state.isNotificationAlertVisible) {
        AlertDialog(
            onDismissRequest = onNotificationAlertDismiss,
            title = { Text(stringResource(R.string.dialog_no_notification_alert_title)) },
            text = { Text(stringResource(R.string.dialog_no_notification_alert_text)) },
            confirmButton = {
                TextButton(onClick = onNotificationAlertOpenSettings) {
                    Text(stringResource(R.string.open_notification_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = onNotificationAlertDontShowAgain) {
                    Text(stringResource(R.string.dont_show_again))
                }
            }
        )
    }
}

/**
 * 单个网络条目。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NetworkItem(
    network: NetworkSummary,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 5.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuExpanded = true }
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                InfoRow(
                    label = stringResource(R.string.network_id),
                    value = network.networkIdStr ?: "",
                    monospace = true
                )
                InfoRow(
                    label = stringResource(R.string.network_name),
                    value = network.networkName?.takeIf { it.isNotEmpty() }
                        ?: stringResource(R.string.empty_network_name)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Switch(checked = isChecked, onCheckedChange = onCheckedChange)
                }
            }
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.copy_network_id_to_clipboard)) },
                onClick = {
                    menuExpanded = false
                    onCopy()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete_network)) },
                onClick = {
                    menuExpanded = false
                    onDelete()
                }
            )
        }
    }
}

/**
 * 底部节点状态栏。
 */
@Composable
private fun NodeStatusBar(state: NetworkListUiState) {
    Surface(color = PrimaryGreen, contentColor = StatusBarTextColor) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = state.nodeId ?: "", style = MaterialTheme.typography.bodySmall)
            Text(
                text = stringResource(
                    if (state.nodeStatus?.isOnline == true) {
                        R.string.status_online
                    } else {
                        R.string.status_offline
                    }
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = state.clientVersion ?: stringResource(R.string.unknown_version),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(R.string.app_version_format, state.appVersion),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NetworkListScreenPreview() {
    ZerotierFixTheme {
        NetworkListScreen(
            state = NetworkListUiState(
                networks = listOf(
                    NetworkSummary(0x0000000000000001L, "0000000000000001", "Home", true),
                    NetworkSummary(0x0000000000000002L, "0000000000000002", null, false)
                ),
                connectedNetworkId = 0x0000000000000001L,
                nodeId = "0123456789",
                nodeStatus = NodeStatusDto(true, 0x0123456789L, "1.16.2"),
                clientVersion = "1.16.2",
                isLoading = false,
                appVersion = "1.0.11"
            ),
            onAddNetwork = {},
            onOpenNetwork = {},
            onOpenSettings = {},
            onOpenMoon = {},
            onOpenPeers = {},
            onNetworkEnabledChange = { _, _ -> },
            onDeleteNetwork = {},
            onCopyNetworkId = {},
            onNotificationAlertOpenSettings = {},
            onNotificationAlertDontShowAgain = {},
            onNotificationAlertDismiss = {}
        )
    }
}
