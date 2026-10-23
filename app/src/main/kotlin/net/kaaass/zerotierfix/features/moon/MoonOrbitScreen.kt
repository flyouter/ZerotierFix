package net.kaaass.zerotierfix.features.moon

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import net.kaaass.zerotierfix.R
import net.kaaass.zerotierfix.model.dto.MoonOrbitDto
import net.kaaass.zerotierfix.ui.common.BackTopBar
import net.kaaass.zerotierfix.ui.common.EmptyState
import net.kaaass.zerotierfix.ui.common.InfoRow
import net.kaaass.zerotierfix.ui.theme.ZerotierFixTheme

/**
 * Moon 入轨页面。
 *
 * @param state 页面状态
 * @param snackbarHostState Snackbar 宿主状态
 * @param onBack 返回
 * @param onPickFile 选择 Moon 文件
 * @param onAddOrbit 手工添加入轨信息
 * @param onRemove 删除入轨信息
 * @param onDeleteCache 删除缓存文件
 * @param onCopyWorldId 复制 Moon 地址
 * @param modifier 修饰符
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoonOrbitScreen(
    state: MoonOrbitUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onPickFile: () -> Unit,
    onAddOrbit: (String, String) -> Unit,
    onRemove: (MoonOrbitDto) -> Unit,
    onDeleteCache: (MoonOrbitDto) -> Unit,
    onCopyWorldId: (MoonOrbitDto) -> Unit,
    modifier: Modifier = Modifier
) {
    var providerDialogVisible by rememberSaveable { mutableStateOf(false) }
    var orbitDialogVisible by rememberSaveable { mutableStateOf(false) }
    var worldIdText by rememberSaveable { mutableStateOf("") }
    var seedText by rememberSaveable { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        topBar = { BackTopBar(stringResource(R.string.moon_orbit_activity), onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { providerDialogVisible = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add))
            }
        }
    ) { padding ->
        if (state.moons.isEmpty()) {
            EmptyState(
                text = stringResource(R.string.no_data),
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
            ) {
                items(state.moons, key = { it.moonWorldId }) { moon ->
                    MoonItem(
                        moon = moon,
                        onCopy = { onCopyWorldId(moon) },
                        onDelete = { onRemove(moon) },
                        onDeleteCache = { onDeleteCache(moon) }
                    )
                }
            }
        }
    }

    if (providerDialogVisible) {
        AlertDialog(
            onDismissRequest = { providerDialogVisible = false },
            title = { Text(stringResource(R.string.import_moon_node)) },
            text = {
                Column {
                    ProviderOption(
                        title = stringResource(R.string.menu_orbit),
                        subtitle = stringResource(R.string.import_via_orbit)
                    ) {
                        providerDialogVisible = false
                        worldIdText = ""
                        seedText = ""
                        orbitDialogVisible = true
                    }
                    ProviderOption(
                        title = stringResource(R.string.file),
                        subtitle = stringResource(R.string.import_via_file)
                    ) {
                        providerDialogVisible = false
                        onPickFile()
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (orbitDialogVisible) {
        AlertDialog(
            onDismissRequest = { orbitDialogVisible = false },
            title = { Text(stringResource(R.string.moon_orbit_info)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.moon_world_id))
                    OutlinedTextField(
                        value = worldIdText,
                        onValueChange = { worldIdText = filterHex(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    HintText(stringResource(R.string.moon_world_id_hint))
                    Text(stringResource(R.string.moon_seed))
                    OutlinedTextField(
                        value = seedText,
                        onValueChange = { seedText = filterHex(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    HintText(stringResource(R.string.moon_seed_hint))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        orbitDialogVisible = false
                        onAddOrbit(worldIdText, seedText)
                    }
                ) {
                    Text(stringResource(R.string.add))
                }
            },
            dismissButton = {
                TextButton(onClick = { orbitDialogVisible = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}

/**
 * 单个入轨条目。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MoonItem(
    moon: MoonOrbitDto,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onDeleteCache: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 5.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { menuExpanded = true }
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                InfoRow(
                    label = stringResource(R.string.moon_world_id),
                    value = java.lang.Long.toHexString(moon.moonWorldId),
                    monospace = true
                )
                InfoRow(
                    label = stringResource(R.string.moon_seed),
                    value = if (moon.isFromFile) {
                        DASH
                    } else {
                        java.lang.Long.toHexString(moon.moonSeed)
                    },
                    monospace = true
                )
                InfoRow(
                    label = stringResource(R.string.config_file),
                    value = when {
                        moon.isFromFile -> stringResource(R.string.external_file)
                        moon.isCacheFile -> stringResource(R.string.cached)
                        else -> stringResource(R.string.wait_to_fetch)
                    }
                )
            }
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.copy_moon_world_id_to_clipboard)) },
                onClick = {
                    menuExpanded = false
                    onCopy()
                }
            )
            if (moon.isCacheFile && !moon.isFromFile) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete_cache)) },
                    onClick = {
                        menuExpanded = false
                        onDeleteCache()
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete_moon_orbit)) },
                onClick = {
                    menuExpanded = false
                    onDelete()
                }
            )
        }
    }
}

/**
 * 弹窗中的可选条目。
 */
@Composable
private fun ProviderOption(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 提示文本。
 */
@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * 过滤 16 进制字符并限制长度。
 */
private fun filterHex(value: String): String =
    value.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }.take(16)

private const val DASH = "-"

@Preview(showBackground = true)
@Composable
private fun MoonOrbitScreenPreview() {
    ZerotierFixTheme {
        MoonOrbitScreen(
            state = MoonOrbitUiState(
                moons = listOf(
                    MoonOrbitDto(0x8056c2e21cL, 0x8056c2e21cL, true, true),
                    MoonOrbitDto(0x8056c2e21dL, 0x8056c2e21dL, false, false)
                ),
                isLoading = false
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onPickFile = {},
            onAddOrbit = { _, _ -> },
            onRemove = {},
            onDeleteCache = {},
            onCopyWorldId = {}
        )
    }
}
