package net.kaaass.zerotierfix.util

import java.net.Inet6Address
import java.net.InetSocketAddress
import java.util.Locale

/**
 * 字符串处理工具类。
 */
object StringUtils {

    private const val VERSION_FORMAT = "%d.%d.%d"

    /**
     * 将版本号转为可读字符串。
     *
     * @param major 主版本号
     * @param minor 次版本号
     * @param revision 修订版本号
     * @return 可读字符串
     */
    @JvmStatic
    fun versionString(major: Int, minor: Int, revision: Int): String =
        String.format(Locale.ROOT, VERSION_FORMAT, major, minor, revision)

    /**
     * 将网络 ID 转为 16 位 16 进制字符串。
     *
     * @param networkId 网络 ID
     * @return 16 位 16 进制字符串
     */
    @JvmStatic
    fun networkIdToString(networkId: Long): String =
        String.format(Locale.ROOT, "%016x", networkId)

    /**
     * 将 MAC 地址转为 XX:XX:XX:XX:XX:XX 格式。
     *
     * @param mac MAC 地址
     * @return 格式化后的 MAC 地址
     */
    @JvmStatic
    fun macAddressToString(mac: Long): String {
        var value = mac
        val macChars = IntArray(6)
        for (i in 0 until 6) {
            macChars[i] = (value % 256).toInt()
            value = value shr 8
        }
        return String.format(
            Locale.ROOT, "%02x:%02x:%02x:%02x:%02x:%02x",
            macChars[5], macChars[4], macChars[3], macChars[2], macChars[1], macChars[0]
        )
    }

    /**
     * 将 16 进制字符串转换为字节数组。
     *
     * @param hex 16 进制字符串
     * @return 字节数组
     * @throws RuntimeException 字符串长度为奇数时抛出
     */
    @JvmStatic
    fun hexStringToBytes(hex: String): ByteArray {
        val length = hex.length
        if (length % 2 != 0) {
            throw RuntimeException("String length must be even")
        }
        val result = ByteArray(length / 2)
        var i = 0
        while (i < length) {
            val highDigit = Character.digit(hex[i], 16)
            val lowDigit = Character.digit(hex[i + 1], 16)
            result[i / 2] = ((highDigit shl 4) + lowDigit).toByte()
            i += 2
        }
        return result
    }

    /**
     * 将 InetSocketAddress 转为 IP:Port（IPv6 则是 [IP]:Port）格式的字符串。
     *
     * @param address 套接字地址
     * @return IP:Port 格式字符串
     */
    @JvmStatic
    fun toString(address: InetSocketAddress): String {
        val inetAddress = address.address
        val port = address.port

        // 将 IP 地址转为字符串
        var ipString = inetAddress.hostAddress
        if (inetAddress is Inet6Address) {
            ipString = "[$ipString]"
        }

        return "$ipString:$port"
    }

    /**
     * 将套接字地址格式化为 `地址/前缀`，用于展示 ZeroTier 分配的 IP。
     *
     * @param address 套接字地址；`port` 字段承载前缀位数
     * @param disableIpv6 是否过滤 IPv6 地址
     * @return 格式化结果；地址不可用或被过滤时返回 `null`
     */
    @JvmStatic
    fun formatAddressWithPrefix(address: InetSocketAddress, disableIpv6: Boolean): String? {
        val inetAddress = address.address ?: return null
        if (disableIpv6 && inetAddress is Inet6Address) {
            return null
        }
        val host = inetAddress.hostAddress ?: return null
        return "$host/${address.port}"
    }
}
