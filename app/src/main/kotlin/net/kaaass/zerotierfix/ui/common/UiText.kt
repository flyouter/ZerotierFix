package net.kaaass.zerotierfix.ui.common

import android.content.Context

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * 可延迟解析的用户可见文本。
 *
 * ViewModel 只产生资源 ID，由 UI 层解析为本地化字符串。
 */
sealed interface UiText {

    /**
     * 字符串资源。
     *
     * @property id 字符串资源 ID
     * @property args 格式化参数
     */
    data class Resource(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    /**
     * 运行时拼接的纯文本。
     *
     * @property value 文本内容
     */
    data class Plain(val value: String) : UiText
}

/**
 * 在非 Composable 环境（如 Toast）解析文本。
 *
 * @param context 上下文
 * @return 本地化字符串
 */
fun UiText.resolve(context: Context): String = when (this) {
    is UiText.Plain -> value
    is UiText.Resource -> context.getString(id, *args.toTypedArray())
}

/**
 * 在 Composable 环境解析文本。
 *
 * @return 本地化字符串
 */
@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Plain -> value
    is UiText.Resource -> stringResource(id, *args.toTypedArray())
}
