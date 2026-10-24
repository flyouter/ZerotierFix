package net.kaaass.zerotierfix.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 网络邀请二维码解析的单元测试。
 */
class NetworkIdScanParserTest {

    private val networkId = "673017c788cb3f7d"

    @Test
    fun `标准邀请链接解析为小写网络 ID`() {
        assertEquals(
            NetworkIdScanResult.Success(networkId),
            NetworkIdScanParser.parse("https://joinzt.com/addnetwork?nwid=$networkId&v=1")
        )
    }

    @Test
    fun `scheme 与 host 大小写不敏感`() {
        assertEquals(
            NetworkIdScanResult.Success(networkId),
            NetworkIdScanParser.parse("HTTPS://JOINZT.COM/addnetwork?nwid=$networkId")
        )
    }

    @Test
    fun `查询参数顺序无关`() {
        assertEquals(
            NetworkIdScanResult.Success(networkId),
            NetworkIdScanParser.parse("https://joinzt.com/addnetwork?v=1&nwid=$networkId")
        )
    }

    @Test
    fun `百分号编码的 nwid 可解析`() {
        val encoded = networkId.map { "%%%02X".format(it.code) }.joinToString("")
        assertEquals(
            NetworkIdScanResult.Success(networkId),
            NetworkIdScanParser.parse("https://joinzt.com/addnetwork?nwid=$encoded")
        )
    }

    @Test
    fun `裸 16 位十六进制可解析并小写化`() {
        assertEquals(
            NetworkIdScanResult.Success(networkId),
            NetworkIdScanParser.parse("  673017C788CB3F7D  ")
        )
    }

    @Test
    fun `显式 443 端口可接受`() {
        assertEquals(
            NetworkIdScanResult.Success(networkId),
            NetworkIdScanParser.parse("https://joinzt.com:443/addnetwork?nwid=$networkId")
        )
    }

    @Test
    fun `http 链接被拒绝`() {
        assertEquals(
            NetworkIdScanResult.NotAJoinCode,
            NetworkIdScanParser.parse("http://joinzt.com/addnetwork?nwid=$networkId")
        )
    }

    @Test
    fun `非 joinzt 主机被拒绝`() {
        assertEquals(
            NetworkIdScanResult.NotAJoinCode,
            NetworkIdScanParser.parse("https://example.com/addnetwork?nwid=$networkId")
        )
        assertEquals(
            NetworkIdScanResult.NotAJoinCode,
            NetworkIdScanParser.parse("https://joinzt.com.evil.com/addnetwork?nwid=$networkId")
        )
        assertEquals(
            NetworkIdScanResult.NotAJoinCode,
            NetworkIdScanParser.parse("https://joinzt.com@evil.com/addnetwork?nwid=$networkId")
        )
    }

    @Test
    fun `错误路径被拒绝`() {
        assertEquals(
            NetworkIdScanResult.NotAJoinCode,
            NetworkIdScanParser.parse("https://joinzt.com/addnetwork/?nwid=$networkId")
        )
        assertEquals(
            NetworkIdScanResult.NotAJoinCode,
            NetworkIdScanParser.parse("https://joinzt.com/other?nwid=$networkId")
        )
    }

    @Test
    fun `非默认端口被拒绝`() {
        assertEquals(
            NetworkIdScanResult.NotAJoinCode,
            NetworkIdScanParser.parse("https://joinzt.com:8443/addnetwork?nwid=$networkId")
        )
    }

    @Test
    fun `缺少 nwid 被拒绝`() {
        assertEquals(
            NetworkIdScanResult.NotAJoinCode,
            NetworkIdScanParser.parse("https://joinzt.com/addnetwork?v=1")
        )
    }

    @Test
    fun `非法 nwid 返回无效网络 ID`() {
        assertEquals(
            NetworkIdScanResult.InvalidNetworkId,
            NetworkIdScanParser.parse("https://joinzt.com/addnetwork?nwid=673017c788cb3f7")
        )
        assertEquals(
            NetworkIdScanResult.InvalidNetworkId,
            NetworkIdScanParser.parse("https://joinzt.com/addnetwork?nwid=zzzzzzzzzzzzzzzz")
        )
    }

    @Test
    fun `普通文本返回非邀请码`() {
        assertEquals(NetworkIdScanResult.NotAJoinCode, NetworkIdScanParser.parse("hello world"))
        assertEquals(NetworkIdScanResult.NotAJoinCode, NetworkIdScanParser.parse("https://example.com/"))
        assertEquals(NetworkIdScanResult.NotAJoinCode, NetworkIdScanParser.parse(""))
        assertEquals(NetworkIdScanResult.NotAJoinCode, NetworkIdScanParser.parse("   "))
    }
}
