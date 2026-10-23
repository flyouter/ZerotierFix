package net.kaaass.zerotierfix

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

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
                ZeroTierNavHost()
            }
        }
    }
}
