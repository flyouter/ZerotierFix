package net.kaaass.zerotierfix.data.repository

import android.content.SharedPreferences

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import net.kaaass.zerotierfix.util.Constants

import javax.inject.Inject
import javax.inject.Singleton

/**
 * [SettingsRepository] 的 SharedPreferences 实现。
 *
 * 构造时注册进程级监听器，把偏好变化同步到 [settings]；
 * 由于仓库是单例，监听器生命周期与进程一致，不会泄漏页面。
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val preferences: SharedPreferences
) : SettingsRepository {

    private val _settings = MutableStateFlow(readSettings())

    override val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _settings.value = readSettings()
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    override val isCellularDataAllowed: Boolean
        get() = preferences.getBoolean(Constants.PREF_NETWORK_USE_CELLULAR_DATA, false)

    override val isIpv6Disabled: Boolean
        get() = preferences.getBoolean(Constants.PREF_NETWORK_DISABLE_IPV6, false)

    override val isPlanetUseCustom: Boolean
        get() = preferences.getBoolean(Constants.PREF_PLANET_USE_CUSTOM, false)

    override val isStartOnBoot: Boolean
        get() = preferences.getBoolean(Constants.PREF_GENERAL_START_ZEROTIER_ON_BOOT, true)

    override val isNoNotificationAlertDisabled: Boolean
        get() = preferences.getBoolean(Constants.PREF_DISABLE_NO_NOTIFICATION_ALERT, false)

    override fun setCellularDataAllowed(value: Boolean) {
        preferences.edit().putBoolean(Constants.PREF_NETWORK_USE_CELLULAR_DATA, value).apply()
    }

    override fun setIpv6Disabled(value: Boolean) {
        preferences.edit().putBoolean(Constants.PREF_NETWORK_DISABLE_IPV6, value).apply()
    }

    override fun setPlanetUseCustom(value: Boolean) {
        preferences.edit().putBoolean(Constants.PREF_PLANET_USE_CUSTOM, value).apply()
    }

    override fun setStartOnBoot(value: Boolean) {
        preferences.edit().putBoolean(Constants.PREF_GENERAL_START_ZEROTIER_ON_BOOT, value).apply()
    }

    override fun setNoNotificationAlertDisabled(value: Boolean) {
        preferences.edit().putBoolean(Constants.PREF_DISABLE_NO_NOTIFICATION_ALERT, value).apply()
    }

    override fun setDefaultRouteEnabled(networkIdStr: String, enabled: Boolean) {
        val current = preferences.getStringSet(DEFAULT_ROUTE_ENABLED_NETWORKS, null)
        val next = current?.toMutableSet() ?: mutableSetOf()
        if (enabled) {
            next.add(networkIdStr)
        } else {
            next.remove(networkIdStr)
        }
        preferences.edit().putStringSet(DEFAULT_ROUTE_ENABLED_NETWORKS, next).apply()
    }

    /**
     * 读取当前设置快照。
     *
     * @return 设置快照
     */
    private fun readSettings(): Settings = Settings(
        isCellularDataAllowed = isCellularDataAllowed,
        isIpv6Disabled = isIpv6Disabled,
        isPlanetUseCustom = isPlanetUseCustom,
        isStartOnBoot = isStartOnBoot,
        isNoNotificationAlertDisabled = isNoNotificationAlertDisabled
    )

    companion object {
        /** 只写不读的历史偏好键。 */
        private const val DEFAULT_ROUTE_ENABLED_NETWORKS = "default_route_enabled_networks"
    }
}
