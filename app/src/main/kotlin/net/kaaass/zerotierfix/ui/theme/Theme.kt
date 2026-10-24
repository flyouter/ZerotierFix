package net.kaaass.zerotierfix.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/**
 * 浅色配色。
 *
 * 关闭动态取色，保持品牌蓝与中性灰的稳定视觉。
 */
private val LightColors = lightColorScheme(
    primary = Blue600,
    onPrimary = Color.White,
    primaryContainer = Blue100,
    onPrimaryContainer = Blue900,
    inversePrimary = Blue400,
    secondary = Slate600,
    onSecondary = Color.White,
    secondaryContainer = Slate200,
    onSecondaryContainer = Slate900,
    tertiary = Violet600,
    onTertiary = Color.White,
    tertiaryContainer = Violet50,
    onTertiaryContainer = Violet900,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate500,
    surfaceTint = Blue600,
    inverseSurface = Slate900,
    inverseOnSurface = Slate50,
    error = Red600,
    onError = Color.White,
    errorContainer = Red100,
    onErrorContainer = Red900,
    outline = Slate300,
    outlineVariant = Slate200,
    scrim = Color.Black,
    surfaceBright = Color.White,
    surfaceDim = Slate200,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Slate50,
    surfaceContainer = Slate100,
    surfaceContainerHigh = Slate200,
    surfaceContainerHighest = Slate300
)

/**
 * 深色配色。
 */
private val DarkColors = darkColorScheme(
    primary = Blue400,
    onPrimary = Ink900,
    primaryContainer = Blue900,
    onPrimaryContainer = Blue100,
    inversePrimary = Blue600,
    secondary = Slate400,
    onSecondary = Ink900,
    secondaryContainer = Slate800,
    onSecondaryContainer = Slate200,
    tertiary = Violet400,
    onTertiary = Violet900,
    tertiaryContainer = Violet900,
    onTertiaryContainer = Violet50,
    background = Ink900,
    onBackground = Slate100,
    surface = Ink800,
    onSurface = Slate100,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate400,
    surfaceTint = Blue400,
    inverseSurface = Slate100,
    inverseOnSurface = Ink900,
    error = Red400,
    onError = Red950,
    errorContainer = Red950,
    onErrorContainer = Red100,
    outline = Slate700,
    outlineVariant = Slate800,
    scrim = Color.Black,
    surfaceBright = Slate800,
    surfaceDim = Ink900,
    surfaceContainerLowest = Ink900,
    surfaceContainerLow = Ink800,
    surfaceContainer = Ink700,
    surfaceContainerHigh = Slate800,
    surfaceContainerHighest = Ink600
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
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    CompositionLocalProvider(LocalZerotierColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ZerotierTypography,
            shapes = ZerotierShapes,
            content = content
        )
    }
}
