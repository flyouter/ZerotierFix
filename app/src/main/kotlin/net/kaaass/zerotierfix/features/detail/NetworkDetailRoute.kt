package net.kaaass.zerotierfix.features.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 网络详情页面的路由层。
 *
 * @param onBack 返回上一页
 * @param viewModel 页面 ViewModel
 */
@Composable
fun NetworkDetailRoute(
    onBack: () -> Unit,
    viewModel: NetworkDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    NetworkDetailScreen(
        state = state,
        onRouteViaZeroTierChanged = viewModel::onRouteViaZeroTierChanged,
        onRetry = viewModel::retry,
        onBack = onBack
    )
}
