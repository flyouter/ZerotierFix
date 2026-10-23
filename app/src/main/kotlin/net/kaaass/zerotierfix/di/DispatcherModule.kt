package net.kaaass.zerotierfix.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * 协程调度器提供者。
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    /** 提供 IO 调度器。 */
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /** 提供默认后台调度器。 */
    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    /**
     * 提供 VPN 命令串行调度器。
     *
     * 单并发保证 leave → stop → start、reconfigure 不会并发执行。
     */
    @Provides
    @VpnCommandDispatcher
    fun provideVpnCommandDispatcher(): CoroutineDispatcher =
        Dispatchers.IO.limitedParallelism(1)
}
