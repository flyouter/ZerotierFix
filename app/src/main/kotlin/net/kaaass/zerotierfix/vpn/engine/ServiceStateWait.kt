package net.kaaass.zerotierfix.vpn.engine

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 等待服务进入指定状态。
 *
 * 与“等待任意稳定状态”不同，这里只认目标状态：停止后必须真正看到
 * `STOPPED` 才算成功，避免把仍然 `RUNNING` 当成已断开。
 *
 * @param state 服务状态流
 * @param target 目标状态
 * @param timeoutMs 最长等待时间
 * @return 在超时前到达目标状态返回 `true`
 */
suspend fun awaitServiceState(
    state: StateFlow<ServiceState>,
    target: ServiceState,
    timeoutMs: Long
): Boolean = withTimeoutOrNull(timeoutMs) {
    state.first { it == target }
} != null
