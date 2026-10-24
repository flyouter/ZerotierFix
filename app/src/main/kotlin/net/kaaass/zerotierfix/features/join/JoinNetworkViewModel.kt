package net.kaaass.zerotierfix.features.join

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import net.kaaass.zerotierfix.R
import net.kaaass.zerotierfix.data.repository.NetworkRepository
import net.kaaass.zerotierfix.data.repository.SettingsRepository
import net.kaaass.zerotierfix.di.IoDispatcher
import net.kaaass.zerotierfix.model.dto.JoinNetworkRequest
import net.kaaass.zerotierfix.model.enums.DNSMode
import net.kaaass.zerotierfix.ui.common.DnsField
import net.kaaass.zerotierfix.ui.common.UiText
import net.kaaass.zerotierfix.ui.common.hasInvalidDns
import net.kaaass.zerotierfix.ui.common.isValidDnsAddress
import net.kaaass.zerotierfix.util.NetworkIdUtils

import javax.inject.Inject

/**
 * 添加网络页面的 ViewModel。
 *
 * 校验规则与原实现一致：网络 ID 长度为 16 且只允许 16 进制字符；
 * 自定义 DNS 仅在非空且格式合法时提交。
 */
@HiltViewModel
class JoinNetworkViewModel @Inject constructor(
    private val networkRepository: NetworkRepository,
    private val settingsRepository: SettingsRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(JoinNetworkUiState())

    /** 页面状态。 */
    val uiState: StateFlow<JoinNetworkUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<JoinNetworkEffect>(extraBufferCapacity = 4)

    /** 一次性事件。 */
    val effects: SharedFlow<JoinNetworkEffect> = _effects.asSharedFlow()

    /**
     * 更新网络 ID 输入。
     *
     * 非 16 进制字符会被过滤，超过 16 位会被截断；输入不足 16 位时取消
     * “通过 ZeroTier 路由”勾选并清除对应的偏好记录，与原实现一致。
     *
     * @param value 用户输入
     */
    fun onNetworkIdChanged(value: String) {
        val filtered = value.filter { isHexDigit(it) }.take(NETWORK_ID_LENGTH)
        val valid = filtered.length == NETWORK_ID_LENGTH
        val previous = _uiState.value
        var routeViaZeroTier = previous.isRouteViaZeroTier
        if (!valid && routeViaZeroTier) {
            settingsRepository.setDefaultRouteEnabled(filtered, false)
            routeViaZeroTier = false
        }
        _uiState.update {
            it.copy(
                networkId = filtered,
                isNetworkIdValid = valid,
                isRouteViaZeroTier = routeViaZeroTier
            )
        }
    }

    /**
     * 更新“通过 ZeroTier 路由”选项。
     *
     * 仅在网络 ID 合法时生效，并同步写入历史偏好键。
     *
     * @param enabled 是否勾选
     */
    fun onRouteViaZeroTierChanged(enabled: Boolean) {
        val state = _uiState.value
        if (!state.isNetworkIdValid) {
            return
        }
        settingsRepository.setDefaultRouteEnabled(state.networkId, enabled)
        _uiState.update { it.copy(isRouteViaZeroTier = enabled) }
    }

    /**
     * 切换 DNS 模式。
     *
     * @param mode DNS 模式
     */
    fun onDnsModeChanged(mode: DNSMode) {
        _uiState.update { state ->
            state.copy(
                dnsMode = mode,
                hasInvalidDns = state.invalidDnsFor(mode)
            )
        }
    }

    /**
     * 更新自定义 DNS 地址。
     *
     * @param field 字段
     * @param value 输入值
     */
    fun onDnsChanged(field: DnsField, value: String) {
        _uiState.update { state ->
            val updated = when (field) {
                DnsField.V4_PRIMARY -> state.copy(dnsV4Primary = value)
                DnsField.V4_SECONDARY -> state.copy(dnsV4Secondary = value)
                DnsField.V6_PRIMARY -> state.copy(dnsV6Primary = value)
                DnsField.V6_SECONDARY -> state.copy(dnsV6Secondary = value)
            }
            updated.copy(
                hasInvalidDns = hasInvalidDns(
                    v4Primary = updated.dnsV4Primary,
                    v4Secondary = updated.dnsV4Secondary,
                    v6Primary = updated.dnsV6Primary,
                    v6Secondary = updated.dnsV6Secondary
                )
            )
        }
    }

    /**
     * 提交加入网络请求。
     *
     * 网络已存在或写入失败时通过 Snackbar 提示并停留在当前页；成功后返回上一页。
     *
     * 线程：数据库写入切换到 IO 线程。
     */
    fun join() {
        val state = _uiState.value
        if (!state.canSubmit) {
            return
        }
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            val networkId = NetworkIdUtils.hexStringToLong(state.networkId)
            val dnsServers = if (state.dnsMode == DNSMode.CUSTOM_DNS) {
                listOf(
                    state.dnsV4Primary to DnsField.V4_PRIMARY,
                    state.dnsV4Secondary to DnsField.V4_SECONDARY,
                    state.dnsV6Primary to DnsField.V6_PRIMARY,
                    state.dnsV6Secondary to DnsField.V6_SECONDARY
                ).filter { (value, field) ->
                    value.isNotEmpty() && isValidDnsAddress(value, field)
                }.map { (value, _) -> value }
            } else {
                emptyList()
            }
            val request = JoinNetworkRequest(
                networkId = networkId,
                networkIdStr = state.networkId,
                isRouteViaZeroTier = state.isRouteViaZeroTier,
                dnsMode = state.dnsMode.toInt(),
                dnsServers = dnsServers
            )
            try {
                val added = withContext(ioDispatcher) {
                    networkRepository.joinNetwork(request)
                }
                if (added) {
                    _effects.emit(JoinNetworkEffect.NavigateBack)
                } else {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effects.emit(
                        JoinNetworkEffect.ShowSnackbar(
                            UiText.Resource(R.string.network_already_exists)
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmitting = false) }
                _effects.emit(
                    JoinNetworkEffect.ShowSnackbar(
                        UiText.Resource(R.string.join_network_failed)
                    )
                )
            }
        }
    }

    /**
     * 判断指定 DNS 模式下是否存在非法地址。
     */
    private fun JoinNetworkUiState.invalidDnsFor(mode: DNSMode): Boolean =
        mode == DNSMode.CUSTOM_DNS && hasInvalidDns(
            v4Primary = dnsV4Primary,
            v4Secondary = dnsV4Secondary,
            v6Primary = dnsV6Primary,
            v6Secondary = dnsV6Secondary
        )

    /**
     * 判断字符是否为 16 进制数字。
     */
    private fun isHexDigit(c: Char): Boolean =
        c.isDigit() || c in 'a'..'f' || c in 'A'..'F'

    companion object {
        private const val NETWORK_ID_LENGTH = 16
    }
}
