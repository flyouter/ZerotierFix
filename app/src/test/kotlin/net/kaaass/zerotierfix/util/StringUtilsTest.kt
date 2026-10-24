package net.kaaass.zerotierfix.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

import java.net.InetSocketAddress

/**
 * [StringUtils.formatAddressWithPrefix] 的单元测试。
 */
class StringUtilsTest {

    @Test
    fun `IPv4 地址格式化为地址斜杠前缀`() {
        val address = InetSocketAddress("10.147.17.5", 24)

        assertEquals("10.147.17.5/24", StringUtils.formatAddressWithPrefix(address, false))
    }

    @Test
    fun `IPv6 地址默认保留`() {
        val address = InetSocketAddress("fd00::5", 88)

        val result = StringUtils.formatAddressWithPrefix(address, false)

        assertTrue(result != null)
        assertTrue(result!!.endsWith("/88"))
        assertTrue(result.contains(":"))
    }

    @Test
    fun `启用 IPv6 过滤时返回 null`() {
        val address = InetSocketAddress("fd00::5", 88)

        assertNull(StringUtils.formatAddressWithPrefix(address, true))
    }

    @Test
    fun `无法解析的地址返回 null`() {
        val address = InetSocketAddress.createUnresolved("example.com", 24)

        assertNull(StringUtils.formatAddressWithPrefix(address, false))
    }
}
