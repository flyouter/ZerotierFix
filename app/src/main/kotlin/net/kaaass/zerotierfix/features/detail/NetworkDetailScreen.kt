package net.kaaass.zerotierfix.features.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import net.kaaass.zerotierfix.R
import net.kaaass.zerotierfix.model.enums.DNSMode
import net.kaaass.zerotierfix.ui.common.BackTopBar
import net.kaaass.zerotierfix.ui.common.DetailRow
import net.kaaass.zerotierfix.ui.common.EnumTextFormatter
import net.kaaass.zerotierfix.ui.common.InfoRow
import net.kaaass.zerotierfix.ui.theme.ZerotierFixTheme

/**
 * 网络详情页面。
 *
 * @param state 页面状态
 * @param onRouteViaZeroTierChanged “通过 ZeroTier 路由”变化
 * @param onBack 返回
 * @param modifier 修饰符
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkDetailScreen(
    state: NetworkDetailUiState,
    onRouteViaZeroTierChanged: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val virtual = state.virtualConfig
    val isDnsRowVisible = state.dnsMode == DNSMode.NETWORK_DNS.toInt()

    Scaffold(
        modifier = modifier,
        topBar = { BackTopBar(stringResource(R.string.network_detail), onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            InfoRow(
                label = stringResource(R.string.network_id),
                value = state.networkIdStr,
                monospace = true
            )
            InfoRow(
                label = stringResource(R.string.network_name),
                value = state.networkName.takeIf { it.isNotEmpty() }
                    ?: stringResource(R.string.empty_network_name)
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            DetailRow(
                label = stringResource(R.string.network_status),
                value = virtual?.let {
                    stringResource(EnumTextFormatter.networkStatus(it.status))
                } ?: stringResource(R.string.network_status_not_connected)
            )
            DetailRow(
                label = stringResource(R.string.network_type),
                value = virtual?.let {
                    stringResource(EnumTextFormatter.networkType(it.type))
                }.orEmpty()
            )
            DetailRow(
                label = stringResource(R.string.network_mac),
                value = virtual?.let { net.kaaass.zerotierfix.util.StringUtils.macAddressToString(it.mac) }
                    .orEmpty()
            )
            DetailRow(
                label = stringResource(R.string.network_mtu),
                value = virtual?.mtu?.toString().orEmpty()
            )
            DetailRow(
                label = stringResource(R.string.network_broadcast),
                value = virtual?.let { booleanText(it.isBroadcastEnabled) }.orEmpty()
            )
            DetailRow(
                label = stringResource(R.string.network_bridging),
                value = virtual?.let { booleanText(it.isBridge) }.orEmpty()
            )
            DetailRow(
                label = stringResource(R.string.network_dns_mode),
                value = state.dnsMode?.let {
                    stringResource(EnumTextFormatter.dnsMode(DNSMode.fromInt(it)))
                }.orEmpty()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.network_default_route),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Checkbox(
                    checked = state.isRouteViaZeroTier,
                    onCheckedChange = onRouteViaZeroTierChanged
                )
            }

            SectionBlock(
                title = stringResource(R.string.network_ips),
                value = state.ipAddresses.joinToString("\n")
            )
            SectionBlock(
                title = stringResource(R.string.custom_dns),
                value = state.dnsServers.joinToString("\n"),
                modifier = Modifier.alpha(if (isDnsRowVisible) 1f else 0f)
            )
        }
    }
}

/**
 * 布尔值本地化文本。
 */
@Composable
private fun booleanText(value: Boolean): String = stringResource(
    if (value) R.string.enabled else R.string.disabled
)

/**
 * 详情页的分段信息块。
 */
@Composable
private fun SectionBlock(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End
        )
        Text(
            text = value,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NetworkDetailScreenPreview() {
    ZerotierFixTheme {
        NetworkDetailScreen(
            state = NetworkDetailUiState(
                networkId = 0x0000000000000001L,
                networkIdStr = "0000000000000001",
                networkName = "Home",
                dnsMode = DNSMode.NETWORK_DNS.toInt(),
                isRouteViaZeroTier = true,
                ipAddresses = listOf("192.168.191.1/24"),
                dnsServers = listOf("192.168.191.1/53"),
                isLoading = false
            ),
            onRouteViaZeroTierChanged = {},
            onBack = {}
        )
    }
}
