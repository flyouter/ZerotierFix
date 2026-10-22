package net.kaaass.zerotierfix;

import android.app.Application;
import android.util.Log;

import net.kaaass.zerotierfix.model.AppDatabase;

/**
 * 主程序入口
 *
 * @author kaaass
 */
public class ZerotierFixApplication extends Application {
    private AppDatabase mDatabase;

    public void onCreate() {
        super.onCreate();
        Log.i("Application", "Starting Application");
        this.mDatabase = AppDatabase.getInstance(this);
    }

    public AppDatabase getDatabase() {
        return this.mDatabase;
    }
}
