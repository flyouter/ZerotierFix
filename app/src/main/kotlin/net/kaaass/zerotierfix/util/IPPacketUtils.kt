package net.kaaass.zerotierfix.util

import android.util.Log

import androidx.core.view.MotionEventCompat

import java.net.InetAddress
import java.net.UnknownHostException

/**
 * IP 报文解析工具类。
 */
object IPPacketUtils {

    private const val TAG = "IPPacketUtils"

    /**
     * 从 IP 报文中获取源地址。
     *
     * @param bArr IP 报文
     * @return 源地址；版本未知或地址非法时返回 `null`
     */
    @JvmStatic
    fun getSourceIP(bArr: ByteArray): InetAddress? {
        val ipVersion = getIPVersion(bArr)
        return if (ipVersion.toInt() == 4) {
            val address = ByteArray(4)
            System.arraycopy(bArr, 12, address, 0, 4)
            try {
                InetAddress.getByAddress(address)
            } catch (e: UnknownHostException) {
                Log.e(TAG, "Error creating InetAddress", e)
                null
            }
        } else if (ipVersion.toInt() == 6) {
            val address = ByteArray(16)
            System.arraycopy(bArr, 8, address, 0, 16)
            try {
                InetAddress.getByAddress(address)
            } catch (e: UnknownHostException) {
                Log.e(TAG, "Error creating InetAddress", e)
                null
            }
        } else {
            Log.e(TAG, "Unknown IP version")
            null
        }
    }

    /**
     * 从 IP 报文中获取目的地址。
     *
     * @param bArr IP 报文
     * @return 目的地址；版本未知或地址非法时返回 `null`
     */
    @JvmStatic
    fun getDestIP(bArr: ByteArray): InetAddress? {
        val ipVersion = getIPVersion(bArr)
        return if (ipVersion.toInt() == 4) {
            val address = ByteArray(4)
            System.arraycopy(bArr, 16, address, 0, 4)
            try {
                InetAddress.getByAddress(address)
            } catch (e: UnknownHostException) {
                Log.e(TAG, "Error creating InetAddress", e)
                null
            }
        } else if (ipVersion.toInt() == 6) {
            val address = ByteArray(16)
            System.arraycopy(bArr, 24, address, 0, 16)
            try {
                InetAddress.getByAddress(address)
            } catch (e: UnknownHostException) {
                Log.e(TAG, "Error creating InetAddress", e)
                null
            }
        } else {
            Log.e(TAG, "Unknown IP version")
            null
        }
    }

    /**
     * 获取 IP 报文版本。
     *
     * @param bArr IP 报文
     * @return 版本号
     */
    @JvmStatic
    fun getIPVersion(bArr: ByteArray): Byte = (bArr[0].toInt() shr 4).toByte()

    /**
     * 计算校验和。
     *
     * @param bArr 报文数据
     * @param j 初始累加值
     * @param i 起始偏移
     * @param i2 结束偏移
     * @return 16 位校验和
     */
    @JvmStatic
    fun calculateChecksum(bArr: ByteArray, j: Long, i: Int, i2: Int): Long {
        var sum = j
        var offset = i
        var remaining = i2 - i
        while (remaining > 1) {
            sum += ((bArr[offset].toInt() and 0xFF) shl 8) or (bArr[offset + 1].toInt() and 0xFF)
            if ((-65536L and sum) > 0) {
                sum = (sum and 0xFFFFL) + 1
            }
            offset += 2
            remaining -= 2
        }
        if (remaining > 0) {
            sum += ((bArr[offset].toInt() and 0xFF) shl 8) and MotionEventCompat.ACTION_POINTER_INDEX_MASK
            if ((sum and -65536L) > 0) {
                sum = (sum and 0xFFFFL) + 1
            }
        }
        return sum.inv() and 0xFFFFL
    }
}
