package net.kaaass.zerotierfix.features.settings

import android.content.res.Configuration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import net.kaaass.zerotierfix.R
import net.kaaass.zerotierfix.ui.common.ActionRow
import net.kaaass.zerotierfix.ui.common.InfoRow
import net.kaaass.zerotierfix.ui.common.SectionHeader
import net.kaaass.zerotierfix.ui.common.SettingDivider
import net.kaaass.zerotierfix.ui.common.SettingsGroup
import net.kaaass.zerotierfix.ui.common.SwitchRow
import net.kaaass.zerotierfix.ui.common.ZerotierTopBar
import net.kaaass.zerotierfix.ui.icons.ZeroTierIcons
import net.kaaass.zerotierfix.ui.theme.ZerotierFixTheme

/**
 * 设置页面。
 *
 * @param state 页面状态
 * @param snackbarHostState Snackbar 宿主状态
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ZerotierTopBar(stringResource(R.string.settings)) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SectionHeader(title = stringResource(R.string.settings_network))
            SettingsGroup {
                SwitchRow(
                    title = stringResource(R.string.preferences_network_use_cell_data),
                    summary = stringResource(R.string.preferences_network_use_cell_data_summary),
                    checked = state.isCellularDataAllowed,
                    onCheckedChange = onSetCellularDataAllowed
                )
                SettingDivider()
                SwitchRow(
                    title = stringResource(R.string.preferences_network_disable_ipv6),
                    summary = stringResource(R.string.preferences_network_disable_ipv6_summary),
                    checked = state.isIpv6Disabled,
                    onCheckedChange = onSetIpv6Disabled
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader(title = stringResource(R.string.planet_settings))
            SettingsGroup {
                SwitchRow(
                    title = stringResource(R.string.use_custom_planet),
                    summary = stringResource(R.string.planet_use_custom_summary),
                    checked = state.isPlanetUseCustom,
                    onCheckedChange = onSetPlanetUseCustom,
                    icon = ZeroTierIcons.Planet
                )
                SettingDivider()
                ActionRow(
                    title = stringResource(R.string.set_planet_file),
                    summary = stringResource(
                        if (state.hasCustomPlanetFile) {
                            R.string.settings_planet_file_set
                        } else {
                            R.string.settings_planet_file_missing
                        }
                    ),
                    enabled = state.isPlanetUseCustom,
                    onClick = onOpenPlanetDialog
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader(title = stringResource(R.string.settings_about))
            SettingsGroup {
                InfoRow(
                    label = stringResource(R.string.settings_app_version),
                    value = state.appVersion,
                    monospace = true
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (state.isPlanetDialogVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismissPlanetDialog,
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.load_planet_file),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                ActionRow(
                    title = stringResource(R.string.file),
                    summary = stringResource(R.string.import_via_file),
                    icon = Icons.Outlined.Share,
                    onClick = onSetPlanetFromFile
                )
                ActionRow(
                    title = stringResource(R.string.url),
                    summary = stringResource(R.string.import_via_url),
                    icon = Icons.Outlined.Edit,
                    onClick = onOpenUrlDialog
                )
            }
        }
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
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissUrlDialog) {
                    Text(stringResource(R.string.action_cancel))
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
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(stringResource(R.string.downloading))
                }
            },
            confirmButton = {}
        )
    }
}

@Preview(name = "浅色", showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    ZerotierFixTheme {
        SettingsScreen(
            state = SettingsUiState(
                isCellularDataAllowed = true,
                isPlanetUseCustom = true,
                hasCustomPlanetFile = true,
                appVersion = "1.0.11+core.1.16.2"
            ),
            snackbarHostState = SnackbarHostState(),
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

@Preview(name = "深色", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsScreenDarkPreview() {
    ZerotierFixTheme {
        SettingsScreen(
            state = SettingsUiState(
                isCellularDataAllowed = false,
                isPlanetUseCustom = false,
                hasCustomPlanetFile = false,
                appVersion = "1.0.11+core.1.16.2"
            ),
            snackbarHostState = SnackbarHostState(),
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
