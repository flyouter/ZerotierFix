package net.kaaass.zerotierfix.ui.common;

import androidx.annotation.StringRes;

import net.kaaass.zerotierfix.R;
import net.kaaass.zerotierfix.model.enums.DNSMode;
import net.kaaass.zerotierfix.model.enums.NetworkStatus;
import net.kaaass.zerotierfix.model.enums.NetworkType;
import net.kaaass.zerotierfix.model.enums.PeerRole;

/**
 * 将领域枚举映射为用户可见文案资源。
 *
 * <p>枚举本身不引用 Android 资源，展示层负责该映射。</p>
 */
public final class EnumTextFormatter {

    private EnumTextFormatter() {
    }

    /**
     * 获取 DNS 模式文案。
     *
     * @param mode DNS 模式
     * @return 字符串资源 ID
     */
    @StringRes
    public static int dnsMode(DNSMode mode) {
        switch (mode) {
            case NO_DNS:
                return R.string.network_dns_mode_no_dns;
            case NETWORK_DNS:
                return R.string.network_dns_mode_network_dns;
            case CUSTOM_DNS:
                return R.string.network_dns_mode_custom_dns;
            default:
                return R.string.network_dns_mode_no_dns;
        }
    }

    /**
     * 获取网络状态文案。
     *
     * @param status 网络状态
     * @return 字符串资源 ID
     */
    @StringRes
    public static int networkStatus(NetworkStatus status) {
        switch (status) {
            case REQUESTING_CONFIGURATION:
                return R.string.network_status_requesting_configuration;
            case OK:
                return R.string.network_status_ok;
            case ACCESS_DENIED:
                return R.string.network_status_access_denied;
            case NOT_FOUND:
                return R.string.network_status_not_found;
            case PORT_ERROR:
                return R.string.network_status_port_error;
            case CLIENT_TOO_OLD:
                return R.string.network_status_client_too_old;
            case AUTHENTICATION_REQUIRED:
                return R.string.network_status_authentication_required;
            default:
                return R.string.network_status_unknown;
        }
    }

    /**
     * 获取网络类型文案。
     *
     * @param type 网络类型
     * @return 字符串资源 ID
     */
    @StringRes
    public static int networkType(NetworkType type) {
        switch (type) {
            case PRIVATE:
                return R.string.network_type_private;
            case PUBLIC:
                return R.string.network_type_public;
            default:
                return R.string.network_type_unknown;
        }
    }

    /**
     * 获取 Peer 角色文案。
     *
     * @param role Peer 角色
     * @return 字符串资源 ID
     */
    @StringRes
    public static int peerRole(PeerRole role) {
        switch (role) {
            case PLANET:
                return R.string.peer_role_planet;
            case MOON:
                return R.string.peer_role_moon;
            case LEAF:
            default:
                return R.string.peer_role_leaf;
        }
    }
}
