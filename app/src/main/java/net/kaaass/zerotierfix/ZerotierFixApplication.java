package net.kaaass.zerotierfix;

import android.app.Application;
import android.util.Log;

import net.kaaass.zerotierfix.di.AppContainer;

/**
 * 主程序入口。
 *
 * <p>持有应用级依赖容器；Activity、Service 通过 {@link #getAppContainer()} 获取
 * Repository 等数据访问能力。</p>
 *
 * @author kaaass
 */
public class ZerotierFixApplication extends Application {

    /** 应用级依赖容器，在 {@link #onCreate()} 中初始化。 */
    private AppContainer appContainer;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("Application", "Starting Application");
        this.appContainer = new AppContainer(this);
    }

    /**
     * 获取应用级依赖容器。
     *
     * @return 依赖容器，非空
     */
    public AppContainer getAppContainer() {
        return this.appContainer;
    }
}
