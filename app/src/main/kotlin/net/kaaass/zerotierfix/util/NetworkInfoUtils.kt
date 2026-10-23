package net.kaaass.zerotierfix.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log

import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import java.util.ArrayList
import java.util.regex.Pattern

/**
 * 网络连接信息处理工具类。
 */
object NetworkInfoUtils {

    /** 日志标签。 */
    const val TAG = "NetworkInfoUtils"

    /**
     * 当前网络连接类型。
     */
    enum class CurrentConnection {

        /**
         * 无网络连接。
         */
        CONNECTION_NONE,

        /**
         * 移动数据连接。
         */
        CONNECTION_MOBILE,

        /**
         * 其他网络连接（如 Wi-Fi）。
         */
        CONNECTION_OTHER
    }

    /**
     * 获取当前网络连接类型。
     *
     * @param context 上下文
     * @return 当前连接类型；无网络或无法获取时返回 [CurrentConnection.CONNECTION_NONE]
     */
    @JvmStatic
    fun getNetworkInfoCurrentConnection(context: Context): CurrentConnection {
        val connectivityManager = context
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            ?: return CurrentConnection.CONNECTION_NONE
        if (Build.VERSION.SDK_INT >= 23) {
            val networkCapabilities = connectivityManager
                .getNetworkCapabilities(connectivityManager.activeNetwork)
                ?: return CurrentConnection.CONNECTION_NONE
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return CurrentConnection.CONNECTION_MOBILE
            }
            return CurrentConnection.CONNECTION_OTHER
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
                ?: return CurrentConnection.CONNECTION_NONE
            if (!activeNetworkInfo.isConnectedOrConnecting) {
                return CurrentConnection.CONNECTION_NONE
            }
            if (activeNetworkInfo.type == 0) {
                return CurrentConnection.CONNECTION_MOBILE
            }
            return CurrentConnection.CONNECTION_OTHER
        }
    }

    /**
     * 列出指定网卡上的组播组。
     *
     * @param interfaceName 网卡名
     * @param isIpv6 是否读取 IPv6 组播信息
     * @return 组播组地址列表；读取失败时返回空列表
     */
    @JvmStatic
    fun listMulticastGroupOnInterface(interfaceName: String, isIpv6: Boolean): List<String> {
        val groups = ArrayList<String>()

        val igmpFilePath = if (isIpv6) "/proc/net/igmp6" else "/proc/net/igmp"

        /*
         * 从 /proc/net/igmp 或 /proc/net/igmp6 的信息格式大致为:
         *
         * Idx     Device    : Count Querier       Group    Users Timer    Reporter
         * 1       tun0      :     2      V3
         *                                 010000E0     1 0:00000000               0
         * 2       wlan0     :     1      V3
         *                                 010000E0     1 0:00000000               0
         *
         * 因此，解析时需要先找到目标 interface 的行，然后开始读取若干行的组播组信息。
         */
        try {
            BufferedReader(FileReader(igmpFilePath)).use { igmpInfo ->
                var foundTargetInterface = false
                var line: String? = igmpInfo.readLine()

                while (line != null) {
                    val row = Pattern.compile("\\s+").split(line, -1)

                    if (!foundTargetInterface) {
                        if (row.size > 1 && row[1] == interfaceName) {
                            // 找到目标 interface 的行
                            foundTargetInterface = true
                        }
                    } else {
                        if (row[0] == "") {
                            groups.add(row[1])
                        } else {
                            // 目标 interface 的信息结束
                            foundTargetInterface = false
                        }
                    }
                    line = igmpInfo.readLine()
                }
            }
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "IGMP info file not found", e)
        } catch (e: IOException) {
            Log.e(TAG, "Error reading IGMP info", e)
        }
        return groups
    }
}
