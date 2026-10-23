package net.kaaass.zerotierfix.util

import android.util.Log

import java.math.BigInteger
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.ByteBuffer

/**
 * IP 地址计算工具类。
 */
object InetAddressUtils {

    /** 日志标签。 */
    const val TAG = "InetAddressUtils"

    /** 广播 MAC 地址。 */
    const val BROADCAST_MAC_ADDRESS = 0xffffffffffffL

    /**
     * 获得地址指定 CIDR 的子网掩码。
     *
     * @param address 参考地址
     * @param cidr CIDR 前缀长度
     * @return 子网掩码字节数组
     */
    @JvmStatic
    fun addressToNetmask(address: InetAddress, cidr: Int): ByteArray {
        val length = address.address.size
        val subnetLength = length * 8 - cidr
        val fullMasked = ByteArray(length)
        for (i in 0 until length) {
            fullMasked[i] = -1
        }
        return if (length == 4) {
            // IPv4 地址
            ByteBuffer.allocate(4)
                .putInt((ByteBuffer.wrap(fullMasked).int shr subnetLength) shl subnetLength)
                .array()
        } else {
            // IPv6 地址
            if (cidr == 0) {
                // 若 CIDR 为 0 则返回空子网掩码
                return ByteArray(length)
            }
            val shiftedAddress = BigInteger(fullMasked)
                .shiftRight(subnetLength)
                .shiftLeft(subnetLength)
                .toByteArray()
            if (shiftedAddress.size == length) {
                return shiftedAddress
            }
            // 高位为 0 时需要在前补 0
            val netmask = ByteArray(length)
            val offset = kotlin.math.abs(length - shiftedAddress.size)
            for (i in 0 until offset) {
                netmask[i] = shiftedAddress[0]
            }
            System.arraycopy(shiftedAddress, 0, netmask, offset, shiftedAddress.size)
            netmask
        }
    }

    /**
     * 获得地址对应的路由前缀。
     *
     * @param inetAddress 地址
     * @param cidr CIDR 前缀长度
     * @return 路由前缀地址；无法计算时返回 `null`
     */
    @JvmStatic
    fun addressToRoute(inetAddress: InetAddress, cidr: Int): InetAddress? {
        if (cidr == 0) {
            if (inetAddress is Inet4Address) {
                return try {
                    Inet4Address.getByAddress(byteArrayOf(0, 0, 0, 0))
                } catch (unused: UnknownHostException) {
                    null
                }
            } else if (inetAddress is Inet6Address) {
                val bytes = ByteArray(16)
                for (i in 0 until 16) {
                    bytes[i] = 0
                }
                return try {
                    Inet6Address.getByAddress(bytes)
                } catch (unused: UnknownHostException) {
                    null
                }
            }
        }
        return addressToRouteNo0Route(inetAddress, cidr)
    }

    /**
     * 获得地址对应的网络前缀。
     *
     * @param address 地址
     * @param cidr CIDR 前缀长度
     * @return 网络前缀地址；无法计算时返回 `null`
     */
    @JvmStatic
    fun addressToRouteNo0Route(address: InetAddress, cidr: Int): InetAddress? {
        val netmask = addressToNetmask(address, cidr)
        val rawAddress = ByteArray(netmask.size)
        for (i in netmask.indices) {
            rawAddress[i] = (address.address[i].toInt() and netmask[i].toInt()).toByte()
        }
        return try {
            InetAddress.getByAddress(rawAddress)
        } catch (unused: UnknownHostException) {
            Log.e(TAG, "Unknown Host Exception calculating route")
            null
        }
    }

    /**
     * 将 IPv6 地址转换为对应的组播 MAC 地址。
     *
     * @param inetAddress IPv6 地址
     * @return 组播 MAC 地址；地址长度不是 16 时返回 0
     */
    @JvmStatic
    fun ipv6ToMulticastAddress(inetAddress: InetAddress): Long {
        val address = inetAddress.address
        if (address.size != 16) {
            return 0
        }
        return ByteBuffer.wrap(
            byteArrayOf(0, 0, 51, 51, -1, address[13], address[14], address[15])
        ).long
    }
}
