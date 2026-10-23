package net.kaaass.zerotierfix.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build

import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

import net.kaaass.zerotierfix.R
import net.kaaass.zerotierfix.MainActivity
import net.kaaass.zerotierfix.util.Constants

/**
 * 构建并显示 VPN 连接状态通知。
 */
class VpnNotificationController(private val service: VpnService) {

    private var notificationManager: NotificationManager? = null

    /**
     * 显示已连接通知。
     *
     * @param networkIdStr 网络 ID 的字符串形式；可能为 `null`
     */
    fun showConnected(networkIdStr: String?) {
        val manager = getNotificationManager()
        createChannelIfNeeded(manager)
        val notification = NotificationCompat.Builder(this.service, Constants.CHANNEL_ID)
            .setPriority(1)
            .setOngoing(true)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(this.service.getString(R.string.notification_title_connected))
            .setContentText(
                this.service.getString(R.string.notification_text_connected, networkIdStr)
            )
            .setColor(
                ContextCompat.getColor(
                    this.service.applicationContext, R.color.zerotier_orange
                )
            )
            .setContentIntent(createContentIntent())
            .build()
        manager.notify(NOTIFICATION_TAG, notification)
    }

    /**
     * 取消已显示的通知。
     */
    fun cancel() {
        this.notificationManager?.cancel(NOTIFICATION_TAG)
    }

    /**
     * 获取通知管理器。
     *
     * @return 通知管理器，非空
     */
    private fun getNotificationManager(): NotificationManager {
        var manager = this.notificationManager
        if (manager == null) {
            manager = this.service
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            this.notificationManager = manager
        }
        return manager
    }

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
        private const val NOTIFICATION_TAG = 5919812
    }
}
