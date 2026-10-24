package net.kaaass.zerotierfix.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Material 3 之外的语义状态色。
 *
 * Material 3 没有 success / warning 角色，这里补充连接状态、延迟与危险操作
 * 所需的颜色。状态色只用于圆点、图标和容器背景，文字必须同时提供文本标签，
 * 不能仅靠颜色传达信息。
 *
 * @property connected 已连接 / 在线 / 直连
 * @property onConnectedContainer 连接态容器上的文字
 * @property connectedContainer 连接态容器背景
 * @property connecting 连接中 / 中继 / 警告
 * @property onConnectingContainer 连接中容器上的文字
 * @property connectingContainer 连接中容器背景
 * @property offline 未连接 / 未知
 * @property danger 错误 / 危险操作
 * @property onDangerContainer 危险容器上的文字
 * @property dangerContainer 危险容器背景
 * @property neutralContainer 中性徽标背景
 * @property onNeutralContainer 中性徽标文字
 */
@Immutable
data class ZerotierExtendedColors(
    val connected: Color,
    val onConnectedContainer: Color,
    val connectedContainer: Color,
    val connecting: Color,
    val onConnectingContainer: Color,
    val connectingContainer: Color,
    val offline: Color,
    val danger: Color,
    val onDangerContainer: Color,
    val dangerContainer: Color,
    val neutralContainer: Color,
    val onNeutralContainer: Color
)

/** 浅色主题的语义状态色。 */
internal val LightExtendedColors = ZerotierExtendedColors(
    connected = Green600,
    onConnectedContainer = Green950,
    connectedContainer = Green100,
    connecting = Amber600,
    onConnectingContainer = Amber950,
    connectingContainer = Amber100,
    offline = Slate500,
    danger = Red600,
    onDangerContainer = Red900,
    dangerContainer = Red100,
    neutralContainer = Slate200,
    onNeutralContainer = Slate800
)

/** 深色主题的语义状态色。 */
internal val DarkExtendedColors = ZerotierExtendedColors(
    connected = Green400,
    onConnectedContainer = Green400,
    connectedContainer = Green950,
    connecting = Amber400,
    onConnectingContainer = Amber400,
    connectingContainer = Amber950,
    offline = Slate400,
    danger = Red400,
    onDangerContainer = Red400,
    dangerContainer = Red950,
    neutralContainer = Slate800,
    onNeutralContainer = Slate200
)

/** 当前主题的语义状态色。 */
internal val LocalZerotierColors = staticCompositionLocalOf { LightExtendedColors }

/**
 * 读取当前主题的语义状态色。
 */
val MaterialTheme.zerotierColors: ZerotierExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalZerotierColors.current
