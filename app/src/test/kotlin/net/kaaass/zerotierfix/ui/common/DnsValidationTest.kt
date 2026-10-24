package net.kaaass.zerotierfix.ui.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DNS 校验纯函数的单元测试。
 */
class DnsValidationTest {

    @Test
    fun `空值视为不配置`() {
        assertTrue(isValidDnsAddress("", DnsField.V4_PRIMARY))
        assertTrue(isValidDnsAddress("", DnsField.V6_PRIMARY))
    }

    @Test
    fun `IPv4 字段接受合法 IPv4`() {
        assertTrue(isValidDnsAddress("8.8.8.8", DnsField.V4_PRIMARY))
        assertTrue(isValidDnsAddress("1.1.1.1", DnsField.V4_SECONDARY))
    }

    @Test
    fun `IPv4 字段拒绝 IPv6 与非法值`() {
        assertFalse(isValidDnsAddress("fd00::1", DnsField.V4_PRIMARY))
        assertFalse(isValidDnsAddress("999.1.1.1", DnsField.V4_PRIMARY))
    }

    @Test
    fun `IPv6 字段接受合法 IPv6`() {
        assertTrue(isValidDnsAddress("fd00::1", DnsField.V6_PRIMARY))
        assertTrue(isValidDnsAddress("2001:4860:4860::8888", DnsField.V6_SECONDARY))
    }

    @Test
    fun `IPv6 字段拒绝 IPv4 与非法值`() {
        assertFalse(isValidDnsAddress("8.8.8.8", DnsField.V6_PRIMARY))
        assertFalse(isValidDnsAddress("not-an-ip", DnsField.V6_PRIMARY))
    }

    @Test
    fun `存在任意非法字段时整体校验失败`() {
        assertFalse(hasInvalidDns("8.8.8.8", "", "", ""))
        assertTrue(hasInvalidDns("8.8.8.8", "999.1.1.1", "", ""))
        assertFalse(hasInvalidDns("", "", "", ""))
        assertFalse(hasInvalidDns("", "", "fd00::1", ""))
    }
}
