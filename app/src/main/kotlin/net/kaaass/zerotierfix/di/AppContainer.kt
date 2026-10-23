package net.kaaass.zerotierfix.di

import android.content.Context

import net.kaaass.zerotierfix.data.database.AppDatabase
import net.kaaass.zerotierfix.data.repository.AppNodeRepository
import net.kaaass.zerotierfix.data.repository.AppNodeRepositoryImpl
import net.kaaass.zerotierfix.data.repository.MoonOrbitRepository
import net.kaaass.zerotierfix.data.repository.MoonOrbitRepositoryImpl
import net.kaaass.zerotierfix.data.repository.NetworkRepository
import net.kaaass.zerotierfix.data.repository.NetworkRepositoryImpl

/**
 * 应用级依赖容器（手动依赖注入）。
 *
 * 集中创建 Room 数据库与 Repository 实例，避免各页面和服务重复初始化。
 * 生命周期与 [android.app.Application] 一致。
 */
class AppContainer(context: Context) {

    /** 应用上下文。 */
    private val appContext = context.applicationContext

    /** 应用级 Room 数据库。 */
    private val database = AppDatabase.getInstance(appContext)

    /** 网络数据仓库。 */
    val networkRepository: NetworkRepository = NetworkRepositoryImpl(database)

    /** Moon 入轨仓库。 */
    val moonOrbitRepository: MoonOrbitRepository =
        MoonOrbitRepositoryImpl(database, appContext)

    /** 本机节点信息仓库。 */
    val appNodeRepository: AppNodeRepository = AppNodeRepositoryImpl(database)
}
