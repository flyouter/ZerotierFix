package net.kaaass.zerotierfix.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 浅色配色。
 *
 * 沿用原 Material Components 的紫色 / 青色品牌色，关闭动态取色。
 */
private val LightColors = lightColorScheme(
    primary = Purple500,
    onPrimary = Color.White,
    primaryContainer = Purple200,
    onPrimaryContainer = Color.Black,
    secondary = Teal200,
    onSecondary = Color.Black,
    secondaryContainer = Teal200,
    onSecondaryContainer = Color.Black,
    tertiary = Teal700,
    onTertiary = Color.White
)

/**
 * 深色配色。
 */
private val DarkColors = darkColorScheme(
    primary = Purple200,
    onPrimary = Color.Black,
    primaryContainer = Purple700,
    onPrimaryContainer = Color.White,
    secondary = Teal200,
    onSecondary = Color.Black,
    secondaryContainer = Teal700,
    onSecondaryContainer = Color.White,
    tertiary = Teal200,
    onTertiary = Color.Black
)

/**
 * 应用主题。
 *
 * @param darkTheme 是否使用深色主题，默认跟随系统
 * @param content 页面内容
 */
@Composable
fun ZerotierFixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = ZerotierTypography,
        content = content
    )
}
