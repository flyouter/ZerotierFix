/**
 * ZeroTier 引擎门面与编排。
 *
 * <p>对外暴露 {@code VpnEngine}，组合 {@code vpn.node}、{@code vpn.tun}、
 * {@code vpn.route}、{@code vpn.config}、{@code vpn.store} 的能力；
 * 不处理 Android {@code VpnService} 生命周期（由 {@code service} 负责）。</p>
 *
 * <p>当前待从 {@code service.ZeroTierOneService} 中拆分。</p>
 */
package net.kaaass.zerotierfix.vpn.engine;
