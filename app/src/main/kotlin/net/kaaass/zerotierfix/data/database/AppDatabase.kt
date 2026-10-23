package net.kaaass.zerotierfix.data.database

import android.content.Context

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import net.kaaass.zerotierfix.data.database.dao.AppNodeDao
import net.kaaass.zerotierfix.data.database.dao.DnsServerDao
import net.kaaass.zerotierfix.data.database.dao.MoonOrbitDao
import net.kaaass.zerotierfix.data.database.dao.NetworkConfigDao
import net.kaaass.zerotierfix.data.database.dao.NetworkDao
import net.kaaass.zerotierfix.data.database.entity.AppNode
import net.kaaass.zerotierfix.data.database.entity.DnsServer
import net.kaaass.zerotierfix.data.database.entity.MoonOrbit
import net.kaaass.zerotierfix.data.database.entity.Network
import net.kaaass.zerotierfix.data.database.entity.NetworkConfig

/**
 * Room 数据库。
 *
 * 数据库文件沿用 `ztfixdb`。不兼容 GreenDAO 时代的旧库：
 * 版本不匹配时会直接重建数据库。
 */
@Database(
    entities = [
        AppNode::class,
        DnsServer::class,
        MoonOrbit::class,
        Network::class,
        NetworkConfig::class
    ],
    version = 24,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    /** @return 本机节点信息 DAO */
    abstract fun appNodeDao(): AppNodeDao

    /** @return DNS 服务器 DAO */
    abstract fun dnsServerDao(): DnsServerDao

    /** @return Moon 入轨信息 DAO */
    abstract fun moonOrbitDao(): MoonOrbitDao

    /** @return 网络 DAO */
    abstract fun networkDao(): NetworkDao

    /** @return 网络配置 DAO */
    abstract fun networkConfigDao(): NetworkConfigDao

    /**
     * 查询网络列表，并填充关联的 [NetworkConfig]。
     *
     * @return 网络列表
     */
    fun loadNetworks(): List<Network> {
        val networks = networkDao().loadAllOrdered()
        attachConfigs(networks)
        return networks
    }

    /**
     * 查询最近一次激活的网络，并填充关联的 [NetworkConfig]。
     *
     * @return 最近激活的网络列表
     */
    fun loadLastActivatedNetworks(): List<Network> {
        val networks = networkDao().findLastActivated()
        attachConfigs(networks)
        return networks
    }

    /**
     * 按 ID 查询网络，并填充关联的 [NetworkConfig]。
     *
     * @param networkId 网络 ID
     * @return 网络；不存在时返回 `null`
     */
    fun loadNetwork(networkId: Long): Network? {
        val network = networkDao().findById(networkId) ?: return null
        network.networkConfig = networkConfigDao().findById(network.networkConfigId)
        return network
    }

    /**
     * 为网络列表填充关联的网络配置。
     *
     * @param networks 网络列表
     */
    private fun attachConfigs(networks: List<Network>) {
        for (network in networks) {
            network.networkConfig = networkConfigDao().findById(network.networkConfigId)
        }
    }

    companion object {
        private const val DB_NAME = "ztfixdb"

        @Volatile
        private var instance: AppDatabase? = null

        /**
         * 获取数据库单例。
         *
         * @param context 上下文
         * @return 数据库实例
         */
        @JvmStatic
        fun getInstance(context: Context): AppDatabase {
            var result = instance
            if (result == null) {
                synchronized(AppDatabase::class.java) {
                    result = instance
                    if (result == null) {
                        result = Room.databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java,
                            DB_NAME
                        )
                            // 原有代码在 UI 线程同步访问数据库，这里保持相同行为
                            .allowMainThreadQueries()
                            // 旧 GreenDAO 数据库直接丢弃重建
                            .fallbackToDestructiveMigration(true)
                            .build()
                        instance = result
                    }
                }
            }
            return result!!
        }
    }
}
