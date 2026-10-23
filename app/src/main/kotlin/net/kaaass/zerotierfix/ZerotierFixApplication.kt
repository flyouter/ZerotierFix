package net.kaaass.zerotierfix

import android.app.Application
import android.util.Log

import dagger.hilt.android.HiltAndroidApp

/**
 * 主程序入口。
 *
 * 依赖由 Hilt 注入；应用级单例（Repository、VpnStateStore、VpnEngine）由
 * `SingletonComponent` 持有。
 */
@HiltAndroidApp
class ZerotierFixApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i("Application", "Starting Application")
    }
}
