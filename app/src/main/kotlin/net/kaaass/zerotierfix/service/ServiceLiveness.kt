package net.kaaass.zerotierfix.service

import android.app.ActivityManager
import android.content.Context

/**
 * 查询本应用服务是否仍在运行。
 *
 * Android O 起 [ActivityManager.getRunningServices] 只返回调用方自身的服务，
 * 无需权限，且不会像 `bindService` 那样创建服务，适合做停止后的真实性校验。
 */
object ServiceLiveness {

    /**
     * 判断指定服务是否处于运行状态。
     *
     * @param context 任意 Context
     * @param serviceClass 服务类型
     * @return 服务仍在运行时返回 `true`
     */
    fun isRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return false
        @Suppress("DEPRECATION")
        val services = manager.getRunningServices(Int.MAX_VALUE) ?: return false
        return services.any {
            it.service.packageName == context.packageName &&
                it.service.className == serviceClass.name
        }
    }
}
