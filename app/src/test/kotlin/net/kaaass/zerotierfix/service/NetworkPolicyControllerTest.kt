package net.kaaass.zerotierfix.service

import net.kaaass.zerotierfix.util.NetworkInfoUtils.CurrentConnection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [NetworkPolicyController] 出口策略判断的纯函数测试。
 */
class NetworkPolicyControllerTest {

    /**
     * 蜂窝且未允许移动网络时不得安装默认路由。
     */
    @Test
    fun cellularNotAllowed() {
        assertFalse(
            NetworkPolicyController.shouldInstallDefaultRoute(
                CurrentConnection.CONNECTION_MOBILE,
                false
            )
        )
    }

    /**
     * 允许移动网络时蜂窝也可以安装默认路由。
     */
    @Test
    fun cellularAllowed() {
        assertTrue(
            NetworkPolicyController.shouldInstallDefaultRoute(
                CurrentConnection.CONNECTION_MOBILE,
                true
            )
        )
    }

    /**
     * 非蜂窝网络不受移动网络设置影响。
     */
    @Test
    fun nonCellularNotAllowed() {
        assertTrue(
            NetworkPolicyController.shouldInstallDefaultRoute(
                CurrentConnection.CONNECTION_OTHER,
                false
            )
        )
    }

    /**
     * 无网络时保持允许，由隧道配置逻辑自行决定是否重建。
     */
    @Test
    fun noConnectionNotAllowed() {
        assertTrue(
            NetworkPolicyController.shouldInstallDefaultRoute(
                CurrentConnection.CONNECTION_NONE,
                false
            )
        )
    }
}
