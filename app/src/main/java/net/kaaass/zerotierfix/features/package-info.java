/**
 * 功能纵切包。
 *
 * <p>每个子包对应一个页面/功能，内部包含 Activity、Fragment、Adapter、ViewModel、
 * UiState 与页面级逻辑。页面只能通过 Repository、{@code VpnEngine} 门面或
 * {@code service} 访问数据与引擎，不得直接依赖 DAO、SDK 或另一个 feature 的内部类。</p>
 */
package net.kaaass.zerotierfix.features;
