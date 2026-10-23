package net.kaaass.zerotierfix.ui.common

import androidx.annotation.StringRes

import net.kaaass.zerotierfix.R
import net.kaaass.zerotierfix.model.enums.DNSMode
import net.kaaass.zerotierfix.model.enums.NetworkStatus
import net.kaaass.zerotierfix.model.enums.NetworkType
import net.kaaass.zerotierfix.model.enums.PeerRole

/**
 * 将领域枚举映射为用户可见文案资源。
 *
 * 枚举本身不引用 Android 资源，展示层负责该映射。
 */
object EnumTextFormatter {

    /**
     * 获取 DNS 模式文案。
     *
     * @param mode DNS 模式
     * @return 字符串资源 ID
     */
    @JvmStatic
    @StringRes
    fun dnsMode(mode: DNSMode): Int = when (mode) {
        DNSMode.NO_DNS -> R.string.network_dns_mode_no_dns
        DNSMode.NETWORK_DNS -> R.string.network_dns_mode_network_dns
        DNSMode.CUSTOM_DNS -> R.string.network_dns_mode_custom_dns
    }

    /**
     * 获取网络状态文案。
     *
     * @param status 网络状态
     * @return 字符串资源 ID
     */
    @JvmStatic
    @StringRes
    fun networkStatus(status: NetworkStatus): Int = when (status) {
        NetworkStatus.REQUESTING_CONFIGURATION -> R.string.network_status_requesting_configuration
        NetworkStatus.OK -> R.string.network_status_ok
        NetworkStatus.ACCESS_DENIED -> R.string.network_status_access_denied
        NetworkStatus.NOT_FOUND -> R.string.network_status_not_found
        NetworkStatus.PORT_ERROR -> R.string.network_status_port_error
        NetworkStatus.CLIENT_TOO_OLD -> R.string.network_status_client_too_old
        NetworkStatus.AUTHENTICATION_REQUIRED -> R.string.network_status_authentication_required
        else -> R.string.network_status_unknown
    }

    /**
     * 获取网络类型文案。
     *
     * @param type 网络类型
     * @return 字符串资源 ID
     */
    @JvmStatic
    @StringRes
    fun networkType(type: NetworkType): Int = when (type) {
        NetworkType.PRIVATE -> R.string.network_type_private
        NetworkType.PUBLIC -> R.string.network_type_public
        NetworkType.UNKNOWN -> R.string.network_type_unknown
    }

    /**
     * 获取 Peer 角色文案。
     *
     * @param role Peer 角色
     * @return 字符串资源 ID
     */
    @JvmStatic
    @StringRes
    fun peerRole(role: PeerRole): Int = when (role) {
        PeerRole.PLANET -> R.string.peer_role_planet
        PeerRole.MOON -> R.string.peer_role_moon
        PeerRole.LEAF -> R.string.peer_role_leaf
    }
}
