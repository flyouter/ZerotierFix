package net.kaaass.zerotierfix.util

import java.math.BigInteger

/**
 * 网络号处理工具类。
 */
object NetworkIdUtils {

    /**
     * 将 16 进制字符串转换为网络 ID。
     *
     * @param str 16 进制字符串
     * @return 网络 ID
     * @throws NumberFormatException 字符串不是合法的 16 进制数时抛出
     */
    @JvmStatic
    fun hexStringToLong(str: String): Long = BigInteger(str, 16).toLong()
}
