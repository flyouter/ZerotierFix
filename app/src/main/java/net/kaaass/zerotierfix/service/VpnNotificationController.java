package net.kaaass.zerotierfix.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import net.kaaass.zerotierfix.R;
import net.kaaass.zerotierfix.features.networklist.NetworkListActivity;
import net.kaaass.zerotierfix.util.Constants;

/**
 * 构建并显示 VPN 连接状态通知。
 */
public final class VpnNotificationController {

    private static final int NOTIFICATION_TAG = 5919812;

    private final VpnService service;

    private NotificationManager notificationManager;

    /**
     * 创建通知控制器。
     *
     * @param service 宿主 VPN 服务，不能为空
     */
    public VpnNotificationController(VpnService service) {
        this.service = service;
    }

    /**
     * 显示已连接通知。
     *
     * @param networkIdStr 网络 ID 的字符串形式
     */
    public void showConnected(String networkIdStr) {
        NotificationManager manager = getNotificationManager();
        createChannelIfNeeded(manager);
        var notification = new NotificationCompat.Builder(this.service, Constants.CHANNEL_ID)
                .setPriority(1)
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(this.service.getString(R.string.notification_title_connected))
                .setContentText(this.service.getString(
                        R.string.notification_text_connected, networkIdStr))
                .setColor(ContextCompat.getColor(
                        this.service.getApplicationContext(), R.color.zerotier_orange))
                .setContentIntent(createContentIntent())
                .build();
        manager.notify(NOTIFICATION_TAG, notification);
    }

    /**
     * 取消已显示的通知。
     */
    public void cancel() {
        if (this.notificationManager != null) {
            this.notificationManager.cancel(NOTIFICATION_TAG);
        }
    }

    /**
     * 获取通知管理器。
     *
     * @return 通知管理器，非空
     */
    private NotificationManager getNotificationManager() {
        if (this.notificationManager == null) {
            this.notificationManager = (NotificationManager) this.service
                    .getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return this.notificationManager;
    }

    /**
     * 在 Android 8.0 及以上创建通知渠道。
     *
     * @param manager 通知管理器
     */
    private void createChannelIfNeeded(NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelName = this.service.getString(R.string.channel_name);
            String description = this.service.getString(R.string.channel_description);
            var channel = new NotificationChannel(
                    Constants.CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(description);
            manager.createNotificationChannel(channel);
        }
    }

    /**
     * 创建点击通知后打开网络列表的 PendingIntent。
     *
     * @return PendingIntent
     */
    private PendingIntent createContentIntent() {
        int pendingIntentFlag = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 31) {
            pendingIntentFlag |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(
                this.service,
                0,
                new Intent(this.service, NetworkListActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                                | Intent.FLAG_ACTIVITY_CLEAR_TOP),
                pendingIntentFlag
        );
    }
}
