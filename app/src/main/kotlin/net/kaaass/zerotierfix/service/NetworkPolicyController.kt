package net.kaaass.zerotierfix.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.util.Log

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

import net.kaaass.zerotierfix.data.repository.SettingsRepository
import net.kaaass.zerotierfix.util.NetworkInfoUtils

/**
 * 移动数据访问策略。
 *
 * “允许移动网络下使用”关闭且当前默认网络为蜂窝时，不允许数据面访问 ZeroTier
 * 内网；隧道本身始终保持连接。策略变化（网络切换或设置变化）时回调
 * [onAccessChanged]，由服务切换 [net.kaaass.zerotierfix.vpn.tun.TunTapAdapter]
 * 的收发闸门。
 *
 * 该设置只决定蜂窝网络下能否访问 ZeroTier 内网；是否把默认流量
 * （`0.0.0.0/0`）交给 ZeroTier 由每个网络的“通过 ZeroTier 路由”开关控制，
 * 两者互不影响。
 *
 * VPN 运行时 Android 对 VPN 宿主应用暴露的默认网络是底层物理网络（或携带底层
 * transport 的 VPN 网络），因此可以直接用 [NetworkInfoUtils] 判断当前连接类型。
 *
 * 线程：[start] 与 [stop] 内部串行化，可在任意线程调用；[isMobileDataAllowed]
 * 可在任意线程读取。
 */
class NetworkPolicyController(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope,
    private val onAccessChanged: (Boolean) -> Unit
) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val handler = Handler(Looper.getMainLooper())

    /** 最近一次稳定后的连接类型；[NetworkInfoUtils.CurrentConnection.CONNECTION_NONE] 表示无网络或未知。 */
    private val connection = MutableStateFlow(NetworkInfoUtils.CurrentConnection.CONNECTION_NONE)

    private val refreshRunnable = Runnable {
        connection.value = NetworkInfoUtils.getNetworkInfoCurrentConnection(context)
    }

    private var policyJob: Job? = null

    /** 是否已注册网络回调。 */
    private var started = false

    /**
     * 当前网络下是否允许访问 ZeroTier 内网。
     *
     * 任意线程可读；每次读取重新查询当前网络，避免网络切换防抖期间读到旧值。
     *
     * @return 允许访问时返回 `true`
     */
    val isMobileDataAllowed: Boolean
        get() = isAllowed(
            NetworkInfoUtils.getNetworkInfoCurrentConnection(context),
            settingsRepository.isCellularDataAllowed
        )

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = scheduleRefresh()

        override fun onLost(network: Network) = scheduleRefresh()

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) = scheduleRefresh()
    }

    /**
     * 开始监听默认网络与设置变化。
     *
     * 幂等：重复调用不会重复注册回调或启动多个收集任务。
     * 线程：主线程与停止线程可能并发调用，方法内部串行化。
     */
    @Synchronized
    fun start() {
        if (started) {
            return
        }
        started = true
        refreshRunnable.run()
        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // 注册失败时仍可按启动时的状态放行，只是不会随网络变化切换访问。
            Log.e(TAG, "registerDefaultNetworkCallback failed", e)
            started = false
            return
        }
        policyJob = scope.launch {
            var previous: Boolean? = null
            combine(
                connection,
                settingsRepository.settings
            ) { connection, settings ->
                // 无网络时保持上一次策略，避免切换瞬间误判。
                if (connection == NetworkInfoUtils.CurrentConnection.CONNECTION_NONE) {
                    null
                } else {
                    isAllowed(connection, settings.isCellularDataAllowed)
                }
            }.collect { allowed ->
                if (allowed == null) {
                    return@collect
                }
                if (previous == allowed) {
                    return@collect
                }
                previous = allowed
                // 首次也回调，让服务与数据面闸门在策略启动时立即对齐。
                Log.i(TAG, "Mobile data access allowed: $allowed")
                onAccessChanged(allowed)
            }
        }
    }

    /**
     * 停止监听并取消策略收集。
     *
     * 幂等：未启动时调用无副作用。
     * 线程：可在任意线程调用。
     */
    @Synchronized
    fun stop() {
        if (!started) {
            return
        }
        started = false
        handler.removeCallbacks(refreshRunnable)
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.e(TAG, "unregisterNetworkCallback failed", e)
        }
        policyJob?.cancel()
        policyJob = null
    }

    /**
     * 网络回调后延迟刷新，等系统完成默认网络切换再取值。
     */
    private fun scheduleRefresh() {
        handler.removeCallbacks(refreshRunnable)
        handler.postDelayed(refreshRunnable, NETWORK_SETTLE_DELAY_MS)
    }

    companion object {
        private const val TAG = "NetworkPolicy"

        /** 网络回调后等待默认网络稳定的时间。 */
        private const val NETWORK_SETTLE_DELAY_MS = 800L

        /**
         * 根据连接类型与设置判断当前网络下是否允许访问 ZeroTier 内网。
         *
         * @param connection 当前连接类型
         * @param isCellularDataAllowed 是否允许在移动网络下访问 ZeroTier 内网
         * @return 允许访问时返回 `true`
         */
        internal fun isAllowed(
            connection: NetworkInfoUtils.CurrentConnection,
            isCellularDataAllowed: Boolean
        ): Boolean = isCellularDataAllowed ||
            connection != NetworkInfoUtils.CurrentConnection.CONNECTION_MOBILE
    }
}
