package net.kaaass.zerotierfix.service

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.VpnService
import android.os.Build
import android.os.Bundle
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

    override fun onCreate() {
        super.onCreate()
        // 点击后 SystemUI 会立刻回调 onStopListening（面板却可能保持展开），
        // 若此时停止收集状态，磁贴将一直停留在点击前的旧状态。
        // 因此状态收集跟随服务实例存活，直到 onDestroy 才取消。
        startStateCollection()
    }

    override fun onStartListening() {
        super.onStartListening()
        Log.d(TAG, "onStartListening")
        controller.refresh()
        startStateCollection()
        // 收集器可能在 qsTile 尚未就绪时就已经发出过最新状态，这里补一次渲染，
        // 保证磁贴一进入监听就显示当前状态。
        renderTile(controller.state.value)
    }

    override fun onStopListening() {
        Log.d(TAG, "onStopListening")
        // 不取消收集：面板仍展开时需要继续把 CONNECTING/ACTIVE 推给磁贴。
        super.onStopListening()
    }

    private fun startStateCollection() {
        if (stateJob?.isActive == true) return
        stateJob = scope.launch {
            controller.state.collect { renderTile(it) }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onClick() {
        super.onClick()
        // onClick 由 SystemUI 同步调用；等待加载、启动服务等放到应用级作用域，
        // 既不阻塞 Binder 回调，也不会因磁贴被解绑而中断。
        scope.launch {
            handleClick()
        }
    }

    /**
     * 处理一次磁贴点击。
     *
     * 已授权时直接在后台启动服务，不启动 Activity，因此快捷设置面板保持展开；
     * 只有首次 VPN 授权、授权被撤销，或系统拒绝后台启动服务时才回退到 Activity。
     */
    private suspend fun handleClick() {
        if (!controller.isLoaded) {
            // 冷启动时 onClick 往往早于 refresh 完成；有界等待避免丢掉第一次点击。
            controller.awaitLoaded()
        }
        if (!controller.isLoaded) {
            toast(R.string.quick_tile_toast_loading)
            return
        }
        // 点击前先校准真实状态：服务被系统杀死后进程内状态可能残留 RUNNING，
        // 直接用缓存状态会执行错误的断开操作。
        controller.reconcileServiceState()
        if (controller.currentStatus() == QuickTileStatus.UNAVAILABLE) {
            toast(R.string.quick_tile_toast_no_network)
            return
        }

        // 点击只翻转期望状态并立即更新磁贴；实际启停由后台收敛循环完成。
        val wantConnected = controller.toggle()
        val targetNetworkId = controller.currentTargetNetworkId()
        Log.d(
            TAG,
            "click: sdk=${Build.VERSION.SDK_INT} locked=$isLocked " +
                "loaded=${controller.isLoaded} wantConnected=$wantConnected " +
                "target=$targetNetworkId status=${controller.currentStatus()}"
        )
        if (!wantConnected) {
            // 断开不需要前台页面，直接在后台收敛。
            controller.startSettle()
            return
        }

        if (targetNetworkId == null) {
            controller.abandonIntent()
            toast(R.string.quick_tile_toast_no_network)
            return
        }

        val consentNeeded = try {
            VpnService.prepare(this@ZeroTierTileService) != null
        } catch (e: Exception) {
            Log.e(TAG, "VpnService.prepare failed", e)
            true
        }
        Log.d(TAG, "click: consentNeeded=$consentNeeded")
        // 已授权：完全后台连接，快捷设置面板保持展开。
        if (!consentNeeded && controller.connectFromTile(targetNetworkId)) {
            Log.d(TAG, "click: direct connect accepted, no activity launched")
            return
        }

        // 回退路径：首次授权/授权被撤销，或 OEM 拒绝后台启动服务。
        Log.d(TAG, "click: falling back to activity, consentNeeded=$consentNeeded")
        if (isLocked) {
            unlockAndRun { launchActivity(targetNetworkId) }
        } else {
            launchActivity(targetNetworkId)
        }
    }

    /**
     * 通过透明 Activity 完成 VPN 授权或连接兜底。
     *
     * Android 14+ 从磁贴启动 Activity 必须收起面板；该路径只用于无法后台
     * 启动服务的场景。
     */
    private fun launchActivity(networkId: Long) {
        val intent = Intent(this, VpnConsentActivity::class.java).apply {
            putExtra(VpnConsentActivity.EXTRA_NETWORK_ID, networkId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // API 34 起 Intent 重载已废弃且对 targetSdk 34+ 会抛异常。
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    networkId.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    createCreatorBalOptions()
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                // 低版本没有 PendingIntent 重载，分支在运行期已排除 34+。
                @SuppressLint("StartActivityAndCollapseDeprecated")
                startActivityAndCollapse(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch connect activity", e)
            controller.abandonIntent()
            toast(R.string.toast_connect_failed)
        }
    }

    /**
     * Android 15+ 创建 PendingIntent 时默认不再出借后台启动权限；
     * 磁贴需要显式 opt-in，否则系统可能静默拦截 Activity 启动。
     *
     * @return ActivityOptions Bundle；低版本返回 `null`
     */
    private fun createCreatorBalOptions(): Bundle? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return null
        }
        return ActivityOptions.makeBasic().apply {
            setPendingIntentCreatorBackgroundActivityStartMode(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            )
        }.toBundle()
    }

    /**
     * 将控制器状态渲染到磁贴。
     */
    private fun renderTile(state: QuickTileState) {
        val tile = qsTile
        Log.d(
            TAG,
            "renderTile status=${state.status} desired=${state.desiredConnected} " +
                "tile=${tile != null}"
        )
        if (tile == null) return

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
        tile.icon = Icon.createWithResource(this, R.drawable.ic_zerotier_symbol)
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
