package net.kaaass.zerotierfix.data.repository;

import android.util.Log;

import net.kaaass.zerotierfix.data.database.AppDatabase;
import net.kaaass.zerotierfix.data.database.DatabaseUtils;
import net.kaaass.zerotierfix.data.database.entity.DnsServer;
import net.kaaass.zerotierfix.data.database.entity.Network;
import net.kaaass.zerotierfix.data.database.entity.NetworkConfig;
import net.kaaass.zerotierfix.model.dto.JoinNetworkRequest;
import net.kaaass.zerotierfix.model.dto.NetworkConfigDto;
import net.kaaass.zerotierfix.model.dto.NetworkSummary;
import net.kaaass.zerotierfix.model.enums.DNSMode;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link NetworkRepository} 的 Room 实现。
 */
public final class NetworkRepositoryImpl implements NetworkRepository {

    private static final String TAG = "NetworkRepository";

    private final AppDatabase database;

    /**
     * 创建网络仓库。
     *
     * @param database 应用级 Room 数据库，不能为空
     */
    public NetworkRepositoryImpl(AppDatabase database) {
        this.database = database;
    }

    @Override
    public List<NetworkSummary> getAllNetworks() {
        DatabaseUtils.readLock.lock();
        try {
            List<Network> networks = this.database.loadNetworks();
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
            Network network = this.database.loadNetwork(networkId);
            return network == null ? null : toSummary(network);
        } finally {
            DatabaseUtils.readLock.unlock();
        }
    }

    @Override
    public NetworkConfigDto getNetworkConfig(long networkId) {
        DatabaseUtils.readLock.lock();
        try {
            Network network = this.database.loadNetwork(networkId);
            if (network == null || network.getNetworkConfig() == null) {
                return null;
            }
            NetworkConfig config = network.getNetworkConfig();
            List<String> dnsServers = new ArrayList<>();
            for (DnsServer server : this.database.dnsServerDao().findByNetworkId(networkId)) {
                dnsServers.add(server.getNameserver());
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
            List<Network> networks = this.database.loadLastActivatedNetworks();
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
            this.database.runInTransaction(() -> {
                if (this.database.networkDao().countById(request.getNetworkId()) > 0) {
                    return;
                }
                NetworkConfig config = new NetworkConfig(
                        request.getNetworkId(),
                        request.isRouteViaZeroTier(),
                        request.getDnsMode()
                );
                this.database.networkConfigDao().insert(config);
                if (request.getDnsMode() == DNSMode.CUSTOM_DNS.toInt()) {
                    for (String server : request.getDnsServers()) {
                        this.database.dnsServerDao().insert(
                                new DnsServer(null, request.getNetworkId(), server)
                        );
                    }
                }
                this.database.networkDao().insert(new Network(
                        request.getNetworkId(),
                        request.getNetworkIdStr(),
                        null,
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
            this.database.runInTransaction(() -> {
                List<Network> networks = this.database.networkDao().loadAllOrdered();
                for (Network network : networks) {
                    boolean active = network.getNetworkId() != null
                            && network.getNetworkId() == networkId;
                    if (network.getLastActivated() != active) {
                        network.setLastActivated(active);
                        this.database.networkDao().update(network);
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
            Network network = this.database.networkDao().findById(networkId);
            if (network == null) {
                return;
            }
            network.setNetworkName(networkName);
            this.database.networkDao().update(network);
        } finally {
            DatabaseUtils.writeLock.unlock();
        }
    }

    @Override
    public void updateRouteViaZeroTier(long networkId, boolean routeViaZeroTier) {
        DatabaseUtils.writeLock.lock();
        try {
            Network network = this.database.loadNetwork(networkId);
            if (network == null || network.getNetworkConfig() == null) {
                return;
            }
            NetworkConfig config = network.getNetworkConfig();
            config.setRouteViaZeroTier(routeViaZeroTier);
            this.database.networkConfigDao().update(config);
        } finally {
            DatabaseUtils.writeLock.unlock();
        }
    }

    @Override
    public void deleteNetwork(long networkId) {
        DatabaseUtils.writeLock.lock();
        try {
            this.database.runInTransaction(() -> {
                Network network = this.database.networkDao().findById(networkId);
                if (network == null) {
                    return;
                }
                this.database.dnsServerDao().deleteByNetworkId(networkId);
                this.database.networkConfigDao().deleteById(network.getNetworkConfigId());
                this.database.networkDao().delete(network);
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
