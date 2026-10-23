package net.kaaass.zerotierfix.di

import android.content.Context
import android.content.SharedPreferences

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

import net.kaaass.zerotierfix.data.database.AppDatabase

import javax.inject.Singleton

/**
 * 数据库与偏好的 Hilt 提供者。
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * 提供应用级 Room 数据库。
     *
     * @param context 应用上下文
     * @return 数据库单例
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getInstance(context)

    /**
     * 提供默认 SharedPreferences。
     *
     * 名称与 `PreferenceManager.getDefaultSharedPreferences` 保持一致。
     *
     * @param context 应用上下文
     * @return 默认偏好
     */
    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
}
