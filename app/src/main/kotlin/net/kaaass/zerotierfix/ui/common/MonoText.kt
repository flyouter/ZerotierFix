package net.kaaass.zerotierfix.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow

/**
 * 等宽文本，用于网络 ID、地址、路径与版本号。
 *
 * @param value 文本内容
 * @param modifier 修饰符
 * @param style 基础样式，字体会被覆盖为等宽字体
 * @param color 文本颜色；默认使用 `Color.Unspecified` 继承当前内容色
 * @param maxLines 最大行数
 * @param overflow 溢出策略
 */
@Composable
fun MonoText(
    value: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val monospaceStyle = remember(style) {
        style.copy(fontFamily = FontFamily.Monospace)
    }
    Text(
        text = value,
        modifier = modifier,
        style = monospaceStyle,
        color = color,
        maxLines = maxLines,
        overflow = overflow
    )
}
