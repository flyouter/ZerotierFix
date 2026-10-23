package net.kaaass.zerotierfix.features.join

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import net.kaaass.zerotierfix.R
import net.kaaass.zerotierfix.ui.common.BackTopBar
import net.kaaass.zerotierfix.ui.theme.ZerotierFixTheme

import org.apache.commons.validator.routines.InetAddressValidator

/**
 * 添加网络页面。
 *
 * @param state 页面状态
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
    onNetworkIdChanged: (String) -> Unit,
    onRouteViaZeroTierChanged: (Boolean) -> Unit,
    onDnsModeChanged: (Int) -> Unit,
    onDnsChanged: (JoinNetworkViewModel.DnsField, String) -> Unit,
    onJoin: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = { BackTopBar(stringResource(R.string.join_network), onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionLabel(stringResource(R.string.network_id_string))
            OutlinedTextField(
                value = state.networkId,
                onValueChange = onNetworkIdChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    capitalization = KeyboardCapitalization.Characters
                )
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = state.isRouteViaZeroTier,
                    onCheckedChange = onRouteViaZeroTierChanged,
                    enabled = state.isNetworkIdValid
                )
                Text(
                    text = stringResource(R.string.default_route),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            SectionLabel(stringResource(R.string.dns_label_string))
            PrimaryTabRow(selectedTabIndex = state.dnsMode.coerceIn(0, DNS_TAB_COUNT - 1)) {
                DNS_TABS.forEachIndexed { index, titleRes ->
                    Tab(
                        selected = state.dnsMode == index,
                        onClick = { onDnsModeChanged(index) },
                        text = { Text(stringResource(titleRes)) }
                    )
                }
            }
            when (state.dnsMode) {
                DNS_MODE_NETWORK -> Text(stringResource(R.string.network_dns_text))
                DNS_MODE_CUSTOM -> CustomDnsFields(state, onDnsChanged)
                else -> Text(stringResource(R.string.no_dns_text))
            }

            Button(
                onClick = onJoin,
                enabled = state.isNetworkIdValid && !state.isSubmitting,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(stringResource(R.string.join_network))
            }
        }
    }
}

/**
 * 自定义 DNS 输入区。
 */
@Composable
private fun CustomDnsFields(
    state: JoinNetworkUiState,
    onDnsChanged: (JoinNetworkViewModel.DnsField, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.dnsv4), style = MaterialTheme.typography.bodyMedium)
        DnsAddressField(
            value = state.dnsV4Primary,
            errorRes = R.string.invalid_ipv4_dns,
            onValueChange = { onDnsChanged(JoinNetworkViewModel.DnsField.V4_PRIMARY, it) }
        )
        DnsAddressField(
            value = state.dnsV4Secondary,
            errorRes = R.string.invalid_ipv4_dns,
            onValueChange = { onDnsChanged(JoinNetworkViewModel.DnsField.V4_SECONDARY, it) }
        )
        Text(stringResource(R.string.dnsv6), style = MaterialTheme.typography.bodyMedium)
        DnsAddressField(
            value = state.dnsV6Primary,
            errorRes = R.string.invalid_ipv6_dns,
            onValueChange = { onDnsChanged(JoinNetworkViewModel.DnsField.V6_PRIMARY, it) }
        )
        DnsAddressField(
            value = state.dnsV6Secondary,
            errorRes = R.string.invalid_ipv6_dns,
            onValueChange = { onDnsChanged(JoinNetworkViewModel.DnsField.V6_SECONDARY, it) }
        )
    }
}

/**
 * 单个 DNS 地址输入框，非空且格式非法时展示错误。
 */
@Composable
private fun DnsAddressField(
    value: String,
    @StringRes errorRes: Int,
    onValueChange: (String) -> Unit
) {
    val isError = value.isNotEmpty() && !InetAddressValidator.getInstance().isValid(value)
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
        isError = isError,
        supportingText = supportingText
    )
}

/**
 * 分组标题。
 */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )
}

private const val DNS_MODE_NETWORK = 1
private const val DNS_MODE_CUSTOM = 2
private const val DNS_TAB_COUNT = 3

private val DNS_TABS = listOf(
    R.string.no_dns_tab_title,
    R.string.network_dns_tab_title,
    R.string.custom_dns_tab_title
)

@Preview(showBackground = true)
@Composable
private fun JoinNetworkScreenPreview() {
    ZerotierFixTheme {
        JoinNetworkScreen(
            state = JoinNetworkUiState(
                networkId = "8056c2e21c000001",
                isNetworkIdValid = true,
                dnsMode = DNS_MODE_CUSTOM,
                dnsV4Primary = "8.8.8.8"
            ),
            onNetworkIdChanged = {},
            onRouteViaZeroTierChanged = {},
            onDnsModeChanged = {},
            onDnsChanged = { _, _ -> },
            onJoin = {},
            onBack = {}
        )
    }
}
