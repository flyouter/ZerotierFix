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

        // 在 IO 上预热偏好与数据库，避免首次读取与首帧组合重叠。
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)
            runCatching { networkRepository.get().getAllNetworks() }
        }

        Log.i("Application", "Starting Application")
    }
}
