package net.kaaass.zerotierfix.service

import net.kaaass.zerotierfix.util.NetworkInfoUtils.CurrentConnection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [NetworkPolicyController] 移动数据策略判断的纯函数测试。
 */
class NetworkPolicyControllerTest {

    /**
     * 蜂窝且未允许移动网络时不得访问 ZeroTier 内网。
     */
    @Test
    fun cellularNotAllowed() {
        assertFalse(
            NetworkPolicyController.isAllowed(
                CurrentConnection.CONNECTION_MOBILE,
                false
            )
        )
    }

    /**
     * 允许移动网络时蜂窝也可以访问 ZeroTier 内网。
     */
    @Test
    fun cellularAllowed() {
        assertTrue(
            NetworkPolicyController.isAllowed(
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
            NetworkPolicyController.isAllowed(
                CurrentConnection.CONNECTION_OTHER,
                false
            )
        )
    }

    /**
     * 无网络时不判定为受限，由启动检查单独提示无网络。
     */
    @Test
    fun noConnectionNotAllowed() {
        assertTrue(
            NetworkPolicyController.isAllowed(
                CurrentConnection.CONNECTION_NONE,
                false
            )
        )
    }
}
