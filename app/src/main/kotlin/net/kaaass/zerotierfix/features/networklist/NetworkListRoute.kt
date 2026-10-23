package net.kaaass.zerotierfix.features.networklist

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.widget.Toast

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import net.kaaass.zerotierfix.R
import net.kaaass.zerotierfix.ui.common.resolve

/**
 * 网络列表页面的路由层：连接 ViewModel 与系统对话框 / 剪贴板。
 *
 * @param onAddNetwork 打开添加网络页
 * @param onOpenNetwork 打开网络详情页
 * @param onOpenSettings 打开设置页
 * @param onOpenMoon 打开 Moon 入轨页
 * @param onOpenPeers 打开 Peer 列表页
 * @param viewModel 页面 ViewModel
 */
@Composable
fun NetworkListRoute(
    onAddNetwork: () -> Unit,
    onOpenNetwork: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMoon: () -> Unit,
    onOpenPeers: () -> Unit,
    viewModel: NetworkListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingNetworkId by rememberSaveable { mutableStateOf<Long?>(null) }

    val vpnConsentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val networkId = pendingNetworkId
        pendingNetworkId = null
        if (networkId != null) {
            viewModel.onVpnConsentResult(networkId, result.resultCode == Activity.RESULT_OK)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is NetworkListEffect.ShowToast -> showToast(
                    context,
                    effect.text.resolve(context)
                )
                is NetworkListEffect.RequestVpnConsent -> {
                    val intent = VpnService.prepare(context)
                    if (intent == null) {
                        viewModel.onVpnConsentResult(effect.networkId, true)
                    } else {
                        pendingNetworkId = effect.networkId
                        vpnConsentLauncher.launch(intent)
                    }
                }
                NetworkListEffect.OpenNotificationSettings ->
                    openNotificationSettings(context)
            }
        }
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    NetworkListScreen(
        state = state,
        onAddNetwork = onAddNetwork,
        onOpenNetwork = onOpenNetwork,
        onOpenSettings = onOpenSettings,
        onOpenMoon = onOpenMoon,
        onOpenPeers = onOpenPeers,
        onNetworkEnabledChange = viewModel::setNetworkEnabled,
        onDeleteNetwork = viewModel::deleteNetwork,
        onCopyNetworkId = { network ->
            val clipboard =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(
                ClipData.newPlainText(
                    context.getString(R.string.network_id),
                    network.networkIdStr ?: ""
                )
            )
            showToast(context, context.getString(R.string.text_copied))
        },
        onNotificationAlertOpenSettings = viewModel::openNotificationSettings,
        onNotificationAlertDontShowAgain = viewModel::disableNotificationAlert,
        onNotificationAlertDismiss = viewModel::dismissNotificationAlert
    )
}

/**
 * 显示短 Toast。
 */
private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

/**
 * 打开应用通知设置。
 */
private fun openNotificationSettings(context: Context) {
    val intent = Intent("android.settings.APP_NOTIFICATION_SETTINGS").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra("app_package", context.packageName)
        putExtra("app_uid", context.applicationInfo.uid)
        putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
    }
    context.startActivity(intent)
}
