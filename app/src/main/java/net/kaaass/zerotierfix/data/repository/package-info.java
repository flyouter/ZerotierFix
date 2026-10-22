/**
 * 数据仓库层。
 *
 * <p>Repository 负责组合 DAO、事务与实体/DTO 映射；UI 与 Service 只能依赖本层的接口，
 * 不再直接访问 DAO 或 {@code DaoSession}。接口与实现放在同一包内。</p>
 */
package net.kaaass.zerotierfix.data.repository;
