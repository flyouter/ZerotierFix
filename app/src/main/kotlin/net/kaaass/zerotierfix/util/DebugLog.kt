package net.kaaass.zerotierfix.util

import android.util.Log

import net.kaaass.zerotierfix.BuildConfig

/**
 * 调试日志相关的工具函数，用于控制部分日志仅在调试模式下输出。
 */
object DebugLog {

    /**
     * 输出调试日志，仅在调试模式下生效。
     *
     * @param tag 日志标签
     * @param message 日志内容
     */
    @JvmStatic
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    /**
     * 输出信息日志，仅在调试模式下生效。
     *
     * @param tag 日志标签
     * @param message 日志内容
     */
    @JvmStatic
    fun i(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, message)
        }
    }
}
