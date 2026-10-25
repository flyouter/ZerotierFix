package net.kaaass.zerotierfix.features.settings

import android.widget.Toast

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

import net.kaaass.zerotierfix.ui.common.resolve

/**
 * 设置页面的路由层。
 *
 * @param viewModel 页面 ViewModel
 */
@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    // 设置页是常驻页面，且状态源只是 ViewModel 的 StateFlow，这里不按导航条目的
    // 生命周期收集：预测性返回后条目的生命周期可能停留在非活跃状态，会导致开关
    // 点击后界面不再刷新。
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.setPlanetFromFile(uri)
        } else {
            viewModel.onFilePickCancelled()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsEffect.ShowToast ->
                    Toast.makeText(context, effect.text.resolve(context), Toast.LENGTH_LONG).show()
                is SettingsEffect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.text.resolve(context))
            }
        }
    }

    SettingsScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onSetPlanetUseCustom = viewModel::setPlanetUseCustom,
        onOpenPlanetDialog = viewModel::openPlanetDialog,
        onDismissPlanetDialog = viewModel::dismissPlanetDialog,
        onSetPlanetFromFile = {
            viewModel.hidePlanetDialog()
            filePicker.launch(arrayOf("*/*"))
        },
        onOpenUrlDialog = viewModel::openUrlDialog,
        onDismissUrlDialog = viewModel::dismissUrlDialog,
        onUrlChanged = viewModel::onUrlChanged,
        onSubmitUrl = { viewModel.setPlanetFromUrl(state.urlText) },
        onSetCellularDataAllowed = viewModel::setCellularDataAllowed,
        onSetIpv6Disabled = viewModel::setIpv6Disabled
    )
}
