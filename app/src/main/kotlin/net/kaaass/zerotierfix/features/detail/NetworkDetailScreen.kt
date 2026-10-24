package net.kaaass.zerotierfix.features.detail

import android.content.res.Configuration

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import net.kaaass.zerotierfix.R
import net.kaaass.zerotierfix.model.dto.VirtualNetworkConfigDto
import net.kaaass.zerotierfix.model.dto.VirtualNetworkDnsDto
import net.kaaass.zerotierfix.model.enums.DNSMode
import net.kaaass.zerotierfix.model.enums.NetworkStatus
import net.kaaass.zerotierfix.model.enums.NetworkType
import net.kaaass.zerotierfix.ui.common.EnumTextFormatter
import net.kaaass.zerotierfix.ui.common.ErrorState
import net.kaaass.zerotierfix.ui.common.InfoRow
import net.kaaass.zerotierfix.ui.common.LoadingState
import net.kaaass.zerotierfix.ui.common.MonoText
import net.kaaass.zerotierfix.ui.common.SectionHeader
import net.kaaass.zerotierfix.ui.common.SettingDivider
import net.kaaass.zerotierfix.ui.common.SettingsGroup
import net.kaaass.zerotierfix.ui.common.StatusPill
import net.kaaass.zerotierfix.ui.common.StatusTone
import net.kaaass.zerotierfix.ui.common.SwitchRow
import net.kaaass.zerotierfix.ui.common.ZerotierTopBar
import net.kaaass.zerotierfix.ui.theme.ZerotierFixTheme
import net.kaaass.zerotierfix.util.StringUtils
import net.kaaass.zerotierfix.vpn.engine.ServiceState

import java.net.InetSocketAddress

/**
 * 网络详情页面。
 *
 * @param state 页面状态
 * @param onRouteViaZeroTierChanged “通过 ZeroTier 路由”变化
 * @param onRetry 重新加载
 * @param onBack 返回
 * @param modifier 修饰符
 */
@Composable
fun NetworkDetailScreen(
    state: NetworkDetailUiState,
    onRouteViaZeroTierChanged: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ZerotierTopBar(
                title = state.networkName.takeIf { it.isNotEmpty() }
                    ?: stringResource(R.string.network_detail),
                subtitle = state.networkIdStr.takeIf { it.isNotEmpty() },
                onBack = onBack
            )
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingState(
                text = stringResource(R.string.loading),
                modifier = Modifier.padding(padding)
            )
            state.isNotFound -> ErrorState(
                title = stringResource(R.string.detail_not_found),
                description = stringResource(R.string.detail_load_failed),
                retryLabel = stringResource(R.string.action_retry),
                onRetry = onRetry,
                modifier = Modifier.padding(padding)
            )
            else -> DetailContent(
                state = state,
                onRouteViaZeroTierChanged = onRouteViaZeroTierChanged,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

/**
 * 详情页主体内容。
 */
@Composable
private fun DetailContent(
    state: NetworkDetailUiState,
    onRouteViaZeroTierChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val virtual = state.virtualConfig
    val dnsMode = state.dnsMode?.let { value ->
        runCatching { DNSMode.fromInt(value) }.getOrDefault(DNSMode.NO_DNS)
    }
    val (dnsValues, dnsEmptyText) = when (dnsMode) {
        DNSMode.NO_DNS -> emptyList<String>() to stringResource(R.string.no_dns_text)
        DNSMode.NETWORK_DNS -> state.dnsServers to stringResource(R.string.network_dns_waiting)
        DNSMode.CUSTOM_DNS -> state.customDnsServers to stringResource(R.string.dns_custom_hint)
        null -> emptyList<String>() to stringResource(R.string.no_dns_text)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        HeaderCard(state = state)
        Spacer(modifier = Modifier.height(8.dp))

        SectionHeader(title = stringResource(R.string.detail_section_connection))
        SettingsGroup {
            SwitchRow(
                title = stringResource(R.string.network_default_route),
                checked = state.isRouteViaZeroTier,
                onCheckedChange = onRouteViaZeroTierChanged,
                enabled = state.dnsMode != null
            )
            SettingDivider()
            AddressBlock(
                title = stringResource(R.string.network_ips),
                values = state.ipAddresses,
                emptyText = stringResource(R.string.network_no_managed_ips)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        SectionHeader(title = stringResource(R.string.detail_section_addresses))
        SettingsGroup {
            InfoRow(
                label = stringResource(R.string.network_dns_mode),
                value = dnsMode?.let { stringResource(EnumTextFormatter.dnsMode(it)) }
                    ?: stringResource(R.string.unknown_version)
            )
            SettingDivider()
            AddressBlock(
                title = stringResource(R.string.network_dns_servers),
                values = dnsValues,
                emptyText = dnsEmptyText
            )
            if (!state.dnsDomain.isNullOrEmpty()) {
                SettingDivider()
                InfoRow(
                    label = stringResource(R.string.network_dns_domain),
                    value = state.dnsDomain,
                    monospace = true
                )
            }
            if (virtual != null) {
                SettingDivider()
                InfoRow(
                    label = stringResource(R.string.network_mac),
                    value = StringUtils.macAddressToString(virtual.mac),
                    monospace = true
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        SectionHeader(title = stringResource(R.string.detail_section_network))
        SettingsGroup {
            InfoRow(
                label = stringResource(R.string.network_status),
                value = virtual?.let { stringResource(EnumTextFormatter.networkStatus(it.status)) }
                    ?: stringResource(R.string.network_status_not_connected)
            )
            if (virtual == null) {
                SettingDivider()
                Text(
                    text = stringResource(R.string.detail_network_info_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            } else {
                SettingDivider()
                InfoRow(
                    label = stringResource(R.string.network_type),
                    value = stringResource(EnumTextFormatter.networkType(virtual.type))
                )
                SettingDivider()
                InfoRow(
                    label = stringResource(R.string.network_mtu),
                    value = virtual.mtu.toString(),
                    monospace = true
                )
                SettingDivider()
                InfoRow(
                    label = stringResource(R.string.network_broadcast),
                    value = booleanText(virtual.isBroadcastEnabled)
                )
                SettingDivider()
                InfoRow(
                    label = stringResource(R.string.network_bridging),
                    value = booleanText(virtual.isBridge)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * 详情页头部卡片。
 */
@Composable
private fun HeaderCard(state: NetworkDetailUiState) {
    val virtual = state.virtualConfig
    val statusLabel: String
    val statusTone: StatusTone
    when {
        state.serviceState == ServiceState.STOPPED ||
            state.serviceState == ServiceState.UNKNOWN -> {
            statusLabel = stringResource(R.string.network_service_stopped)
            statusTone = StatusTone.Offline
        }
        virtual == null -> {
            statusLabel = stringResource(R.string.network_status_not_connected)
            statusTone = StatusTone.Offline
        }
        virtual.status == NetworkStatus.OK -> {
            statusLabel = stringResource(R.string.status_online)
            statusTone = StatusTone.Connected
        }
        virtual.status == NetworkStatus.REQUESTING_CONFIGURATION -> {
            statusLabel = stringResource(R.string.network_status_connecting)
            statusTone = StatusTone.Connecting
        }
        else -> {
            statusLabel = stringResource(EnumTextFormatter.networkStatus(virtual.status))
            statusTone = StatusTone.Danger
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = state.networkName.takeIf { it.isNotEmpty() }
                    ?: stringResource(R.string.network_unnamed),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(10.dp))
            StatusPill(label = statusLabel, tone = statusTone)
            if (state.networkIdStr.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                SelectionContainer {
                    MonoText(
                        value = state.networkIdStr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 多值地址块。
 */
@Composable
private fun AddressBlock(
    title: String,
    values: List<String>,
    emptyText: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        if (values.isEmpty()) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    values.forEach { value ->
                        MonoText(
                            value = value,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
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

@Preview(name = "浅色", showBackground = true)
@Composable
private fun NetworkDetailScreenPreview() {
    ZerotierFixTheme {
        NetworkDetailScreen(
            state = previewState(),
            onRouteViaZeroTierChanged = {},
            onRetry = {},
            onBack = {}
        )
    }
}

@Preview(name = "深色", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NetworkDetailScreenDarkPreview() {
    ZerotierFixTheme {
        NetworkDetailScreen(
            state = previewState(),
            onRouteViaZeroTierChanged = {},
            onRetry = {},
            onBack = {}
        )
    }
}

/**
 * 预览用状态。
 */
private fun previewState(): NetworkDetailUiState = NetworkDetailUiState(
    networkId = 0x8056c2e21c000001uL.toLong(),
    networkIdStr = "8056c2e21c000001",
    networkName = "Home",
    dnsMode = DNSMode.NETWORK_DNS.toInt(),
    isRouteViaZeroTier = true,
    virtualConfig = VirtualNetworkConfigDto(
        networkId = 0x8056c2e21c000001uL.toLong(),
        networkName = "Home",
        status = NetworkStatus.OK,
        type = NetworkType.PRIVATE,
        mac = 0x112233445566L,
        mtu = 2800,
        isBroadcastEnabled = true,
        isBridge = false,
        assignedAddresses = listOf(
            InetSocketAddress("10.147.17.5", 24),
            InetSocketAddress("fd00:1234::5", 88)
        ),
        dns = VirtualNetworkDnsDto(
            domain = "home.arpa",
            servers = listOf(InetSocketAddress("10.147.17.1", 53))
        )
    ),
    ipAddresses = listOf("10.147.17.5/24", "fd00:1234::5/88"),
    dnsServers = listOf("10.147.17.1/53"),
    dnsDomain = "home.arpa",
    serviceState = ServiceState.RUNNING,
    isLoading = false
)
