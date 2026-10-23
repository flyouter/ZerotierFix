package net.kaaass.zerotierfix.data.repository

import android.util.Log

import net.kaaass.zerotierfix.data.database.AppDatabase
import net.kaaass.zerotierfix.data.database.DatabaseUtils
import net.kaaass.zerotierfix.data.database.entity.DnsServer
import net.kaaass.zerotierfix.data.database.entity.Network
import net.kaaass.zerotierfix.data.database.entity.NetworkConfig
import net.kaaass.zerotierfix.model.dto.JoinNetworkRequest
import net.kaaass.zerotierfix.model.dto.NetworkConfigDto
import net.kaaass.zerotierfix.model.dto.NetworkSummary
import net.kaaass.zerotierfix.model.enums.DNSMode

import javax.inject.Inject
import javax.inject.Singleton

/**
 * [NetworkRepository] 的 Room 实现。
 */
@Singleton
class NetworkRepositoryImpl @Inject constructor(
    private val database: AppDatabase
) : NetworkRepository {

    override fun getAllNetworks(): List<NetworkSummary> {
        DatabaseUtils.readLock.lock()
        try {
            val networks = database.loadNetworks()
            val result = ArrayList<NetworkSummary>(networks.size)
            for (network in networks) {
                result.add(toSummary(network))
            }
            return result
        } finally {
            DatabaseUtils.readLock.unlock()
        }
    }

    override fun getNetworkSummary(networkId: Long): NetworkSummary? {
        DatabaseUtils.readLock.lock()
        try {
            val network = database.loadNetwork(networkId)
            return if (network == null) null else toSummary(network)
        } finally {
            DatabaseUtils.readLock.unlock()
        }
    }

    override fun getNetworkConfig(networkId: Long): NetworkConfigDto? {
        DatabaseUtils.readLock.lock()
        try {
            val network = database.loadNetwork(networkId)
            if (network == null || network.networkConfig == null) {
                return null
            }
            val config = network.networkConfig!!
            val dnsServers = ArrayList<String>()
            for (server in database.dnsServerDao().findByNetworkId(networkId)) {
                // 数据库列允许 null；原 Java 实现会原样加入列表，这里保持相同行为
                @Suppress("UNCHECKED_CAST")
                (dnsServers as MutableList<String?>).add(server.nameserver)
            }
            return NetworkConfigDto(
                config.routeViaZeroTier,
                config.dnsMode,
                dnsServers
            )
        } finally {
            DatabaseUtils.readLock.unlock()
        }
    }

    override fun getLastActivatedNetworkId(): Long? {
        DatabaseUtils.readLock.lock()
        try {
            val networks = database.loadLastActivatedNetworks()
            if (networks.isEmpty()) {
                return null
            }
            if (networks.size > 1) {
                Log.e(TAG, "Multiple networks marked as last activated: " + networks.size)
                return null
            }
            return networks[0].networkId
        } finally {
            DatabaseUtils.readLock.unlock()
        }
    }

    override fun joinNetwork(request: JoinNetworkRequest): Boolean {
        DatabaseUtils.writeLock.lock()
        try {
            var added = false
            database.runInTransaction {
                if (database.networkDao().countById(request.networkId) > 0) {
                    return@runInTransaction
                }
                val config = NetworkConfig(
                    request.networkId,
                    request.isRouteViaZeroTier,
                    request.dnsMode
                )
                database.networkConfigDao().insert(config)
                if (request.dnsMode == DNSMode.CUSTOM_DNS.toInt()) {
                    for (server in request.dnsServers) {
                        database.dnsServerDao().insert(
                            DnsServer(null, request.networkId, server)
                        )
                    }
                }
                database.networkDao().insert(
                    Network(
                        request.networkId,
                        request.networkIdStr,
                        null,
                        false,
                        request.networkId
                    )
                )
                added = true
            }
            return added
        } finally {
            DatabaseUtils.writeLock.unlock()
        }
    }

    override fun setLastActivated(networkId: Long) {
        DatabaseUtils.writeLock.lock()
        try {
            database.runInTransaction {
                val networks = database.networkDao().loadAllOrdered()
                for (network in networks) {
                    val id = network.networkId
                    val active = id != null && id == networkId
                    if (network.lastActivated != active) {
                        network.lastActivated = active
                        database.networkDao().update(network)
                    }
                }
            }
        } finally {
            DatabaseUtils.writeLock.unlock()
        }
    }

    override fun updateNetworkName(networkId: Long, networkName: String?) {
        if (networkName == null || networkName.isEmpty()) {
            return
        }
        DatabaseUtils.writeLock.lock()
        try {
            val network = database.networkDao().findById(networkId)
            if (network == null) {
                return
            }
            network.networkName = networkName
            database.networkDao().update(network)
        } finally {
            DatabaseUtils.writeLock.unlock()
        }
    }

    override fun updateRouteViaZeroTier(networkId: Long, routeViaZeroTier: Boolean) {
        DatabaseUtils.writeLock.lock()
        try {
            val network = database.loadNetwork(networkId)
            if (network == null || network.networkConfig == null) {
                return
            }
            val config = network.networkConfig!!
            config.routeViaZeroTier = routeViaZeroTier
            database.networkConfigDao().update(config)
        } finally {
            DatabaseUtils.writeLock.unlock()
        }
    }

    override fun deleteNetwork(networkId: Long) {
        DatabaseUtils.writeLock.lock()
        try {
            database.runInTransaction {
                val network = database.networkDao().findById(networkId)
                if (network == null) {
                    return@runInTransaction
                }
                database.dnsServerDao().deleteByNetworkId(networkId)
                database.networkConfigDao().deleteById(network.networkConfigId)
                database.networkDao().delete(network)
            }
        } finally {
            DatabaseUtils.writeLock.unlock()
        }
    }

    /**
     * 将实体转换为列表用的概要 DTO。
     *
     * @param network 网络实体，不能为空
     * @return 网络概要
     */
    private fun toSummary(network: Network): NetworkSummary = NetworkSummary(
        network.networkId!!,
        network.networkIdStr,
        network.networkName,
        network.lastActivated
    )

    companion object {
        private const val TAG = "NetworkRepository"
    }
}
