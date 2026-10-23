package net.kaaass.zerotierfix.service

import android.util.Log

import com.zerotier.sdk.Node
import com.zerotier.sdk.ResultCode

import net.kaaass.zerotierfix.util.NetworkInfoUtils
import net.kaaass.zerotierfix.util.StringUtils
import net.kaaass.zerotierfix.vpn.tun.TunTapAdapter

import java.net.InetAddress
import java.util.ArrayList

/**
 * 旧版本 Android 的组播组扫描线程。
 *
 * 定期读取 `tun0` 上的组播组并与已订阅列表比对，向 ZeroTier 节点发送订阅/退订请求。
 * 线程生命周期由 [ZeroTierOneService] 管理。
 *
 * @param nodeProvider 返回当前节点实例；节点未创建时返回 `null`
 * @param networkIdProvider 返回当前网络 ID
 * @param isIpv6 是否读取 IPv6 组播信息
 */
internal class MulticastScanner(
    private val nodeProvider: () -> Node?,
    private val networkIdProvider: () -> Long,
    private val isIpv6: Boolean
) : Thread() {

    private var subscriptions: List<String> = ArrayList()

    override fun run() {
        val family = if (isIpv6) "IPv6" else "IPv4"
        Log.d(TAG, "$family Multicast Scanner Thread Started.")
        while (!isInterrupted) {
            try {
                val groups = NetworkInfoUtils.listMulticastGroupOnInterface("tun0", isIpv6)

                val oldSubscriptions = ArrayList(subscriptions)
                val newGroups = ArrayList(groups)
                newGroups.removeAll(oldSubscriptions)
                for (group in newGroups) {
                    try {
                        val address = toAddress(group)
                        val multicastSubscribe = nodeProvider()!!.multicastSubscribe(
                            networkIdProvider(),
                            TunTapAdapter.multicastAddressToMAC(address)
                        )
                        if (multicastSubscribe != ResultCode.RESULT_OK) {
                            Log.e(
                                TAG,
                                "Error when calling multicastSubscribe: $multicastSubscribe"
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, e.toString(), e)
                    }
                }
                oldSubscriptions.removeAll(ArrayList(groups))
                for (group in oldSubscriptions) {
                    try {
                        val address = toAddress(group)
                        val multicastUnsubscribe = nodeProvider()!!.multicastUnsubscribe(
                            networkIdProvider(),
                            TunTapAdapter.multicastAddressToMAC(address)
                        )
                        if (multicastUnsubscribe != ResultCode.RESULT_OK) {
                            Log.e(
                                TAG,
                                "Error when calling multicastUnsubscribe: $multicastUnsubscribe"
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, e.toString(), e)
                    }
                }
                subscriptions = groups
                Thread.sleep(1000)
            } catch (e: InterruptedException) {
                Log.d(TAG, "${if (isIpv6) "V6" else "V4"} Multicast Scanner Thread Interrupted", e)
                break
            }
        }
        Log.d(TAG, "$family Multicast Scanner Thread Ended.")
    }

    /**
     * 将 `/proc/net/igmp*` 中的组播组字符串转为地址。
     *
     * IPv4 组播组按 4 字节一组倒序存放，需要先交换每对字节。
     */
    private fun toAddress(group: String): InetAddress {
        val bytes = StringUtils.hexStringToBytes(group)
        if (!isIpv6) {
            for (i in 0 until bytes.size / 2) {
                val b = bytes[i]
                bytes[i] = bytes[(bytes.size - i) - 1]
                bytes[(bytes.size - i) - 1] = b
            }
        }
        return InetAddress.getByAddress(bytes)
    }

    companion object {
        private const val TAG = "ZT1_Service"
    }
}
