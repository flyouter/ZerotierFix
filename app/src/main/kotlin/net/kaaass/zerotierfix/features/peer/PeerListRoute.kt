package net.kaaass.zerotierfix.features.peer

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
 * Peer 列表页面的路由层。
 *
 * @param onBack 返回上一页
 * @param viewModel 页面 ViewModel
 */
@Composable
fun PeerListRoute(
    onBack: () -> Unit,
    viewModel: PeerListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.load()
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
        onBack = onBack
    )
}
