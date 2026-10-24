package net.kaaass.zerotierfix.features.join

import android.widget.Toast

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
 * 添加网络页面的路由层。
 *
 * @param onBack 返回上一页
 * @param viewModel 页面 ViewModel
 */
@Composable
fun JoinNetworkRoute(
    onBack: () -> Unit,
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

    JoinNetworkScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onNetworkIdChanged = viewModel::onNetworkIdChanged,
        onRouteViaZeroTierChanged = viewModel::onRouteViaZeroTierChanged,
        onDnsModeChanged = viewModel::onDnsModeChanged,
        onDnsChanged = viewModel::onDnsChanged,
        onJoin = viewModel::join,
        onBack = onBack
    )
}
