package net.kaaass.zerotierfix.service

import android.app.Activity
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope

import dagger.hilt.android.AndroidEntryPoint

import kotlinx.coroutines.launch

import net.kaaass.zerotierfix.R

import javax.inject.Inject

/**
 * 从快捷设置磁贴触发的透明 VPN 授权页。
 *
 * 页面本身不绘制 UI，只负责拉起系统 VPN 授权弹窗。授权通过后调用
 * [QuickTileController.connect] 并在应用级作用域中完成连接，随后立即关闭。
 */
@AndroidEntryPoint
class VpnConsentActivity : ComponentActivity() {

    @Inject
    lateinit var controller: QuickTileController

    private var networkId: Long = 0L
    private var consentRequested = false
    private var resultHandled = false

    private val consentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (resultHandled) {
            return@registerForActivityResult
        }
        resultHandled = true
        if (result.resultCode == Activity.RESULT_OK) {
            connectAndFinish()
        } else {
            Toast.makeText(
                this,
                R.string.toast_vpn_permission_denied,
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        networkId = intent.getLongExtra(EXTRA_NETWORK_ID, 0L)
        if (networkId == 0L) {
            finish()
            return
        }

        // 配置变化重建时不要再次拉起系统授权弹窗，等待 ActivityResult 恢复结果。
        consentRequested = savedInstanceState?.getBoolean(KEY_CONSENT_REQUESTED) ?: false
        if (!consentRequested) {
            requestConsent()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_CONSENT_REQUESTED, consentRequested)
        super.onSaveInstanceState(outState)
    }

    /**
     * 请求系统 VPN 授权；已授权时直接连接。
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
            finish()
            return
        }
        if (consentIntent == null) {
            resultHandled = true
            connectAndFinish()
        } else {
            consentLauncher.launch(consentIntent)
        }
    }

    /**
     * 连接目标网络，等待服务启动请求完成后再关闭页面。
     *
     * Android 12+ 禁止后台应用启动服务，因此页面必须保持在前台直到
     * [QuickTileController.connectAwait] 返回。
     */
    private fun connectAndFinish() {
        lifecycleScope.launch {
            controller.connectAwait(networkId)
            finish()
        }
    }

    companion object {
        /** 待连接网络 ID 的 Intent extra。 */
        const val EXTRA_NETWORK_ID = "net.kaaass.zerotierfix.extra.NETWORK_ID"

        private const val KEY_CONSENT_REQUESTED = "consent_requested"
    }
}
