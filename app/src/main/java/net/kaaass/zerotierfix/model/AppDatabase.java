package net.kaaass.zerotierfix.model;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.List;

/**
 * Room 数据库。
 *
 * <p>数据库文件沿用 {@code ztfixdb}。不兼容 GreenDAO 时代的旧库：
 * 版本不匹配时会直接重建数据库。</p>
 */
@Database(
        entities = {
                AppNode.class,
                AssignedAddress.class,
                DnsServer.class,
                MoonOrbit.class,
                Network.class,
                NetworkConfig.class
        },
        version = 23,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DB_NAME = "ztfixdb";

    private static volatile AppDatabase instance;

    public abstract AppNodeDao appNodeDao();

    public abstract AssignedAddressDao assignedAddressDao();

    public abstract DnsServerDao dnsServerDao();

    public abstract MoonOrbitDao moonOrbitDao();

    public abstract NetworkDao networkDao();

    public abstract NetworkConfigDao networkConfigDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DB_NAME)
                            // 原有代码在 UI 线程同步访问数据库，这里保持相同行为
                            .allowMainThreadQueries()
                            // 旧 GreenDAO 数据库直接丢弃重建
                            .fallbackToDestructiveMigration(true)
                            .build();
                }
            }
        }
        return instance;
    }

    /**
     * 查询网络列表，并填充关联的 {@link NetworkConfig}。
     */
    public List<Network> loadNetworks() {
        List<Network> networks = networkDao().loadAllOrdered();
        attachConfigs(networks);
        return networks;
    }

    /**
     * 查询最近一次激活的网络，并填充关联的 {@link NetworkConfig}。
     */
    public List<Network> loadLastActivatedNetworks() {
        List<Network> networks = networkDao().findLastActivated();
        attachConfigs(networks);
        return networks;
    }

    /**
     * 按 ID 查询网络，并填充关联的 {@link NetworkConfig}。
     */
    public Network loadNetwork(long networkId) {
        Network network = networkDao().findById(networkId);
        if (network != null) {
            network.setNetworkConfig(networkConfigDao().findById(network.getNetworkConfigId()));
        }
        return network;
    }

    private void attachConfigs(List<Network> networks) {
        for (Network network : networks) {
            network.setNetworkConfig(networkConfigDao().findById(network.getNetworkConfigId()));
        }
    }
}
