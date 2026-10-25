package net.kaaass.zerotierfix.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.util.Log

import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

import net.kaaass.zerotierfix.R
import net.kaaass.zerotierfix.MainActivity
import net.kaaass.zerotierfix.util.Constants

/**
 * 构建并显示 VPN 连接状态通知。
 *
 * 通知同时作为前台服务通知使用：服务被销毁或进程结束时由系统移除，
 * 不再依赖 `cancel()` 是否被调用到。
 */
class VpnNotificationController(private val service: VpnService) {

    /** 是否已通过 `startForeground` 进入前台。 */
    private var foregroundStarted = false

    /**
     * 构建并显示连接通知。
     *
     * 正文优先显示网络名称，名称为空时回退到网络 ID，
     * 保证通知栏始终能看出当前连接的是哪个网络。
     *
     * 线程：可在任意线程调用。
     *
     * @param networkIdStr 网络 ID 的字符串形式；可能为 `null`
     * @param networkName 网络名称；可能为 `null` 或空字符串
     * @return 通知对象
     */
    fun buildConnected(networkIdStr: String?, networkName: String?): Notification {
        val manager = getNotificationManager()
        createChannelIfNeeded(manager)
        val displayName = networkName?.takeIf { it.isNotBlank() } ?: networkIdStr
        return NotificationCompat.Builder(this.service, Constants.CHANNEL_ID)
            .setPriority(1)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(
                NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
            )
            .setSmallIcon(R.drawable.ic_zerotier_symbol)
            .setContentTitle(this.service.getString(R.string.notification_title_connected))
            .setContentText(
                this.service.getString(R.string.notification_text_connected, displayName)
            )
            .setColor(
                ContextCompat.getColor(
                    this.service.applicationContext, R.color.zerotier_orange
                )
            )
            .setContentIntent(createContentIntent())
            .build()
    }

    /**
     * 构建连接通知并进入前台服务。
     *
     * 先构建通知再尝试 `startForeground`；失败时只记录日志，不退回普通通知 ——
     * 普通通知无法随服务/进程结束被系统移除。
     *
     * 线程：可在任意线程调用。
     *
     * @param networkIdStr 网络 ID 的字符串形式；可能为 `null`
     * @param networkName 网络名称；可能为 `null` 或空字符串
     */
    fun enterForeground(networkIdStr: String?, networkName: String?) {
        val notification = buildConnected(networkIdStr, networkName)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                this.service.startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
                )
            } else {
                this.service.startForeground(NOTIFICATION_ID, notification)
            }
            this.foregroundStarted = true
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed", e)
            this.foregroundStarted = false
        }
    }

    /**
     * 退出前台并移除通知。
     *
     * 幂等：`startForeground` 未成功时也会按 ID 取消，避免旧通知残留。
     *
     * 线程：可在任意线程调用。
     */
    fun leaveForeground() {
        if (this.foregroundStarted) {
            try {
                this.service.stopForeground(Service.STOP_FOREGROUND_REMOVE)
            } catch (e: Exception) {
                Log.e(TAG, "stopForeground failed", e)
            }
        }
        this.foregroundStarted = false
        cancel()
    }

    /**
     * 取消已显示的通知。
     *
     * 每次都重新获取 [NotificationManager]，不依赖任何缓存字段，
     * 保证服务实例重建后也能清理上一个实例留下的通知。
     */
    fun cancel() {
        getNotificationManager().cancel(NOTIFICATION_ID)
    }

    /**
     * 获取通知管理器。
     *
     * @return 通知管理器，非空
     */
    private fun getNotificationManager(): NotificationManager =
        this.service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * 在 Android 8.0 及以上创建通知渠道。
     *
     * @param manager 通知管理器
     */
    private fun createChannelIfNeeded(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = this.service.getString(R.string.channel_name)
            val description = this.service.getString(R.string.channel_description)
            val channel = NotificationChannel(
                Constants.CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = description
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * 创建点击通知后打开网络列表的 PendingIntent。
     *
     * @return PendingIntent
     */
    private fun createContentIntent(): PendingIntent {
        var pendingIntentFlag = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= 31) {
            pendingIntentFlag = pendingIntentFlag or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getActivity(
            this.service,
            0,
            Intent(this.service, MainActivity::class.java)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                        or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        or Intent.FLAG_ACTIVITY_CLEAR_TOP
                ),
            pendingIntentFlag
        )
    }

    companion object {
        /** 连接状态通知 ID，同时作为前台服务通知 ID。 */
        const val NOTIFICATION_ID = 5919812

        private const val TAG = "VpnNotification"

        /**
         * 清理可能由旧版本或已结束进程遗留的通知。
         *
         * @param context 任意 Context
         */
        fun cancelStale(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as? NotificationManager ?: return
            manager.cancel(NOTIFICATION_ID)
        }
    }
}
