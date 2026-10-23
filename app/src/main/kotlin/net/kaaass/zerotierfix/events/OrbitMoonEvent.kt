package net.kaaass.zerotierfix.events

import net.kaaass.zerotierfix.model.dto.MoonOrbitDto

/**
 * Moon 入轨事件。
 *
 * 事件发生时应实际处理入轨动作。
 */
data class OrbitMoonEvent(
    /**
     * 待入轨 Moon 的信息，不能为空。
     */
    val moonOrbits: List<MoonOrbitDto>
)
