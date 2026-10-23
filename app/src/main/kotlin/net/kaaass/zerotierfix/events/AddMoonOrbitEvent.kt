package net.kaaass.zerotierfix.events

/**
 * 增加 Moon 入轨信息事件
 *
 * 当需要入轨某一 Moon 时触发，如用户配置入轨信息。该事件处理应仅进行入轨信息的管理，实际请求 ZT 的事件为
 * [OrbitMoonEvent]
 */
data class AddMoonOrbitEvent(
    /**
     * Moon 地址。
     */
    val moonWorldId: Long?,

    /**
     * Moon 种子。
     */
    val moonSeed: Long?,

    /**
     * 是否从文件添加。
     */
    val isFromFile: Boolean
)
