package net.kaaass.zerotierfix.events

/**
 * 删除 Moon 入轨信息事件。
 */
data class RemoveMoonOrbitEvent(
    /**
     * Moon 地址。
     */
    val moonWorldId: Long?,

    /**
     * Moon 种子。
     */
    val moonSeed: Long?
)
