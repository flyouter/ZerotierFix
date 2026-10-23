package net.kaaass.zerotierfix.features.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import net.kaaass.zerotierfix.R
import net.kaaass.zerotierfix.ui.common.BackTopBar
import net.kaaass.zerotierfix.ui.theme.ZerotierFixTheme

/**
 * 设置页面。
 *
 * @param state 页面状态
 * @param snackbarHostState Snackbar 宿主状态
 * @param onBack 返回
 * @param onSetPlanetUseCustom 切换自定义 planet
 * @param onOpenPlanetDialog 打开 planet 文件来源对话框
 * @param onDismissPlanetDialog 关闭 planet 文件来源对话框
 * @param onSetPlanetFromFile 打开文件选择器
 * @param onOpenUrlDialog 打开 URL 输入对话框
 * @param onDismissUrlDialog 关闭 URL 输入对话框
 * @param onUrlChanged URL 输入变化
 * @param onSubmitUrl 提交 URL
 * @param onSetCellularDataAllowed 切换移动数据开关
 * @param onSetIpv6Disabled 切换 IPv6 开关
 * @param modifier 修饰符
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onSetPlanetUseCustom: (Boolean) -> Unit,
    onOpenPlanetDialog: () -> Unit,
    onDismissPlanetDialog: () -> Unit,
    onSetPlanetFromFile: () -> Unit,
    onOpenUrlDialog: () -> Unit,
    onDismissUrlDialog: () -> Unit,
    onUrlChanged: (String) -> Unit,
    onSubmitUrl: () -> Unit,
    onSetCellularDataAllowed: (Boolean) -> Unit,
    onSetIpv6Disabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = { BackTopBar(stringResource(R.string.settings), onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeader(stringResource(R.string.planet_settings))
            SwitchRow(
                title = stringResource(R.string.use_custom_planet),
                summary = stringResource(R.string.planet_use_custom_summary),
                checked = state.isPlanetUseCustom,
                onCheckedChange = onSetPlanetUseCustom
            )
            TextRow(
                title = stringResource(R.string.set_planet_file),
                enabled = state.isPlanetUseCustom,
                onClick = onOpenPlanetDialog
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader(stringResource(R.string.preferences_network))
            SwitchRow(
                title = stringResource(R.string.preferences_network_use_cell_data),
                summary = null,
                checked = state.isCellularDataAllowed,
                onCheckedChange = onSetCellularDataAllowed
            )
            SwitchRow(
                title = stringResource(R.string.preferences_network_disable_ipv6),
                summary = stringResource(R.string.preferences_network_disable_ipv6_summary),
                checked = state.isIpv6Disabled,
                onCheckedChange = onSetIpv6Disabled
            )
        }
    }

    if (state.isPlanetDialogVisible) {
        AlertDialog(
            onDismissRequest = onDismissPlanetDialog,
            title = { Text(stringResource(R.string.load_planet_file)) },
            text = {
                Column {
                    TextRow(
                        title = stringResource(R.string.file),
                        subtitle = stringResource(R.string.import_via_file),
                        enabled = true,
                        onClick = onSetPlanetFromFile
                    )
                    TextRow(
                        title = stringResource(R.string.url),
                        subtitle = stringResource(R.string.import_via_url),
                        enabled = true,
                        onClick = onOpenUrlDialog
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismissPlanetDialog) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    if (state.isUrlDialogVisible) {
        AlertDialog(
            onDismissRequest = onDismissUrlDialog,
            title = { Text(stringResource(R.string.import_via_url)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.enter_planet_file_url))
                    OutlinedTextField(
                        value = state.urlText,
                        onValueChange = onUrlChanged,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onSubmitUrl) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissUrlDialog) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    if (state.isLoadingDialogVisible) {
        AlertDialog(
            onDismissRequest = {},
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.downloading))
                }
            },
            confirmButton = {}
        )
    }
}

/**
 * 分组标题。
 */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

/**
 * 开关设置行。
 */
@Composable
private fun SwitchRow(
    title: String,
    summary: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * 可点击设置行。
 */
@Composable
private fun TextRow(
    title: String,
    enabled: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    ZerotierFixTheme {
        SettingsScreen(
            state = SettingsUiState(
                isCellularDataAllowed = true,
                isPlanetUseCustom = false
            ),
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onSetPlanetUseCustom = {},
            onOpenPlanetDialog = {},
            onDismissPlanetDialog = {},
            onSetPlanetFromFile = {},
            onOpenUrlDialog = {},
            onDismissUrlDialog = {},
            onUrlChanged = {},
            onSubmitUrl = {},
            onSetCellularDataAllowed = {},
            onSetIpv6Disabled = {}
        )
    }
}
