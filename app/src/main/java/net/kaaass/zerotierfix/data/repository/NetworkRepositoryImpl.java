package net.kaaass.zerotierfix.data.repository;

import android.util.Log;

import net.kaaass.zerotierfix.data.database.DatabaseUtils;
import net.kaaass.zerotierfix.data.database.entity.AssignedAddress;
import net.kaaass.zerotierfix.data.database.entity.AssignedAddressDao;
import net.kaaass.zerotierfix.data.database.entity.DaoSession;
import net.kaaass.zerotierfix.data.database.entity.DnsServer;
import net.kaaass.zerotierfix.data.database.entity.DnsServerDao;
import net.kaaass.zerotierfix.data.database.entity.Network;
import net.kaaass.zerotierfix.data.database.entity.NetworkConfig;
import net.kaaass.zerotierfix.data.database.entity.NetworkConfigDao;
import net.kaaass.zerotierfix.data.database.entity.NetworkDao;
import net.kaaass.zerotierfix.model.dto.JoinNetworkRequest;
import net.kaaass.zerotierfix.model.dto.NetworkConfigDto;
import net.kaaass.zerotierfix.model.dto.NetworkSummary;
import net.kaaass.zerotierfix.model.enums.DNSMode;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link NetworkRepository} 的 GreenDAO 实现。
 */
public final class NetworkRepositoryImpl implements NetworkRepository {

    private static final String TAG = "NetworkRepository";

    private final DaoSession daoSession;

    /**
     * 创建网络仓库。
     *
     * @param daoSession 应用级数据库会话，不能为空
     */
    public NetworkRepositoryImpl(DaoSession daoSession) {
        this.daoSession = daoSession;
    }

    @Override
    public List<NetworkSummary> getAllNetworks() {
        DatabaseUtils.readLock.lock();
        try {
            List<Network> networks = this.daoSession.getNetworkDao()
                    .queryBuilder()
                    .orderAsc(NetworkDao.Properties.NetworkId)
                    .list();
            List<NetworkSummary> result = new ArrayList<>(networks.size());
            for (Network network : networks) {
                result.add(toSummary(network));
            }
            return result;
        } finally {
            DatabaseUtils.readLock.unlock();
        }
    }

    @Override
    public NetworkSummary getNetworkSummary(long networkId) {
        DatabaseUtils.readLock.lock();
        try {
            Network network = this.daoSession.getNetworkDao().load(networkId);
            return network == null ? null : toSummary(network);
        } finally {
            DatabaseUtils.readLock.unlock();
        }
    }

    @Override
    public NetworkConfigDto getNetworkConfig(long networkId) {
        DatabaseUtils.readLock.lock();
        try {
            Network network = this.daoSession.getNetworkDao().load(networkId);
            if (network == null) {
                return null;
            }
            NetworkConfig config = this.daoSession.getNetworkConfigDao()
                    .load(network.getNetworkConfigId());
            if (config == null) {
                return null;
            }
            List<String> dnsServers = new ArrayList<>();
            List<DnsServer> servers = config.getDnsServers();
            if (servers != null) {
                for (DnsServer server : servers) {
                    dnsServers.add(server.getNameserver());
                }
            }
            return new NetworkConfigDto(
                    config.getRouteViaZeroTier(),
                    config.getDnsMode(),
                    dnsServers
            );
        } finally {
            DatabaseUtils.readLock.unlock();
        }
    }

    @Override
    public Long getLastActivatedNetworkId() {
        DatabaseUtils.readLock.lock();
        try {
            List<Network> networks = this.daoSession.getNetworkDao()
                    .queryBuilder()
                    .where(NetworkDao.Properties.LastActivated.eq(true))
                    .list();
            if (networks.isEmpty()) {
                return null;
            }
            if (networks.size() > 1) {
                Log.e(TAG, "Multiple networks marked as last activated: " + networks.size());
                return null;
            }
            return networks.get(0).getNetworkId();
        } finally {
            DatabaseUtils.readLock.unlock();
        }
    }

    @Override
    public boolean joinNetwork(JoinNetworkRequest request) {
        DatabaseUtils.writeLock.lock();
        try {
            final boolean[] added = {false};
            this.daoSession.runInTx(() -> {
                if (this.daoSession.getNetworkDao().load(request.getNetworkId()) != null) {
                    return;
                }
                NetworkConfig config = new NetworkConfig(
                        request.getNetworkId(),
                        request.isRouteViaZeroTier(),
                        request.getDnsMode()
                );
                this.daoSession.getNetworkConfigDao().insert(config);
                if (request.getDnsMode() == DNSMode.CUSTOM_DNS.toInt()) {
                    for (String server : request.getDnsServers()) {
                        this.daoSession.getDnsServerDao().insert(
                                new DnsServer(null, request.getNetworkId(), server)
                        );
                    }
                }
                this.daoSession.getNetworkDao().insert(new Network(
                        request.getNetworkId(),
                        request.getNetworkIdStr(),
                        null,
                        false,
                        false,
                        request.getNetworkId()
                ));
                added[0] = true;
            });
            return added[0];
        } finally {
            DatabaseUtils.writeLock.unlock();
        }
    }

    @Override
    public void setLastActivated(long networkId) {
        DatabaseUtils.writeLock.lock();
        try {
            this.daoSession.runInTx(() -> {
                List<Network> networks = this.daoSession.getNetworkDao().loadAll();
                for (Network network : networks) {
                    boolean active = network.getNetworkId() != null
                            && network.getNetworkId() == networkId;
                    if (network.getLastActivated() != active) {
                        network.setLastActivated(active);
                        this.daoSession.getNetworkDao().update(network);
                    }
                }
            });
        } finally {
            DatabaseUtils.writeLock.unlock();
        }
    }

    @Override
    public void updateNetworkName(long networkId, String networkName) {
        if (networkName == null || networkName.isEmpty()) {
            return;
        }
        DatabaseUtils.writeLock.lock();
        try {
            Network network = this.daoSession.getNetworkDao().load(networkId);
            if (network == null) {
                return;
            }
            network.setNetworkName(networkName);
            this.daoSession.getNetworkDao().update(network);
        } finally {
            DatabaseUtils.writeLock.unlock();
        }
    }

    @Override
    public void updateRouteViaZeroTier(long networkId, boolean routeViaZeroTier) {
        DatabaseUtils.writeLock.lock();
        try {
            Network network = this.daoSession.getNetworkDao().load(networkId);
            if (network == null) {
                return;
            }
            NetworkConfig config = this.daoSession.getNetworkConfigDao()
                    .load(network.getNetworkConfigId());
            if (config == null) {
                return;
            }
            config.setRouteViaZeroTier(routeViaZeroTier);
            this.daoSession.getNetworkConfigDao().update(config);
        } finally {
            DatabaseUtils.writeLock.unlock();
        }
    }

    @Override
    public void deleteNetwork(long networkId) {
        DatabaseUtils.writeLock.lock();
        try {
            this.daoSession.runInTx(() -> {
                Network network = this.daoSession.getNetworkDao().load(networkId);
                if (network == null) {
                    return;
                }
                List<AssignedAddress> addresses = this.daoSession.getAssignedAddressDao()
                        .queryBuilder()
                        .where(AssignedAddressDao.Properties.NetworkId.eq(networkId))
                        .list();
                for (AssignedAddress address : addresses) {
                    this.daoSession.getAssignedAddressDao().delete(address);
                }
                List<DnsServer> dnsServers = this.daoSession.getDnsServerDao()
                        .queryBuilder()
                        .where(DnsServerDao.Properties.NetworkId.eq(networkId))
                        .list();
                for (DnsServer server : dnsServers) {
                    this.daoSession.getDnsServerDao().delete(server);
                }
                NetworkConfig config = this.daoSession.getNetworkConfigDao()
                        .load(network.getNetworkConfigId());
                if (config != null) {
                    this.daoSession.getNetworkConfigDao().delete(config);
                }
                this.daoSession.getNetworkDao().delete(network);
            });
        } finally {
            DatabaseUtils.writeLock.unlock();
        }
    }

    /**
     * 将实体转换为列表用的概要 DTO。
     *
     * @param network 网络实体，不能为空
     * @return 网络概要
     */
    private NetworkSummary toSummary(Network network) {
        return new NetworkSummary(
                network.getNetworkId(),
                network.getNetworkIdStr(),
                network.getNetworkName(),
                network.getLastActivated()
        );
    }
}
