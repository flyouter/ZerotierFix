package net.kaaass.zerotierfix.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
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
        when (controller.currentStatus()) {
            QuickTileStatus.UNAVAILABLE -> toast(R.string.quick_tile_toast_no_network)
            QuickTileStatus.CONNECTING -> {
                // 控制器空闲时表示服务正在启动，此时点击用于取消连接。
                if (!controller.isBusy) {
                    controller.disconnect()
                }
            }
            QuickTileStatus.ACTIVE -> controller.disconnect()
            QuickTileStatus.INACTIVE -> connectFromTile()
        }
    }

    /**
     * 处理未连接状态下的点击：选择目标网络并打开授权/连接页。
     *
     * 连接统一经由前台透明 Activity 发起：Android 12+ 禁止后台应用直接
     * `startService`，磁贴点击时进程可能仍被视为后台。
     */
    private fun connectFromTile() {
        if (!controller.isLoaded) {
            controller.refresh()
            toast(R.string.quick_tile_toast_loading)
            return
        }
        val targetNetworkId = controller.currentTargetNetworkId()
        if (targetNetworkId == null) {
            toast(R.string.quick_tile_toast_no_network)
            return
        }
        if (isLocked) {
            unlockAndRun { launchConsentActivity(targetNetworkId) }
        } else {
            launchConsentActivity(targetNetworkId)
        }
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
}
