package net.kaaass.zerotierfix.data.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * 应用设置快照。
 */
data class Settings(
    /**
     * 是否允许在移动网络下通过 ZeroTier 出口路由流量。
     *
     * 为 `false` 时 VPN 仍可运行，但不安装默认路由（`0.0.0.0/0`、`::/0`）。
     */
    val isCellularDataAllowed: Boolean,

    /**
     * 是否禁用 IPv6。
     */
    val isIpv6Disabled: Boolean,

    /**
     * 是否使用自定义 planet 文件。
     */
    val isPlanetUseCustom: Boolean,

    /**
     * 是否不再显示“无通知权限”提示。
     */
    val isNoNotificationAlertDisabled: Boolean
)

/**
 * 应用设置仓库。
 *
 * 直接封装默认 SharedPreferences，保留原有 key 与默认值；同步 getter 可在
 * 任意线程读取，[settings] 用于 Compose 观察。
 */
interface SettingsRepository {

    /** 设置快照，随 SharedPreferences 变化更新。 */
    val settings: StateFlow<Settings>

    /** 是否允许在移动网络下通过 ZeroTier 出口路由流量。 */
    val isCellularDataAllowed: Boolean

    /** 是否禁用 IPv6。 */
    val isIpv6Disabled: Boolean

    /** 是否使用自定义 planet。 */
    val isPlanetUseCustom: Boolean

    /** 是否不再显示无通知权限提示。 */
    val isNoNotificationAlertDisabled: Boolean

    /**
     * 设置是否允许在移动网络下通过 ZeroTier 出口路由流量。
     *
     * @param value 是否允许
     */
    fun setCellularDataAllowed(value: Boolean)

    /**
     * 设置是否禁用 IPv6。
     *
     * @param value 是否禁用
     */
    fun setIpv6Disabled(value: Boolean)

    /**
     * 设置是否使用自定义 planet。
     *
     * @param value 是否使用
     */
    fun setPlanetUseCustom(value: Boolean)

    /**
     * 设置是否不再显示无通知权限提示。
     *
     * @param value 是否不再显示
     */
    fun setNoNotificationAlertDisabled(value: Boolean)

    /**
     * 写入“默认路由启用网络”集合。
     *
     * 该 key 当前只写不读，保留原有写入行为。
     *
     * @param networkIdStr 网络 ID 字符串
     * @param enabled 是否启用
     */
    fun setDefaultRouteEnabled(networkIdStr: String, enabled: Boolean)
}
