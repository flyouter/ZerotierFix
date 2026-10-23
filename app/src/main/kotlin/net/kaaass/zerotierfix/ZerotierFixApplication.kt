package net.kaaass.zerotierfix

import android.app.Application
import android.util.Log

import net.kaaass.zerotierfix.di.AppContainer

/**
 * 主程序入口。
 *
 * 持有应用级依赖容器；Activity、Service 通过 [appContainer] 获取
 * Repository 等数据访问能力。
 */
class ZerotierFixApplication : Application() {

    /** 应用级依赖容器，在 [onCreate] 中初始化。 */
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        Log.i("Application", "Starting Application")
        this.appContainer = AppContainer(this)
    }
}
