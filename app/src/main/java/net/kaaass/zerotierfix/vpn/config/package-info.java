/**
 * VPN 隧道与 DNS 配置。
 *
 * <p>负责把 ZeroTier 网络配置转换为 Android {@code VpnService.Builder} 参数：
 * 地址、路由、DNS、MTU、允许/禁止的 App 等。不处理界面与数据库。</p>
 *
 * <p>当前待从 {@code service.ZeroTierOneService#updateTunnelConfig} 与
 * {@code addDNSServers} 中拆分。</p>
 */
package net.kaaass.zerotierfix.vpn.config;
