package net.kaaass.zerotierfix.features.settings

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import net.kaaass.zerotierfix.ui.common.resolve

/**
 * 设置页面的路由层。
 *
 * @param onBack 返回上一页
 * @param viewModel 页面 ViewModel
 */
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.setPlanetFromFile(uri)
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
        onBack = onBack,
        onSetPlanetUseCustom = viewModel::setPlanetUseCustom,
        onOpenPlanetDialog = viewModel::openPlanetDialog,
        onDismissPlanetDialog = viewModel::dismissPlanetDialog,
        onSetPlanetFromFile = { filePicker.launch(arrayOf("*/*")) },
        onOpenUrlDialog = viewModel::openUrlDialog,
        onDismissUrlDialog = viewModel::dismissUrlDialog,
        onUrlChanged = viewModel::onUrlChanged,
        onSubmitUrl = { viewModel.setPlanetFromUrl(state.urlText) },
        onSetCellularDataAllowed = viewModel::setCellularDataAllowed,
        onSetIpv6Disabled = viewModel::setIpv6Disabled
    )
}
