package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import android.os.Build
import org.lsposed.corepatch.XposedHelper
import org.lsposed.corepatch.XposedHelper.hostClassLoader

object ReconcilePackageUtilsHook : BaseHook() {
    override val name = "ReconcilePackageUtilsHook"

    @SuppressLint("PrivateApi")
    override fun hook() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val reconcilePackageUtilsClazz =
            hostClassLoader.loadClass("com.android.server.pm.ReconcilePackageUtils")
        val reconcilePackagesMethod =
            reconcilePackageUtilsClazz.declaredMethods.first { m -> m.name == "reconcilePackages" }
        val success = XposedHelper.deoptimize(reconcilePackagesMethod)
        if (success) {
        } else {
        }
    }
}