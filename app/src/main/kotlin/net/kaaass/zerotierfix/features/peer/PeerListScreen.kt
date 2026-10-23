package net.kaaass.zerotierfix.features.peer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import net.kaaass.zerotierfix.R
import net.kaaass.zerotierfix.model.dto.PeerInfoDto
import net.kaaass.zerotierfix.model.enums.PeerRole
import net.kaaass.zerotierfix.ui.common.BackTopBar
import net.kaaass.zerotierfix.ui.common.EmptyState
import net.kaaass.zerotierfix.ui.common.EnumTextFormatter
import net.kaaass.zerotierfix.ui.common.InfoRow
import net.kaaass.zerotierfix.ui.theme.ZerotierFixTheme
import net.kaaass.zerotierfix.util.StringUtils

/**
 * Peer 列表页面。
 *
 * @param state 页面状态
 * @param snackbarHostState Snackbar 宿主状态
 * @param onRefresh 下拉刷新
 * @param onBack 返回
 * @param modifier 修饰符
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeerListScreen(
    state: PeerListUiState,
    snackbarHostState: SnackbarHostState,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = { BackTopBar(stringResource(R.string.peer_list), onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (state.peers.isEmpty()) {
                    item {
                        EmptyState(
                            text = stringResource(R.string.peer_no_connection),
                            modifier = Modifier.fillParentMaxSize()
                        )
                    }
                } else {
                    items(state.peers, key = { it.address }) { peer ->
                        PeerItem(peer)
                    }
                }
            }
        }
    }
}

/**
 * 单个 Peer 条目。
 */
@Composable
private fun PeerItem(peer: PeerInfoDto) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 5.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            InfoRow(
                label = stringResource(R.string.peer_address),
                value = java.lang.Long.toHexString(peer.address),
                monospace = true
            )
            InfoRow(
                label = stringResource(R.string.peer_role),
                value = stringResource(EnumTextFormatter.peerRole(peer.role))
            )
            InfoRow(
                label = stringResource(R.string.peer_client_version),
                value = if (peer.versionMajor > 0) {
                    StringUtils.versionString(
                        peer.versionMajor,
                        peer.versionMinor,
                        peer.versionRevision
                    )
                } else {
                    stringResource(R.string.unknown_version)
                }
            )
            InfoRow(
                label = stringResource(R.string.peer_latency),
                value = stringResource(R.string.peer_lat, peer.latency)
            )
            InfoRow(
                label = stringResource(R.string.peer_preferred_path),
                value = peer.preferredPath ?: stringResource(R.string.peer_relay)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PeerListScreenPreview() {
    ZerotierFixTheme {
        PeerListScreen(
            state = PeerListUiState(
                peers = listOf(
                    PeerInfoDto(
                        address = 0x8056c2e21cL,
                        role = PeerRole.PLANET,
                        versionMajor = 1,
                        versionMinor = 16,
                        versionRevision = 2,
                        latency = 42,
                        preferredPath = "192.168.1.1/9993"
                    )
                ),
                hasLoaded = true
            ),
            snackbarHostState = androidx.compose.material3.SnackbarHostState(),
            onRefresh = {},
            onBack = {}
        )
    }
}
