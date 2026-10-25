package net.kaaass.zerotierfix.features.moon

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import net.kaaass.zerotierfix.R
import net.kaaass.zerotierfix.model.dto.MoonOrbitDto
import net.kaaass.zerotierfix.navigation.WIDE_LAYOUT_MIN_WIDTH_DP
import net.kaaass.zerotierfix.ui.common.ActionRow
import net.kaaass.zerotierfix.ui.common.ConfirmDialog
import net.kaaass.zerotierfix.ui.common.EmptyState
import net.kaaass.zerotierfix.ui.common.InfoRow
import net.kaaass.zerotierfix.ui.common.LoadingState
import net.kaaass.zerotierfix.ui.common.MonoText
import net.kaaass.zerotierfix.ui.common.StatusPill
import net.kaaass.zerotierfix.ui.common.StatusTone
import net.kaaass.zerotierfix.ui.common.ZerotierTopBar
import net.kaaass.zerotierfix.ui.icons.ZeroTierIcons
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
    var providerSheetVisible by rememberSaveable { mutableStateOf(false) }
    var orbitDialogVisible by rememberSaveable { mutableStateOf(false) }
    var worldIdText by rememberSaveable { mutableStateOf("") }
    var seedText by rememberSaveable { mutableStateOf("") }
    var pendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }

    val pendingDelete = pendingDeleteId?.let { id ->
        state.moons.firstOrNull { it.moonWorldId == id }
    }
    // 宽屏下把添加入口移到顶栏，避免悬浮按钮遮挡卡片。
    val isWideLayout = LocalConfiguration.current.screenWidthDp >= WIDE_LAYOUT_MIN_WIDTH_DP

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ZerotierTopBar(
                title = stringResource(R.string.moon_orbit_activity),
                onBack = onBack,
                actions = {
                    if (isWideLayout && state.moons.isNotEmpty()) {
                        IconButton(onClick = { providerSheetVisible = true }) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.cd_add_moon)
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!isWideLayout && state.moons.isNotEmpty()) {
                FloatingActionButton(onClick = { providerSheetVisible = true }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.cd_add_moon)
                    )
                }
            }
        }
    ) { padding ->
        when {
            state.isLoading && state.moons.isEmpty() -> LoadingState(
                text = stringResource(R.string.loading),
                modifier = Modifier.padding(padding)
            )
            state.moons.isEmpty() -> EmptyState(
                icon = ZeroTierIcons.Moon,
                title = stringResource(R.string.moon_empty_title),
                description = stringResource(R.string.moon_empty_message),
                actionLabel = stringResource(R.string.add),
                onAction = { providerSheetVisible = true },
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
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
                items(
                    items = state.moons,
                    key = { it.moonWorldId }
                ) { moon ->
                    MoonCard(
                        moon = moon,
                        onCopy = { onCopyWorldId(moon) },
                        onDelete = { pendingDeleteId = moon.moonWorldId },
                        onDeleteCache = { onDeleteCache(moon) }
                    )
                }
            }
        }
    }

    if (providerSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { providerSheetVisible = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.import_moon_node),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                ActionRow(
                    title = stringResource(R.string.moon_manual_add),
                    icon = Icons.Outlined.Edit,
                    onClick = {
                        providerSheetVisible = false
                        worldIdText = ""
                        seedText = ""
                        orbitDialogVisible = true
                    }
                )
                ActionRow(
                    title = stringResource(R.string.moon_import_file_action),
                    icon = Icons.Outlined.Share,
                    onClick = {
                        providerSheetVisible = false
                        onPickFile()
                    }
                )
            }
        }
    }

    if (orbitDialogVisible) {
        val canAdd = worldIdText.length == MOON_ADDRESS_LENGTH &&
            seedText.length == MOON_ADDRESS_LENGTH
        AlertDialog(
            onDismissRequest = { orbitDialogVisible = false },
            title = { Text(stringResource(R.string.moon_orbit_info)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.label_moon_world_id))
                    OutlinedTextField(
                        value = worldIdText,
                        onValueChange = { worldIdText = filterHex(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = worldIdText.isNotEmpty() &&
                            worldIdText.length < MOON_ADDRESS_LENGTH,
                        supportingText = if (worldIdText.isNotEmpty() &&
                            worldIdText.length < MOON_ADDRESS_LENGTH
                        ) {
                            {
                                Text(
                                    stringResource(
                                        R.string.network_id_remaining,
                                        MOON_ADDRESS_LENGTH - worldIdText.length
                                    )
                                )
                            }
                        } else {
                            null
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Text(
                        text = stringResource(R.string.moon_world_id_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(stringResource(R.string.label_moon_seed))
                    OutlinedTextField(
                        value = seedText,
                        onValueChange = { seedText = filterHex(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = seedText.isNotEmpty() &&
                            seedText.length < MOON_ADDRESS_LENGTH,
                        supportingText = if (seedText.isNotEmpty() &&
                            seedText.length < MOON_ADDRESS_LENGTH
                        ) {
                            {
                                Text(
                                    stringResource(
                                        R.string.network_id_remaining,
                                        MOON_ADDRESS_LENGTH - seedText.length
                                    )
                                )
                            }
                        } else {
                            null
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Text(
                        text = stringResource(R.string.moon_seed_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = canAdd,
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
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (pendingDelete != null) {
        val worldId = java.lang.Long.toHexString(pendingDelete.moonWorldId)
        ConfirmDialog(
            title = stringResource(R.string.moon_delete_confirm_title),
            text = stringResource(R.string.moon_delete_confirm_message, worldId),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                onRemove(pendingDelete)
                pendingDeleteId = null
            },
            onDismiss = { pendingDeleteId = null }
        )
    }
}

/**
 * 单个 Moon 卡片。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MoonCard(
    moon: MoonOrbitDto,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onDeleteCache: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val worldId = java.lang.Long.toHexString(moon.moonWorldId)
    val seed = if (moon.isFromFile) DASH else java.lang.Long.toHexString(moon.moonSeed)
    val sourceLabel: String
    val sourceTone: StatusTone
    when {
        moon.isFromFile -> {
            sourceLabel = stringResource(R.string.external_file)
            sourceTone = StatusTone.Neutral
        }
        moon.isCacheFile -> {
            sourceLabel = stringResource(R.string.cached)
            sourceTone = StatusTone.Connected
        }
        else -> {
            sourceLabel = stringResource(R.string.wait_to_fetch)
            sourceTone = StatusTone.Connecting
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { menuExpanded = true },
                onLongClick = { menuExpanded = true }
            ),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    MonoText(
                        value = worldId,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    StatusPill(label = sourceLabel, tone = sourceTone)
                }
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
                            text = { Text(stringResource(R.string.copy_moon_world_id_to_clipboard)) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Share, contentDescription = null)
                            },
                            onClick = {
                                menuExpanded = false
                                onCopy()
                            }
                        )
                        if (moon.isCacheFile && !moon.isFromFile) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete_cache)) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Delete, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteCache()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.delete_moon_orbit),
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
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(
                label = stringResource(R.string.label_moon_seed),
                value = seed,
                monospace = true
            )
        }
    }
}

/**
 * 过滤 16 进制字符并限制长度。
 */
private fun filterHex(value: String): String =
    value.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
        .take(MOON_ADDRESS_LENGTH)

private const val MOON_ADDRESS_LENGTH = 10

private const val DASH = "-"

@Preview(name = "浅色", showBackground = true)
@Composable
private fun MoonOrbitScreenPreview() {
    ZerotierFixTheme {
        MoonOrbitScreen(
            state = previewState(),
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onPickFile = {},
            onAddOrbit = { _, _ -> },
            onRemove = {},
            onDeleteCache = {},
            onCopyWorldId = {}
        )
    }
}

@Preview(name = "深色", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MoonOrbitScreenDarkPreview() {
    ZerotierFixTheme {
        MoonOrbitScreen(
            state = previewState(),
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onPickFile = {},
            onAddOrbit = { _, _ -> },
            onRemove = {},
            onDeleteCache = {},
            onCopyWorldId = {}
        )
    }
}

@Preview(name = "空", showBackground = true)
@Composable
private fun MoonOrbitScreenEmptyPreview() {
    ZerotierFixTheme {
        MoonOrbitScreen(
            state = MoonOrbitUiState(moons = emptyList(), isLoading = false),
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onPickFile = {},
            onAddOrbit = { _, _ -> },
            onRemove = {},
            onDeleteCache = {},
            onCopyWorldId = {}
        )
    }
}

/**
 * 预览用状态。
 */
private fun previewState(): MoonOrbitUiState = MoonOrbitUiState(
    moons = listOf(
        MoonOrbitDto(0x8056c2e21cL, 0x8056c2e21cL, true, true),
        MoonOrbitDto(0x8056c2e21dL, 0x8056c2e21dL, false, false)
    ),
    isLoading = false
)
