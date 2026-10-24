package net.kaaass.zerotierfix.vpn.engine

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [awaitServiceState] 的单元测试。
 */
class ServiceStateWaitTest {

    /**
     * 当前状态已经是目标时立即成功。
     */
    @Test
    fun `已是目标状态时立即返回`() = runTest {
        val state = MutableStateFlow(ServiceState.STOPPED)

        assertTrue(awaitServiceState(state, ServiceState.STOPPED, 1_000L))
    }

    /**
     * 状态在等待期间变化时能够在超时前返回成功。
     */
    @Test
    fun `等待期间进入目标状态返回成功`() = runTest {
        val state = MutableStateFlow(ServiceState.STARTING)
        launch {
            delay(100L)
            state.value = ServiceState.RUNNING
        }

        assertTrue(awaitServiceState(state, ServiceState.RUNNING, 1_000L))
    }

    /**
     * 状态始终停留在 {@code RUNNING} 时不能把停止当成成功。
     *
     * 这是“磁贴关闭后实际未断开”的回归测试。
     */
    @Test
    fun `始终未进入停止状态时超时返回失败`() = runTest {
        val state = MutableStateFlow(ServiceState.RUNNING)

        assertFalse(awaitServiceState(state, ServiceState.STOPPED, 1_000L))
    }

    /**
     * 从启动中直接落到停止时也算断开成功。
     */
    @Test
    fun `启动中直接停止返回成功`() = runTest {
        val state = MutableStateFlow(ServiceState.STARTING)
        launch {
            delay(100L)
            state.value = ServiceState.STOPPED
        }

        assertTrue(awaitServiceState(state, ServiceState.STOPPED, 1_000L))
    }
}
