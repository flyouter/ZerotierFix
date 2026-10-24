package net.kaaass.zerotierfix.features.peer

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import kotlinx.coroutines.launch

import net.kaaass.zerotierfix.ui.common.resolve

/**
 * Peer 列表页面的路由层。
 *
 * @param viewModel 页面 ViewModel
 */
@Composable
fun PeerListRoute(
    viewModel: PeerListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        // 先启动收集再请求，避免首次请求的错误提示丢失
        launch { viewModel.load() }
        viewModel.effects.collect { effect ->
            when (effect) {
                is PeerListEffect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.text.resolve(context))
            }
        }
    }

    PeerListScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry
    )
}
