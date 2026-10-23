package net.kaaass.zerotierfix.model.enums

/**
 * 网络 DNS 模式，持久化值为 [toInt]。
 */
enum class DNSMode(private val id: Int) {

    /**
     * 不使用 DNS。
     */
    NO_DNS(0),

    /**
     * 使用 ZeroTier 网络下发的 DNS。
     */
    NETWORK_DNS(1),

    /**
     * 使用用户自定义的 DNS。
     */
    CUSTOM_DNS(2);

    /**
     * 获取持久化值。
     *
     * @return 持久化用的整数值
     */
    fun toInt(): Int = this.id

    companion object {
        /**
         * 将持久化值转换为枚举。
         *
         * @param id 持久化值
         * @return 对应枚举
         * @throws IllegalArgumentException 值未知时抛出
         */
        @JvmStatic
        fun fromInt(id: Int): DNSMode = when (id) {
            0 -> NO_DNS
            1 -> NETWORK_DNS
            2 -> CUSTOM_DNS
            else -> throw IllegalArgumentException("Unhandled DNS mode: $id")
        }
    }
}
