package net.kaaass.zerotierfix.features.peer

import android.content.res.Configuration

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import net.kaaass.zerotierfix.R
import net.kaaass.zerotierfix.model.dto.PeerInfoDto
import net.kaaass.zerotierfix.model.enums.PeerRole
import net.kaaass.zerotierfix.ui.common.EmptyState
import net.kaaass.zerotierfix.ui.common.EnumTextFormatter
import net.kaaass.zerotierfix.ui.common.ErrorState
import net.kaaass.zerotierfix.ui.common.InfoRow
import net.kaaass.zerotierfix.ui.common.LoadingState
import net.kaaass.zerotierfix.ui.common.MonoText
import net.kaaass.zerotierfix.ui.common.StatusPill
import net.kaaass.zerotierfix.ui.common.StatusTone
import net.kaaass.zerotierfix.ui.common.ZerotierTopBar
import net.kaaass.zerotierfix.ui.theme.ZerotierFixTheme
import net.kaaass.zerotierfix.util.StringUtils

/**
 * Peer 列表页面。
 *
 * @param state 页面状态
 * @param snackbarHostState Snackbar 宿主状态
 * @param onRefresh 下拉刷新 / 顶栏刷新
 * @param onRetry 重试
 * @param modifier 修饰符
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeerListScreen(
    state: PeerListUiState,
    snackbarHostState: SnackbarHostState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ZerotierTopBar(
                title = stringResource(R.string.peer_list),
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.cd_refresh_peers)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                // 首次加载与刷新都显示整屏加载态，避免点击刷新后没有任何可见反馈。
                state.isLoading || state.isRefreshing -> LoadingState(
                    text = stringResource(R.string.peer_loading)
                )
                state.error != null && state.peers.isEmpty() -> {
                    val error = state.error
                    ErrorState(
                        title = stringResource(error.titleRes()),
                        description = stringResource(error.descriptionRes()),
                        retryLabel = stringResource(R.string.action_retry),
                        onRetry = onRetry
                    )
                }
                state.peers.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.Person,
                    title = stringResource(R.string.peer_no_connection),
                    description = stringResource(R.string.peer_empty_message),
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = onRetry
                )
                else -> PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(key = "summary") {
                            PeerSummary(state = state)
                        }
                        items(
                            items = state.peers,
                            key = { it.address }
                        ) { peer ->
                            PeerCard(peer = peer)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Peer 汇总信息。
 */
@Composable
private fun PeerSummary(state: PeerListUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryItem(
                label = stringResource(R.string.peer_summary_total),
                value = state.peers.size.toString(),
                modifier = Modifier.weight(1f)
            )
            SummaryItem(
                label = stringResource(R.string.peer_summary_direct),
                value = state.directCount.toString(),
                modifier = Modifier.weight(1f)
            )
            SummaryItem(
                label = stringResource(R.string.peer_summary_relay),
                value = state.relayCount.toString(),
                modifier = Modifier.weight(1f)
            )
            SummaryItem(
                label = stringResource(R.string.peer_summary_latency),
                value = state.averageLatency?.let {
                    stringResource(R.string.peer_lat, it)
                } ?: stringResource(R.string.unknown_version),
                modifier = Modifier.weight(1.2f)
            )
        }
    }
}

/**
 * 单个汇总指标。
 */
@Composable
private fun SummaryItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 单个 Peer 卡片。
 */
@Composable
private fun PeerCard(peer: PeerInfoDto) {
    val latencyTone = when {
        peer.latency < 100 -> StatusTone.Connected
        peer.latency < 250 -> StatusTone.Connecting
        else -> StatusTone.Danger
    }
    val version = if (peer.versionMajor > 0) {
        StringUtils.versionString(
            peer.versionMajor,
            peer.versionMinor,
            peer.versionRevision
        )
    } else {
        stringResource(R.string.unknown_version)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(
                    label = stringResource(EnumTextFormatter.peerRole(peer.role)),
                    tone = StatusTone.Neutral
                )
                Spacer(modifier = Modifier.weight(1f))
                StatusPill(
                    label = stringResource(R.string.peer_lat, peer.latency),
                    tone = latencyTone
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            MonoText(
                value = java.lang.Long.toHexString(peer.address),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(
                label = stringResource(R.string.label_peer_client_version),
                value = version,
                monospace = true
            )
            InfoRow(
                label = stringResource(R.string.label_peer_preferred_path),
                value = peer.preferredPath ?: stringResource(R.string.peer_relay),
                monospace = peer.preferredPath != null
            )
        }
    }
}

/**
 * 错误类型对应的标题资源。
 */
private fun PeerListError.titleRes(): Int = when (this) {
    PeerListError.SERVICE_NOT_RUNNING -> R.string.network_service_stopped
    PeerListError.NODE_NOT_READY -> R.string.peer_node_not_ready
    PeerListError.RETRIEVE_FAILED -> R.string.peer_load_failed
}

/**
 * 错误类型对应的说明资源。
 */
private fun PeerListError.descriptionRes(): Int = when (this) {
    PeerListError.SERVICE_NOT_RUNNING -> R.string.peer_service_not_running
    PeerListError.NODE_NOT_READY -> R.string.fail_retrieve_peer_list
    PeerListError.RETRIEVE_FAILED -> R.string.fail_retrieve_peer_list
}

@Preview(name = "浅色", showBackground = true)
@Composable
private fun PeerListScreenPreview() {
    ZerotierFixTheme {
        PeerListScreen(
            state = previewState(),
            snackbarHostState = androidx.compose.material3.SnackbarHostState(),
            onRefresh = {},
            onRetry = {}
        )
    }
}

@Preview(name = "深色", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PeerListScreenDarkPreview() {
    ZerotierFixTheme {
        PeerListScreen(
            state = previewState(),
            snackbarHostState = androidx.compose.material3.SnackbarHostState(),
            onRefresh = {},
            onRetry = {}
        )
    }
}

@Preview(name = "错误", showBackground = true)
@Composable
private fun PeerListScreenErrorPreview() {
    ZerotierFixTheme {
        PeerListScreen(
            state = PeerListUiState(
                isLoading = false,
                hasLoaded = true,
                error = PeerListError.SERVICE_NOT_RUNNING
            ),
            snackbarHostState = androidx.compose.material3.SnackbarHostState(),
            onRefresh = {},
            onRetry = {}
        )
    }
}

/**
 * 预览用状态。
 */
private fun previewState(): PeerListUiState = PeerListUiState(
    peers = listOf(
        PeerInfoDto(
            address = 0x8056c2e21cL,
            role = PeerRole.PLANET,
            versionMajor = 1,
            versionMinor = 16,
            versionRevision = 2,
            latency = 42,
            preferredPath = "192.168.1.1/9993"
        ),
        PeerInfoDto(
            address = 0x8056c2e22cL,
            role = PeerRole.LEAF,
            versionMajor = 1,
            versionMinor = 16,
            versionRevision = 2,
            latency = 128,
            preferredPath = null
        )
    ),
    hasLoaded = true,
    isLoading = false
)
