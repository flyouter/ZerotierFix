package net.kaaass.zerotierfix

import android.app.Application
import android.content.Context
import android.os.StrictMode
import android.util.Log

import dagger.hilt.android.HiltAndroidApp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

import net.kaaass.zerotierfix.data.repository.NetworkRepository
import net.kaaass.zerotierfix.service.QuickTileController
import net.kaaass.zerotierfix.service.ServiceLiveness
import net.kaaass.zerotierfix.service.VpnNotificationController
import net.kaaass.zerotierfix.service.ZeroTierOneService

import javax.inject.Inject
import javax.inject.Provider

/**
 * 主程序入口。
 *
 * 依赖由 Hilt 注入；应用级单例（Repository、VpnStateStore、VpnEngine）由
 * `SingletonComponent` 持有。
 */
@HiltAndroidApp
class ZerotierFixApplication : Application() {

    /** 延迟获取仓库，确保 Room 的构造与首次打开发生在 IO 线程。 */
    @Inject
    lateinit var networkRepository: Provider<NetworkRepository>

    /** 延迟获取磁贴控制器，构造它会连带打开数据库。 */
    @Inject
    lateinit var quickTileController: Provider<QuickTileController>

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .detectCustomSlowCalls()
                    .detectUnbufferedIo()
                    .penaltyLog()
                    .build()
            )
        }

        // 新进程里不可能存在仍在运行的 VPN 服务；清理旧版本或已结束进程
        // 遗留的常驻通知，避免用户看到“已连接”假象。
        if (!ServiceLiveness.isRunning(this, ZeroTierOneService::class.java)) {
            VpnNotificationController.cancelStale(this)
        }

        // 在 IO 上预热偏好与数据库，避免首次读取与首帧组合重叠。
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)
            runCatching { networkRepository.get().getAllNetworks() }
            // 磁贴控制器必须随进程常驻：磁贴状态的变化只能由进程内的
            // requestListeningState 推给 SystemUI。若等到磁贴首次绑定才创建，
            // App 内的连接/断开不会刷新 SystemUI 缓存的旧磁贴，退出 App 后
            // 磁贴仍可能显示“已连接”。
            runCatching {
                quickTileController.get().refresh()
            }.onFailure {
                Log.w("Application", "Failed to start quick tile controller", it)
            }
        }

        Log.i("Application", "Starting Application")
    }
}
