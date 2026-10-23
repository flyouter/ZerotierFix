package net.kaaass.zerotierfix.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

import net.kaaass.zerotierfix.data.repository.AppNodeRepository
import net.kaaass.zerotierfix.data.repository.AppNodeRepositoryImpl
import net.kaaass.zerotierfix.data.repository.MoonOrbitRepository
import net.kaaass.zerotierfix.data.repository.MoonOrbitRepositoryImpl
import net.kaaass.zerotierfix.data.repository.NetworkRepository
import net.kaaass.zerotierfix.data.repository.NetworkRepositoryImpl
import net.kaaass.zerotierfix.data.repository.SettingsRepository
import net.kaaass.zerotierfix.data.repository.SettingsRepositoryImpl

import javax.inject.Singleton

/**
 * Repository 接口与实现的绑定。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /** 绑定网络仓库。 */
    @Binds
    @Singleton
    abstract fun bindNetworkRepository(impl: NetworkRepositoryImpl): NetworkRepository

    /** 绑定 Moon 入轨仓库。 */
    @Binds
    @Singleton
    abstract fun bindMoonOrbitRepository(impl: MoonOrbitRepositoryImpl): MoonOrbitRepository

    /** 绑定本机节点仓库。 */
    @Binds
    @Singleton
    abstract fun bindAppNodeRepository(impl: AppNodeRepositoryImpl): AppNodeRepository

    /** 绑定设置仓库。 */
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
