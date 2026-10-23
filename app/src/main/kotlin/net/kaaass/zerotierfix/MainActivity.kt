package net.kaaass.zerotierfix

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier

import dagger.hilt.android.AndroidEntryPoint

import net.kaaass.zerotierfix.navigation.ZeroTierNavHost
import net.kaaass.zerotierfix.ui.theme.ZerotierFixTheme

/**
 * 应用唯一的 Activity，承载全部 Compose 页面。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZerotierFixTheme {
                // Android 15+ 强制 edge-to-edge，且部分 ROM 不会把状态栏 inset 传到
                // Compose 的 Scaffold/TopAppBar。这里统一留出系统栏与刘海空间，再消费
                // 掉 inset，避免内部组件重复添加相同 padding。
                val rootInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(rootInsets)
                ) {
                    Box(modifier = Modifier.consumeWindowInsets(rootInsets)) {
                        ZeroTierNavHost()
                    }
                }
            }
        }
    }
}
