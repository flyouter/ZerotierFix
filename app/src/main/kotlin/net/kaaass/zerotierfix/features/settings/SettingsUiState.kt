package net.kaaass.zerotierfix.features.settings

import net.kaaass.zerotierfix.ui.common.UiText

/**
 * 设置页面状态。
 *
 * @property isCellularDataAllowed 是否允许在移动网络下访问 ZeroTier 内网
 * @property isIpv6Disabled 是否禁用 IPv6
 * @property isPlanetUseCustom 是否使用自定义 planet
 * @property hasCustomPlanetFile 是否已配置自定义 planet 文件
 * @property isPlanetDialogVisible 是否显示 planet 文件来源对话框
 * @property isUrlDialogVisible 是否显示 URL 输入对话框
 * @property urlText URL 输入值
 * @property isLoadingDialogVisible 是否显示下载进度对话框
 * @property appVersion 应用版本号
 */
data class SettingsUiState(
    val isCellularDataAllowed: Boolean = false,
    val isIpv6Disabled: Boolean = false,
    val isPlanetUseCustom: Boolean = false,
    val hasCustomPlanetFile: Boolean = false,
    val isPlanetDialogVisible: Boolean = false,
    val isUrlDialogVisible: Boolean = false,
    val urlText: String = "",
    val isLoadingDialogVisible: Boolean = false,
    val appVersion: String = ""
)

/**
 * 设置页面的一次性事件。
 */
sealed interface SettingsEffect {

    /**
     * 显示 Toast。
     *
     * @property text 文本
     */
    data class ShowToast(val text: UiText) : SettingsEffect

    /**
     * 显示 Snackbar。
     *
     * @property text 文本
     */
    data class ShowSnackbar(val text: UiText) : SettingsEffect
}
