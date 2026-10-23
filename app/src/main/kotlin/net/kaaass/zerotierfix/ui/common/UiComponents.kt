package net.kaaass.zerotierfix.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 带返回按钮的页面顶栏。
 *
 * @param title 标题
 * @param onBack 返回回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        }
    )
}

/**
 * 空列表占位。
 *
 * @param text 提示文本
 * @param modifier 修饰符
 */
@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 两列信息行：左侧标签，右侧取值。
 *
 * @param label 标签
 * @param value 取值
 * @param modifier 修饰符
 * @param monospace 取值是否使用等宽字体
 */
@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    monospace: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            modifier = Modifier.weight(1.4f),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 详情页的两列信息行，值为空时保留占位。
 *
 * @param label 标签
 * @param value 取值
 * @param modifier 修饰符
 */
@Composable
fun DetailRow(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        InfoRow(label = label, value = value)
    }
}
