package net.kaaass.zerotierfix.features.join

import android.content.res.Configuration

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import net.kaaass.zerotierfix.R
import net.kaaass.zerotierfix.model.enums.DNSMode
import net.kaaass.zerotierfix.ui.common.DnsField
import net.kaaass.zerotierfix.ui.common.SectionHeader
import net.kaaass.zerotierfix.ui.common.SettingsGroup
import net.kaaass.zerotierfix.ui.common.SwitchRow
import net.kaaass.zerotierfix.ui.common.ZerotierTopBar
import net.kaaass.zerotierfix.ui.common.isValidDnsAddress
import net.kaaass.zerotierfix.ui.theme.ZerotierFixTheme

/**
 * 添加网络页面。
 *
 * @param state 页面状态
 * @param snackbarHostState Snackbar 宿主状态
 * @param onNetworkIdChanged 网络 ID 输入变化
 * @param onRouteViaZeroTierChanged “通过 ZeroTier 路由”变化
 * @param onDnsModeChanged DNS 模式变化
 * @param onDnsChanged 自定义 DNS 输入变化
 * @param onJoin 提交
 * @param onBack 返回
 * @param modifier 修饰符
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinNetworkScreen(
    state: JoinNetworkUiState,
    snackbarHostState: SnackbarHostState,
    onNetworkIdChanged: (String) -> Unit,
    onRouteViaZeroTierChanged: (Boolean) -> Unit,
    onDnsModeChanged: (DNSMode) -> Unit,
    onDnsChanged: (DnsField, String) -> Unit,
    onJoin: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ZerotierTopBar(stringResource(R.string.join_network), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            JoinBottomBar(
                state = state,
                onJoin = onJoin
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionHeader(title = stringResource(R.string.label_network_id))
            OutlinedTextField(
                value = state.networkId,
                onValueChange = onNetworkIdChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                ),
                placeholder = { Text(stringResource(R.string.network_id_hint)) },
                isError = state.networkId.isNotEmpty() && !state.isNetworkIdValid,
                supportingText = if (state.networkId.isNotEmpty() && !state.isNetworkIdValid) {
                    {
                        Text(
                            stringResource(
                                R.string.network_id_remaining,
                                NETWORK_ID_LENGTH - state.networkId.length
                            )
                        )
                    }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    capitalization = KeyboardCapitalization.Characters
                )
            )

            SettingsGroup {
                SwitchRow(
                    title = stringResource(R.string.default_route),
                    checked = state.isRouteViaZeroTier,
                    onCheckedChange = onRouteViaZeroTierChanged,
                    enabled = state.isNetworkIdValid
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(title = stringResource(R.string.dns_label_string))
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                DnsModeSelector(
                    selected = state.dnsMode,
                    onSelect = onDnsModeChanged
                )
            }
            Text(
                text = stringResource(
                    when (state.dnsMode) {
                        DNSMode.NO_DNS -> R.string.no_dns_text
                        DNSMode.NETWORK_DNS -> R.string.network_dns_text
                        DNSMode.CUSTOM_DNS -> R.string.dns_custom_hint
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            if (state.dnsMode == DNSMode.CUSTOM_DNS) {
                CustomDnsFields(
                    state = state,
                    onDnsChanged = onDnsChanged
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * DNS 模式选择器。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DnsModeSelector(
    selected: DNSMode,
    onSelect: (DNSMode) -> Unit
) {
    val modes = DNSMode.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                label = {
                    Text(
                        text = stringResource(mode.labelRes),
                        maxLines = 1
                    )
                }
            )
        }
    }
}

/**
 * 自定义 DNS 输入区。
 */
@Composable
private fun CustomDnsFields(
    state: JoinNetworkUiState,
    onDnsChanged: (DnsField, String) -> Unit
) {
    SettingsGroup {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.dnsv4),
                style = MaterialTheme.typography.titleSmall
            )
            DnsAddressField(
                value = state.dnsV4Primary,
                field = DnsField.V4_PRIMARY,
                label = stringResource(R.string.dns_primary),
                errorRes = R.string.invalid_ipv4_dns,
                onValueChange = { onDnsChanged(DnsField.V4_PRIMARY, it) }
            )
            DnsAddressField(
                value = state.dnsV4Secondary,
                field = DnsField.V4_SECONDARY,
                label = stringResource(R.string.dns_secondary),
                errorRes = R.string.invalid_ipv4_dns,
                onValueChange = { onDnsChanged(DnsField.V4_SECONDARY, it) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = stringResource(R.string.dnsv6),
                style = MaterialTheme.typography.titleSmall
            )
            DnsAddressField(
                value = state.dnsV6Primary,
                field = DnsField.V6_PRIMARY,
                label = stringResource(R.string.dns_primary),
                errorRes = R.string.invalid_ipv6_dns,
                onValueChange = { onDnsChanged(DnsField.V6_PRIMARY, it) }
            )
            DnsAddressField(
                value = state.dnsV6Secondary,
                field = DnsField.V6_SECONDARY,
                label = stringResource(R.string.dns_secondary),
                errorRes = R.string.invalid_ipv6_dns,
                onValueChange = { onDnsChanged(DnsField.V6_SECONDARY, it) }
            )
        }
    }
}

/**
 * 单个 DNS 地址输入框，非空且格式非法时展示错误。
 */
@Composable
private fun DnsAddressField(
    value: String,
    field: DnsField,
    label: String,
    @StringRes errorRes: Int,
    onValueChange: (String) -> Unit
) {
    val isError = value.isNotEmpty() && !isValidDnsAddress(value, field)
    val supportingText: (@Composable () -> Unit)? = if (isError) {
        { Text(stringResource(errorRes)) }
    } else {
        null
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
        isError = isError,
        supportingText = supportingText,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
    )
}

/**
 * 底部粘性提交栏。
 */
@Composable
private fun JoinBottomBar(
    state: JoinNetworkUiState,
    onJoin: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = onJoin,
                enabled = state.canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.join_submitting))
                } else {
                    Text(stringResource(R.string.join_network))
                }
            }
        }
    }
}

/**
 * DNS 模式对应的标题资源。
 */
private val DNSMode.labelRes: Int
    @StringRes get() = when (this) {
        DNSMode.NO_DNS -> R.string.no_dns_tab_title
        DNSMode.NETWORK_DNS -> R.string.network_dns_tab_title
        DNSMode.CUSTOM_DNS -> R.string.custom_dns_tab_title
    }

private const val NETWORK_ID_LENGTH = 16

@Preview(name = "浅色", showBackground = true)
@Composable
private fun JoinNetworkScreenPreview() {
    ZerotierFixTheme {
        JoinNetworkScreen(
            state = JoinNetworkUiState(
                networkId = "8056c2e21c000001",
                isNetworkIdValid = true,
                dnsMode = DNSMode.CUSTOM_DNS,
                dnsV4Primary = "8.8.8.8"
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onNetworkIdChanged = {},
            onRouteViaZeroTierChanged = {},
            onDnsModeChanged = {},
            onDnsChanged = { _, _ -> },
            onJoin = {},
            onBack = {}
        )
    }
}

@Preview(name = "深色", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun JoinNetworkScreenDarkPreview() {
    ZerotierFixTheme {
        JoinNetworkScreen(
            state = JoinNetworkUiState(
                networkId = "8056c2e21c000001",
                isNetworkIdValid = true,
                dnsMode = DNSMode.NETWORK_DNS
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onNetworkIdChanged = {},
            onRouteViaZeroTierChanged = {},
            onDnsModeChanged = {},
            onDnsChanged = { _, _ -> },
            onJoin = {},
            onBack = {}
        )
    }
}
