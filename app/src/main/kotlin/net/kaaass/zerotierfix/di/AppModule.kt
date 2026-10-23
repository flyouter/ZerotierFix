package net.kaaass.zerotierfix.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

import net.kaaass.zerotierfix.service.AndroidVpnServiceCommands
import net.kaaass.zerotierfix.vpn.engine.VpnEngine
import net.kaaass.zerotierfix.vpn.engine.VpnEngineImpl
import net.kaaass.zerotierfix.vpn.engine.VpnServiceCommands

import javax.inject.Singleton

/**
 * VPN 引擎与命令接口的绑定。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    /** 绑定引擎门面。 */
    @Binds
    @Singleton
    abstract fun bindVpnEngine(impl: VpnEngineImpl): VpnEngine

    /** 绑定服务命令实现。 */
    @Binds
    @Singleton
    abstract fun bindVpnServiceCommands(
        impl: AndroidVpnServiceCommands
    ): VpnServiceCommands
}
