package org.lsposed.corepatch

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import io.github.libxposed.service.XposedServiceHelper.OnServiceListener

class App : Application(), OnServiceListener {

    companion object {
        var rwPrefs: SharedPreferences? = null
        var mService: XposedService? = null
        var reloadListener: () -> Unit = {}
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        XposedServiceHelper.registerListener(this)
    }

    override fun onServiceBind(service: XposedService) {
        synchronized(this) {
            mService = service
            rwPrefs = service.getRemotePreferences("conf")
            reloadListener()
        }
    }

    override fun onServiceDied(service: XposedService) {
        synchronized(this) {
            mService = null
            rwPrefs = null
        }
    }
}