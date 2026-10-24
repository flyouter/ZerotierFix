package net.kaaass.zerotierfix.util

import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder

/**
 * 二维码内容解析结果。
 */
sealed interface NetworkIdScanResult {

    /** 解析得到的 16 位小写十六进制网络 ID。 */
    data class Success(val networkId: String) : NetworkIdScanResult

    /** 不是 ZeroTier 网络邀请码。 */
    data object NotAJoinCode : NetworkIdScanResult

    /** 是网络邀请码，但其中的网络 ID 非法。 */
    data object InvalidNetworkId : NetworkIdScanResult
}

/**
 * 解析 ZeroTier 网络邀请二维码内容。
 *
 * 支持 `https://joinzt.com/addnetwork?nwid=<16 位十六进制>`，以及内容本身就是
 * 16 位十六进制网络 ID 的裸码。解析保持纯 JVM，便于单元测试。
 */
object NetworkIdScanParser {

    /** 邀请码主机。 */
    private const val HOST = "joinzt.com"

    /** 邀请码路径。 */
    private const val PATH = "/addnetwork"

    /** 网络 ID 格式。 */
    private val NETWORK_ID = Regex("^[0-9a-fA-F]{16}$")

    /**
     * 解析扫码内容。
     *
     * @param raw 二维码原始文本
     * @return 解析结果
     */
    fun parse(raw: String): NetworkIdScanResult {
        val text = raw.trim()
        if (NETWORK_ID.matches(text)) {
            return NetworkIdScanResult.Success(text.lowercase())
        }

        val uri = try {
            URI(text)
        } catch (_: URISyntaxException) {
            return NetworkIdScanResult.NotAJoinCode
        }

        if (!uri.scheme.equals("https", ignoreCase = true) ||
            !uri.host.equals(HOST, ignoreCase = true) ||
            (uri.port != -1 && uri.port != 443) ||
            uri.path != PATH
        ) {
            return NetworkIdScanResult.NotAJoinCode
        }

        val nwid = parseQuery(uri.rawQuery)["nwid"] ?: return NetworkIdScanResult.NotAJoinCode
        return if (NETWORK_ID.matches(nwid)) {
            NetworkIdScanResult.Success(nwid.lowercase())
        } else {
            NetworkIdScanResult.InvalidNetworkId
        }
    }

    /**
     * 解析查询参数，非法编码的键值对会被忽略。
     */
    private fun parseQuery(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrEmpty()) {
            return emptyMap()
        }
        return buildMap {
            for (pair in rawQuery.split('&')) {
                val index = pair.indexOf('=')
                if (index <= 0) {
                    continue
                }
                val name = decode(pair.substring(0, index)) ?: continue
                val value = decode(pair.substring(index + 1)) ?: continue
                put(name, value)
            }
        }
    }

    /**
     * 百分号解码。
     */
    private fun decode(value: String): String? = try {
        URLDecoder.decode(value, Charsets.UTF_8.name())
    } catch (_: IllegalArgumentException) {
        null
    }
}
