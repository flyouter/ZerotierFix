package net.kaaass.zerotierfix.di;

import android.content.Context;

import net.kaaass.zerotierfix.data.database.ZTOpenHelper;
import net.kaaass.zerotierfix.data.database.entity.DaoMaster;
import net.kaaass.zerotierfix.data.database.entity.DaoSession;
import net.kaaass.zerotierfix.data.repository.AppNodeRepository;
import net.kaaass.zerotierfix.data.repository.AppNodeRepositoryImpl;
import net.kaaass.zerotierfix.data.repository.MoonOrbitRepository;
import net.kaaass.zerotierfix.data.repository.MoonOrbitRepositoryImpl;
import net.kaaass.zerotierfix.data.repository.NetworkRepository;
import net.kaaass.zerotierfix.data.repository.NetworkRepositoryImpl;

/**
 * 应用级依赖容器（手动依赖注入）。
 *
 * <p>集中创建数据库会话与 Repository 实例，避免各页面和服务重复初始化。
 * 生命周期与 {@link android.app.Application} 一致。</p>
 */
public final class AppContainer {

    /** 应用级数据库会话。 */
    private final DaoSession daoSession;

    /** 网络数据仓库。 */
    private final NetworkRepository networkRepository;

    /** Moon 入轨仓库。 */
    private final MoonOrbitRepository moonOrbitRepository;

    /** 本机节点信息仓库。 */
    private final AppNodeRepository appNodeRepository;

    /**
     * 创建依赖容器并初始化数据库与仓库。
     *
     * @param context 应用上下文，不能为空
     */
    public AppContainer(Context context) {
        this.daoSession = new DaoMaster(
                new ZTOpenHelper(context, "ztfixdb", null).getWritableDatabase()
        ).newSession();
        this.networkRepository = new NetworkRepositoryImpl(this.daoSession);
        this.moonOrbitRepository = new MoonOrbitRepositoryImpl(
                this.daoSession, context.getApplicationContext());
        this.appNodeRepository = new AppNodeRepositoryImpl(this.daoSession);
    }

    /**
     * 获取网络数据仓库。
     *
     * @return 网络仓库，非空
     */
    public NetworkRepository getNetworkRepository() {
        return networkRepository;
    }

    /**
     * 获取 Moon 入轨仓库。
     *
     * @return Moon 仓库，非空
     */
    public MoonOrbitRepository getMoonOrbitRepository() {
        return moonOrbitRepository;
    }

    /**
     * 获取本机节点信息仓库。
     *
     * @return 本机节点仓库，非空
     */
    public AppNodeRepository getAppNodeRepository() {
        return appNodeRepository;
    }

}
