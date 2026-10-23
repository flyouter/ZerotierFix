package net.kaaass.zerotierfix.features.moon

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import net.kaaass.zerotierfix.R
import net.kaaass.zerotierfix.model.dto.MoonOrbitDto
import net.kaaass.zerotierfix.ui.common.resolve

/**
 * Moon 入轨页面的路由层。
 *
 * @param onBack 返回上一页
 * @param viewModel 页面 ViewModel
 */
@Composable
fun MoonOrbitRoute(
    onBack: () -> Unit,
    viewModel: MoonOrbitViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importFromFile(uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MoonOrbitEffect.ShowToast ->
                    Toast.makeText(context, effect.text.resolve(context), Toast.LENGTH_SHORT).show()
                is MoonOrbitEffect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.text.resolve(context))
            }
        }
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    MoonOrbitScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onPickFile = { filePicker.launch(arrayOf("*/*")) },
        onAddOrbit = viewModel::addOrbitFromInput,
        onRemove = viewModel::remove,
        onDeleteCache = viewModel::deleteCacheFile,
        onCopyWorldId = { moon -> copyWorldId(context, moon) }
    )
}

/**
 * 复制 Moon 地址并提示。
 */
private fun copyWorldId(context: Context, moon: MoonOrbitDto) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(
        ClipData.newPlainText(
            context.getString(R.string.moon_world_id),
            java.lang.Long.toHexString(moon.moonWorldId)
        )
    )
    Toast.makeText(context, R.string.text_copied, Toast.LENGTH_SHORT).show()
}
