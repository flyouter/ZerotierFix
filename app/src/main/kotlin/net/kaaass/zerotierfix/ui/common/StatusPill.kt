package net.kaaass.zerotierfix.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import net.kaaass.zerotierfix.ui.theme.ZerotierFixTheme
import net.kaaass.zerotierfix.ui.theme.zerotierColors

/**
 * 状态胶囊的语义色调。
 */
enum class StatusTone {
    /** 正常、已连接、在线。 */
    Connected,

    /** 进行中、连接中、中继。 */
    Connecting,

    /** 未连接、未知。 */
    Offline,

    /** 错误、失败。 */
    Danger,

    /** 中性信息，如 Peer 角色。 */
    Neutral
}

/**
 * 圆点 + 文本的状态胶囊。
 *
 * 颜色只作辅助，状态含义始终由文本传达。
 *
 * @param label 状态文本
 * @param tone 语义色调
 * @param modifier 修饰符
 */
@Composable
fun StatusPill(
    label: String,
    tone: StatusTone,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.zerotierColors
    val background: Color
    val content: Color
    val dot: Color
    when (tone) {
        StatusTone.Connected -> {
            background = colors.connectedContainer
            content = colors.onConnectedContainer
            dot = colors.connected
        }
        StatusTone.Connecting -> {
            background = colors.connectingContainer
            content = colors.onConnectingContainer
            dot = colors.connecting
        }
        StatusTone.Offline -> {
            background = colors.neutralContainer
            content = colors.onNeutralContainer
            dot = colors.offline
        }
        StatusTone.Danger -> {
            background = colors.dangerContainer
            content = colors.onDangerContainer
            dot = colors.danger
        }
        StatusTone.Neutral -> {
            background = colors.neutralContainer
            content = colors.onNeutralContainer
            dot = colors.offline
        }
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(dot)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusPillPreview() {
    ZerotierFixTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill("已连接", StatusTone.Connected)
            StatusPill("正在连接…", StatusTone.Connecting)
            StatusPill("未连接", StatusTone.Offline)
            StatusPill("访问拒绝", StatusTone.Danger)
            StatusPill("Planet", StatusTone.Neutral)
        }
    }
}
