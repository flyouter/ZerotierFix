package net.kaaass.zerotierfix.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.util.Log

import net.kaaass.zerotierfix.util.Constants

/**
 * 开机广播接收器。
 */
class StartupReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "Received: " + intent.action + ". Starting ZeroTier One service.")
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        if (pref.getBoolean(Constants.PREF_GENERAL_START_ZEROTIER_ON_BOOT, true)) {
            Log.i(TAG, "Preferences set to start ZeroTier on boot")
        } else {
            Log.i(TAG, "Preferences set to not start ZeroTier on boot")
        }
    }

    companion object {
        private const val TAG = "StartupReceiver"
    }
}
