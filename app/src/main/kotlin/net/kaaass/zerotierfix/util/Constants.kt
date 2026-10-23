package net.kaaass.zerotierfix.util

/**
 * 维护程序中公共的常量。
 */
object Constants {

    /** 是否允许使用移动数据的偏好键。 */
    const val PREF_NETWORK_USE_CELLULAR_DATA = "network_use_cellular_data"

    /** 是否使用自定义 planet 文件的偏好键。 */
    const val PREF_PLANET_USE_CUSTOM = "planet_use_custom"

    /** 自定义 planet 文件路径的偏好键。 */
    const val PREF_SET_PLANET_FILE = "set_planet_file"

    /** 是否禁用 IPv6 的偏好键。 */
    const val PREF_NETWORK_DISABLE_IPV6 = "network_disable_ipv6"

    /** 是否开机启动 ZeroTier 的偏好键。 */
    const val PREF_GENERAL_START_ZEROTIER_ON_BOOT = "general_start_zerotier_on_boot"

    /** 是否禁用“无通知”提示的偏好键。 */
    const val PREF_DISABLE_NO_NOTIFICATION_ALERT = "disable_no_notification_alert"

    /** 自定义 planet 文件名。 */
    const val FILE_CUSTOM_PLANET = "planet.custom"

    /** 临时文件名。 */
    const val FILE_TEMP = "temp"

    /** 默认 planet 文件名。 */
    const val FILE_PLANET = "planet"

    /** 前台通知渠道 ID。 */
    const val CHANNEL_ID = "ZerotierFix"

    /** VPN 会话名称。 */
    const val VPN_SESSION_NAME = "ZerotierFix"
}
