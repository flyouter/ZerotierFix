package net.kaaass.zerotierfix.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast

import dagger.hilt.android.AndroidEntryPoint

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

import net.kaaass.zerotierfix.R
import net.kaaass.zerotierfix.vpn.engine.QuickTileState
import net.kaaass.zerotierfix.vpn.engine.QuickTileStatus

import javax.inject.Inject

/**
 * ZeroTier 快捷设置磁贴。
 *
 * 只负责渲染状态与分发点击；选网、连接、断开与防抖由 [QuickTileController]
 * 在应用级作用域中完成，因此磁贴被系统解绑后操作仍能继续。
 *
 * 没有已添加网络时使用 `STATE_INACTIVE` 而不是 `STATE_UNAVAILABLE`：
 * SystemUI 会让 `STATE_UNAVAILABLE` 的磁贴不可点击，无法满足“点击提示添加网络”的需求。
 */
@AndroidEntryPoint
class ZeroTierTileService : TileService() {

    @Inject
    lateinit var controller: QuickTileController

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateJob: Job? = null

    override fun onTileAdded() {
        super.onTileAdded()
        controller.refresh()
    }

    override fun onStartListening() {
        super.onStartListening()
        controller.refresh()
        stateJob?.cancel()
        stateJob = scope.launch {
            controller.state.collect { renderTile(it) }
        }
    }

    override fun onStopListening() {
        stateJob?.cancel()
        stateJob = null
        super.onStopListening()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onClick() {
        super.onClick()
        if (controller.currentStatus() == QuickTileStatus.UNAVAILABLE) {
            toast(R.string.quick_tile_toast_no_network)
            return
        }
        if (!controller.isLoaded) {
            controller.refresh()
            toast(R.string.quick_tile_toast_loading)
            return
        }

        // 点击只翻转期望状态并立即更新磁贴；实际启停由后台收敛循环完成。
        val wantConnected = controller.toggle()
        if (!wantConnected) {
            // 断开不需要前台页面，直接在后台收敛。
            controller.startSettle()
            return
        }

        val targetNetworkId = controller.currentTargetNetworkId()
        if (targetNetworkId == null) {
            controller.abandonIntent()
            toast(R.string.quick_tile_toast_no_network)
            return
        }
        if (isLocked) {
            unlockAndRun { launchForConnect(targetNetworkId) }
        } else {
            launchForConnect(targetNetworkId)
        }
    }

    /**
     * 启动前台页面驱动连接；连接需要保持前台才能通过系统的后台启动限制。
     */
    private fun launchForConnect(networkId: Long) {
        try {
            if (VpnService.prepare(this) == null) {
                // 已授权：普通 startActivity 让透明页留在通知栏后面完成服务启动，
                // 磁贴面板保持展开；startActivityAndCollapse 会收起面板并跳到桌面。
                launchConnectActivity(networkId)
            } else {
                launchConsentActivity(networkId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch connect activity", e)
            controller.abandonIntent()
            toast(R.string.toast_connect_failed)
        }
    }

    /**
     * 在通知栏后面启动透明连接页，不收起快捷设置面板。
     *
     * 普通 `startActivity` 依赖 SystemUI 绑定 TileService 时授予的后台 Activity
     * 启动豁免；Android 14+ 收紧了该限制，只能回退到系统提供的
     * `startActivityAndCollapse`（面板会被收起）。
     */
    private fun launchConnectActivity(networkId: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            launchConsentActivity(networkId)
            return
        }
        startActivity(
            Intent(this, VpnConsentActivity::class.java).apply {
                putExtra(VpnConsentActivity.EXTRA_NETWORK_ID, networkId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    /**
     * 启动透明授权页，完成系统 VPN 授权后自动连接。
     */
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun launchConsentActivity(networkId: Long) {
        val intent = Intent(this, VpnConsentActivity::class.java).apply {
            putExtra(VpnConsentActivity.EXTRA_NETWORK_ID, networkId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // API 34 起 Intent 重载已废弃且对 targetSdk 34+ 会抛异常。
            val pendingIntent = PendingIntent.getActivity(
                this,
                networkId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    /**
     * 将控制器状态渲染到磁贴。
     */
    private fun renderTile(state: QuickTileState) {
        val tile = qsTile ?: return

        val subtitleRes = when (state.status) {
            QuickTileStatus.UNAVAILABLE -> R.string.quick_tile_subtitle_no_network
            QuickTileStatus.INACTIVE -> R.string.quick_tile_subtitle_disconnected
            QuickTileStatus.CONNECTING -> R.string.quick_tile_subtitle_connecting
            QuickTileStatus.ACTIVE -> R.string.quick_tile_subtitle_connected
        }
        val subtitle = getString(subtitleRes)

        tile.label = getString(R.string.quick_tile_label)
        tile.subtitle = subtitle
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            tile.stateDescription = subtitle
        }
        tile.contentDescription = getString(
            R.string.quick_tile_content_description,
            subtitle
        )
        tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_zerotier)
        tile.state = when (state.status) {
            QuickTileStatus.UNAVAILABLE, QuickTileStatus.INACTIVE -> Tile.STATE_INACTIVE
            QuickTileStatus.CONNECTING, QuickTileStatus.ACTIVE -> Tile.STATE_ACTIVE
        }
        tile.updateTile()
    }

    private fun toast(messageRes: Int) {
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "ZeroTierTileService"
    }
}
