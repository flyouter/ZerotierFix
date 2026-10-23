package net.kaaass.zerotierfix.features.moon

import net.kaaass.zerotierfix.model.dto.MoonOrbitDto
import net.kaaass.zerotierfix.ui.common.UiText

/**
 * Moon 入轨页面状态。
 *
 * @property moons 已保存的入轨信息
 * @property isLoading 是否正在加载
 */
data class MoonOrbitUiState(
    val moons: List<MoonOrbitDto> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * Moon 入轨页面的一次性事件。
 */
sealed interface MoonOrbitEffect {

    /**
     * 显示 Toast。
     *
     * @property text 文本
     */
    data class ShowToast(val text: UiText) : MoonOrbitEffect

    /**
     * 显示 Snackbar。
     *
     * @property text 文本
     */
    data class ShowSnackbar(val text: UiText) : MoonOrbitEffect
}
