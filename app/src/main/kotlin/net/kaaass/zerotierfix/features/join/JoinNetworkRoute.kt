package net.kaaass.zerotierfix.features.join

import android.widget.Toast

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import net.kaaass.zerotierfix.R
import net.kaaass.zerotierfix.navigation.ZeroTierRoutes
import net.kaaass.zerotierfix.ui.common.resolve

/**
 * 添加网络页面的路由层。
 *
 * @param onBack 返回上一页
 * @param onScanRequested 打开扫码页
 * @param savedStateHandle 页面状态句柄，用于接收扫码结果
 * @param viewModel 页面 ViewModel
 */
@Composable
fun JoinNetworkRoute(
    onBack: () -> Unit,
    onScanRequested: () -> Unit,
    savedStateHandle: SavedStateHandle,
    viewModel: JoinNetworkViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is JoinNetworkEffect.ShowToast ->
                    Toast.makeText(context, effect.text.resolve(context), Toast.LENGTH_SHORT).show()
                is JoinNetworkEffect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.text.resolve(context))
                JoinNetworkEffect.NavigateBack -> onBack()
            }
        }
    }

    val scanResult by savedStateHandle
        .getStateFlow<String?>(ZeroTierRoutes.SCAN_RESULT_KEY, null)
        .collectAsStateWithLifecycle()

    LaunchedEffect(scanResult) {
        val networkId = scanResult ?: return@LaunchedEffect
        savedStateHandle[ZeroTierRoutes.SCAN_RESULT_KEY] = null
        val applied = viewModel.onScanResult(networkId)
        snackbarHostState.showSnackbar(
            context.getString(
                if (applied) R.string.scan_result_applied else R.string.scan_invalid_qr
            )
        )
    }

    JoinNetworkScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onNetworkIdChanged = viewModel::onNetworkIdChanged,
        onRouteViaZeroTierChanged = viewModel::onRouteViaZeroTierChanged,
        onDnsModeChanged = viewModel::onDnsModeChanged,
        onDnsChanged = viewModel::onDnsChanged,
        onJoin = viewModel::join,
        onScanClick = onScanRequested,
        onBack = onBack
    )
}
