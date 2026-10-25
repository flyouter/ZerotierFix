package net.kaaass.zerotierfix.features.networklist

import android.content.res.Configuration

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import net.kaaass.zerotierfix.R
import net.kaaass.zerotierfix.model.dto.NetworkSummary
import net.kaaass.zerotierfix.model.dto.NodeStatusDto
import net.kaaass.zerotierfix.model.enums.NetworkStatus
import net.kaaass.zerotierfix.navigation.WIDE_LAYOUT_MIN_WIDTH_DP
import net.kaaass.zerotierfix.ui.common.ConfirmDialog
import net.kaaass.zerotierfix.ui.common.EmptyState
import net.kaaass.zerotierfix.ui.common.EnumTextFormatter
import net.kaaass.zerotierfix.ui.common.InfoRow
import net.kaaass.zerotierfix.ui.common.LoadingState
import net.kaaass.zerotierfix.ui.common.MonoText
import net.kaaass.zerotierfix.ui.common.SectionHeader
import net.kaaass.zerotierfix.ui.common.StatusPill
import net.kaaass.zerotierfix.ui.common.StatusTone
import net.kaaass.zerotierfix.ui.common.ZerotierTopBar
import net.kaaass.zerotierfix.ui.common.zerotierSwitchColors
import net.kaaass.zerotierfix.ui.icons.ZeroTierIcons
import net.kaaass.zerotierfix.ui.theme.ZerotierFixTheme
import net.kaaass.zerotierfix.vpn.engine.ServiceState

/**
 * 网络列表页面。
 *
 * @param state 页面状态
 * @param onAddNetwork 添加网络
 * @param onOpenNetwork 打开网络详情
 * @param onOpenMoon 打开 Moon 入轨页
 * @param onNetworkEnabledChange 切换网络开关
 * @param onDeleteNetwork 删除网络
 * @param onCopyNetworkId 复制网络 ID
 * @param onCopyNodeId 复制本机节点 ID
 * @param onNotificationAlertOpenSettings 打开通知设置
 * @param onNotificationAlertDontShowAgain 不再提示通知权限
 * @param onNotificationAlertDismiss 关闭通知权限提示
 * @param modifier 修饰符
 */
@Composable
fun NetworkListScreen(
    state: NetworkListUiState,
    onAddNetwork: () -> Unit,
    onOpenNetwork: (Long) -> Unit,
    onOpenMoon: () -> Unit,
    onNetworkEnabledChange: (NetworkSummary, Boolean) -> Unit,
    onDeleteNetwork: (NetworkSummary) -> Unit,
    onCopyNetworkId: (NetworkSummary) -> Unit,
    onCopyNodeId: (String) -> Unit,
    onNotificationAlertOpenSettings: () -> Unit,
    onNotificationAlertDontShowAgain: () -> Unit,
    onNotificationAlertDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    val pendingDelete = pendingDeleteId?.let { id ->
        state.networks.firstOrNull { it.summary.networkId == id }?.summary
    }
    // 宽屏下底栏变为左侧导航栏，改用顶栏入口，避免 FAB 遮挡列表内容。
    val isWideLayout = LocalConfiguration.current.screenWidthDp >= WIDE_LAYOUT_MIN_WIDTH_DP

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ZerotierTopBar(
                title = stringResource(R.string.app_name),
                actions = {
                    if (isWideLayout && state.networks.isNotEmpty()) {
                        IconButton(onClick = onAddNetwork) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.join_network)
                            )
                        }
                    }
                    IconButton(onClick = onOpenMoon) {
                        Icon(
                            imageVector = ZeroTierIcons.Moon,
                            contentDescription = stringResource(R.string.moon_orbit_activity)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isWideLayout && state.networks.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onAddNetwork,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.join_network)) }
                )
            }
        }
    ) { padding ->
        when {
            state.isLoading && state.networks.isEmpty() -> {
                LoadingState(
                    text = stringResource(R.string.loading),
                    modifier = Modifier.padding(padding)
                )
            }
            state.networks.isEmpty() -> {
                EmptyState(
                    icon = Icons.Outlined.Home,
                    title = stringResource(R.string.network_empty_title),
                    description = stringResource(R.string.network_empty_message),
                    actionLabel = stringResource(R.string.join_network),
                    onAction = onAddNetwork,
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "node_status") {
                        NodeStatusCard(
                            state = state,
                            onCopyNodeId = onCopyNodeId
                        )
                    }
                    item(key = "networks_header") {
                        SectionHeader(title = stringResource(R.string.tab_networks))
                    }
                    items(
                        items = state.networks,
                        key = { it.summary.networkId }
                    ) { item ->
                        NetworkCard(
                            item = item,
                            connectedNetworkId = state.connectedNetworkId,
                            operatingNetworkId = state.operatingNetworkId,
                            onCheckedChange = { enabled ->
                                onNetworkEnabledChange(item.summary, enabled)
                            },
                            onClick = { onOpenNetwork(item.summary.networkId) },
                            onCopy = { onCopyNetworkId(item.summary) },
                            onDelete = { pendingDeleteId = item.summary.networkId }
                        )
                    }
                }
            }
        }
    }

    if (pendingDelete != null) {
        val name = pendingDelete.networkName?.takeIf { it.isNotEmpty() }
            ?: stringResource(R.string.network_unnamed)
        ConfirmDialog(
            title = stringResource(R.string.network_delete_confirm_title),
            text = stringResource(R.string.network_delete_confirm_message, name),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                onDeleteNetwork(pendingDelete)
                pendingDeleteId = null
            },
            onDismiss = { pendingDeleteId = null }
        )
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
 * 节点状态卡：在线状态、节点 ID、客户端版本与应用版本。
 */
@Composable
private fun NodeStatusCard(
    state: NetworkListUiState,
    onCopyNodeId: (String) -> Unit
) {
    val statusLabel: String
    val statusTone: StatusTone
    when (state.serviceState) {
        ServiceState.STARTING -> {
            statusLabel = stringResource(R.string.network_status_connecting)
            statusTone = StatusTone.Connecting
        }
        ServiceState.RUNNING -> {
            val online = state.nodeStatus?.isOnline == true
            statusLabel = stringResource(
                if (online) R.string.status_online else R.string.status_offline
            )
            statusTone = if (online) StatusTone.Connected else StatusTone.Offline
        }
        ServiceState.STOPPED, ServiceState.UNKNOWN -> {
            statusLabel = stringResource(R.string.network_service_stopped)
            statusTone = StatusTone.Offline
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(label = statusLabel, tone = statusTone)
                Spacer(modifier = Modifier.weight(1f))
                state.clientVersion?.let { version ->
                    Text(
                        text = version,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            val nodeId = state.nodeId
            InfoRow(
                label = stringResource(R.string.label_node_id),
                value = nodeId ?: stringResource(R.string.unknown_version),
                monospace = true,
                trailing = if (nodeId != null) {
                    {
                        IconButton(
                            onClick = { onCopyNodeId(nodeId) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = stringResource(
                                    R.string.cd_copy_value,
                                    stringResource(R.string.label_node_id)
                                ),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    null
                }
            )
            InfoRow(
                label = stringResource(R.string.label_app_version),
                value = state.appVersion,
                monospace = true
            )
        }
    }
}

/**
 * 单个网络卡片。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NetworkCard(
    item: NetworkListItem,
    connectedNetworkId: Long?,
    operatingNetworkId: Long?,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val network = item.summary
    val isOperating = operatingNetworkId == network.networkId
    val isActive = connectedNetworkId == network.networkId || isOperating
    val runtimeStatus = item.runtimeStatus
    val name = item.displayName ?: stringResource(R.string.network_unnamed)

    val statusLabel: String
    val statusTone: StatusTone
    when {
        isOperating -> {
            statusLabel = stringResource(R.string.network_status_connecting)
            statusTone = StatusTone.Connecting
        }
        !isActive -> {
            statusLabel = stringResource(R.string.network_status_disconnected)
            statusTone = StatusTone.Offline
        }
        runtimeStatus == null || runtimeStatus == NetworkStatus.REQUESTING_CONFIGURATION -> {
            statusLabel = stringResource(R.string.network_status_connecting)
            statusTone = StatusTone.Connecting
        }
        runtimeStatus == NetworkStatus.OK -> {
            statusLabel = stringResource(R.string.status_online)
            statusTone = StatusTone.Connected
        }
        else -> {
            statusLabel = stringResource(EnumTextFormatter.networkStatus(runtimeStatus))
            statusTone = StatusTone.Danger
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { menuExpanded = true }
            ),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 14.dp, end = 4.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    MonoText(
                        value = network.networkIdStr.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val switchDescription = stringResource(
                    R.string.cd_network_switch,
                    item.displayName
                        ?: network.networkIdStr?.takeIf { it.isNotEmpty() }
                        ?: name
                )
                Switch(
                    checked = isActive,
                    onCheckedChange = onCheckedChange,
                    colors = zerotierSwitchColors(),
                    modifier = Modifier.semantics {
                        contentDescription = switchDescription
                    }
                )
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.cd_more_options)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.copy_network_id_to_clipboard)) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Share, contentDescription = null)
                            },
                            onClick = {
                                menuExpanded = false
                                onCopy()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.delete_network),
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.padding(end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(label = statusLabel, tone = statusTone)
                if (item.ipAddresses.isNotEmpty()) {
                    MonoText(
                        value = item.ipAddresses.joinToString(" · "),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Preview(name = "浅色", showBackground = true)
@Composable
private fun NetworkListScreenLightPreview() {
    ZerotierFixTheme {
        NetworkListScreen(
            state = previewState(),
            onAddNetwork = {},
            onOpenNetwork = {},
            onOpenMoon = {},
            onNetworkEnabledChange = { _, _ -> },
            onDeleteNetwork = {},
            onCopyNetworkId = {},
            onCopyNodeId = {},
            onNotificationAlertOpenSettings = {},
            onNotificationAlertDontShowAgain = {},
            onNotificationAlertDismiss = {}
        )
    }
}

@Preview(name = "深色", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NetworkListScreenDarkPreview() {
    ZerotierFixTheme {
        NetworkListScreen(
            state = previewState(),
            onAddNetwork = {},
            onOpenNetwork = {},
            onOpenMoon = {},
            onNetworkEnabledChange = { _, _ -> },
            onDeleteNetwork = {},
            onCopyNetworkId = {},
            onCopyNodeId = {},
            onNotificationAlertOpenSettings = {},
            onNotificationAlertDontShowAgain = {},
            onNotificationAlertDismiss = {}
        )
    }
}

/**
 * 预览用状态。
 */
private fun previewState(): NetworkListUiState = NetworkListUiState(
    networks = listOf(
        NetworkListItem(
            summary = NetworkSummary(
                networkId = 0x8056c2e21c000001uL.toLong(),
                networkIdStr = "8056c2e21c000001",
                networkName = "Home",
                isLastActivated = true
            ),
            runtimeStatus = NetworkStatus.OK,
            ipAddresses = listOf("10.147.17.5/24"),
            displayName = "Home"
        ),
        NetworkListItem(
            summary = NetworkSummary(
                networkId = 0x8056c2e21c000002uL.toLong(),
                networkIdStr = "8056c2e21c000002",
                networkName = "Lab",
                isLastActivated = false
            ),
            runtimeStatus = null,
            ipAddresses = emptyList(),
            displayName = "Lab"
        )
    ),
    connectedNetworkId = 0x8056c2e21c000001uL.toLong(),
    serviceState = ServiceState.RUNNING,
    nodeId = "8056c2e21c",
    nodeStatus = NodeStatusDto(true, 0x8056c2e21cL, "1.16.2"),
    clientVersion = "1.16.2",
    isLoading = false,
    appVersion = "1.0.11+core.1.16.2"
)
