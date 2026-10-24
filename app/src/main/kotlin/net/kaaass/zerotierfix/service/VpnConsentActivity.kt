package net.kaaass.zerotierfix.service

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope

import dagger.hilt.android.AndroidEntryPoint

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

import net.kaaass.zerotierfix.R

import javax.inject.Inject

/**
 * 从快捷设置磁贴触发的透明页面。
 *
 * 页面不绘制 UI，职责是：
 * - 尚未授权时拉起系统 VPN 授权弹窗；
 * - 保持应用处于前台，调用 [QuickTileController.settleAwait] 完成连接/断开
 *   （Android 12+ 禁止后台应用启动服务）；
 * - 收敛到用户期望状态后立即关闭。
 *
 * 多次点击会通过 `singleTop` 复用本实例：新 Intent 触发 [onNewIntent] 并继续
 * 驱动同一个收敛循环，因此快速连点不会堆叠页面。
 */
@AndroidEntryPoint
class VpnConsentActivity : ComponentActivity() {

    @Inject
    lateinit var controller: QuickTileController

    private var networkId: Long = 0L
    private var consentRequested = false
    private var resultHandled = false
    private var settleJob: Job? = null
    private var settleRequested = false

    private val consentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (resultHandled) {
            return@registerForActivityResult
        }
        resultHandled = true
        if (result.resultCode == Activity.RESULT_OK) {
            startSettleIfNeeded()
        } else {
            Toast.makeText(
                this,
                R.string.toast_vpn_permission_denied,
                Toast.LENGTH_SHORT
            ).show()
            controller.abandonIntent()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!applyNetworkId(intent)) {
            return
        }

        consentRequested = savedInstanceState?.getBoolean(KEY_CONSENT_REQUESTED) ?: false
        resultHandled = savedInstanceState?.getBoolean(KEY_RESULT_HANDLED) ?: false

        when {
            resultHandled -> startSettleIfNeeded()
            // 配置变化重建时不要再次拉起系统授权弹窗，等待 ActivityResult 恢复结果。
            consentRequested -> Unit
            else -> requestConsent()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (!applyNetworkId(intent)) {
            return
        }
        // 新点击意味着此前的授权流程已不再需要结果；
        // 即使旧授权回调迟到，也不会覆盖当前收敛过程。
        resultHandled = true
        startSettleIfNeeded()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_CONSENT_REQUESTED, consentRequested)
        outState.putBoolean(KEY_RESULT_HANDLED, resultHandled)
        super.onSaveInstanceState(outState)
    }

    /**
     * 读取并校验目标网络 ID。
     *
     * @return 是否可继续；无效时结束页面
     */
    private fun applyNetworkId(intent: Intent): Boolean {
        val id = intent.getLongExtra(EXTRA_NETWORK_ID, 0L)
        if (id == 0L) {
            controller.abandonIntent()
            finish()
            return false
        }
        networkId = id
        return true
    }

    /**
     * 请求系统 VPN 授权；已授权时直接开始收敛。
     */
    private fun requestConsent() {
        consentRequested = true
        val consentIntent = try {
            VpnService.prepare(this)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                R.string.toast_connect_failed,
                Toast.LENGTH_SHORT
            ).show()
            controller.abandonIntent()
            finish()
            return
        }
        if (consentIntent == null) {
            resultHandled = true
            startSettleIfNeeded()
        } else {
            consentLauncher.launch(consentIntent)
        }
    }

    /**
     * 启动（或标记重启）前台收敛循环。
     *
     * Android 12+ 禁止后台启动服务，页面必须保持在前台直到
     * [QuickTileController.settleAwait] 返回。
     */
    private fun startSettleIfNeeded() {
        if (settleJob?.isActive == true) {
            settleRequested = true
            return
        }
        settleRequested = false
        settleJob = lifecycleScope.launch {
            do {
                settleRequested = false
                controller.settleAwait(allowConnect = true)
            } while (settleRequested)
            finish()
        }
    }

    companion object {
        /** 待连接网络 ID 的 Intent extra。 */
        const val EXTRA_NETWORK_ID = "net.kaaass.zerotierfix.extra.NETWORK_ID"

        private const val KEY_CONSENT_REQUESTED = "consent_requested"
        private const val KEY_RESULT_HANDLED = "result_handled"
    }
}
