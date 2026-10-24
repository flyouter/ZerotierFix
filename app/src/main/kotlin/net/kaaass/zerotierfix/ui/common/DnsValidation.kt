package net.kaaass.zerotierfix.ui.common

import org.apache.commons.validator.routines.InetAddressValidator

/**
 * 自定义 DNS 输入字段。
 */
enum class DnsField {
    /** 第一个 IPv4 地址。 */
    V4_PRIMARY,

    /** 第二个 IPv4 地址。 */
    V4_SECONDARY,

    /** 第一个 IPv6 地址。 */
    V6_PRIMARY,

    /** 第二个 IPv6 地址。 */
    V6_SECONDARY;

    /** 该字段期望的地址类型。 */
    val isIpv6: Boolean
        get() = this == V6_PRIMARY || this == V6_SECONDARY
}

/**
 * 判断 DNS 输入是否合法。
 *
 * 空值视为“不配置”，返回 `true`；非空时按字段类型校验 IPv4 / IPv6。
 *
 * @param value 输入值
 * @param field 字段
 * @return 合法或为空返回 `true`
 */
fun isValidDnsAddress(value: String, field: DnsField): Boolean {
    if (value.isEmpty()) {
        return true
    }
    val validator = InetAddressValidator.getInstance()
    return if (field.isIpv6) {
        validator.isValidInet6Address(value)
    } else {
        validator.isValidInet4Address(value)
    }
}

/**
 * 判断四个自定义 DNS 字段中是否存在非法值。
 *
 * @param v4Primary 第一个 IPv4 地址
 * @param v4Secondary 第二个 IPv4 地址
 * @param v6Primary 第一个 IPv6 地址
 * @param v6Secondary 第二个 IPv6 地址
 * @return 存在非法值返回 `true`
 */
fun hasInvalidDns(
    v4Primary: String,
    v4Secondary: String,
    v6Primary: String,
    v6Secondary: String
): Boolean = !isValidDnsAddress(v4Primary, DnsField.V4_PRIMARY) ||
    !isValidDnsAddress(v4Secondary, DnsField.V4_SECONDARY) ||
    !isValidDnsAddress(v6Primary, DnsField.V6_PRIMARY) ||
    !isValidDnsAddress(v6Secondary, DnsField.V6_SECONDARY)
