package net.kaaass.zerotierfix.di

import javax.inject.Qualifier

/** IO 调度器。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** 默认后台调度器。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/** VPN 命令串行调度器。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class VpnCommandDispatcher
