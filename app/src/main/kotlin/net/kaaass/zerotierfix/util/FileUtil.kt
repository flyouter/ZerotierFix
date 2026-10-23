package net.kaaass.zerotierfix.util

import android.content.Context

import java.io.File

/**
 * 文件工具类。
 */
object FileUtil {

    /**
     * 获取临时文件。
     *
     * @param context 上下文
     * @return 缓存目录下的临时文件
     */
    @JvmStatic
    fun tempFile(context: Context): File = File(context.cacheDir, Constants.FILE_TEMP)

    /**
     * 清理临时文件。
     *
     * @param context 上下文
     */
    @JvmStatic
    fun clearTempFile(context: Context) {
        val temp = tempFile(context)
        if (temp.exists()) {
            temp.delete()
        }
    }
}
