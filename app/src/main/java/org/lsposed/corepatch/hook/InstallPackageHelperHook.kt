package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import android.os.Build
import org.lsposed.corepatch.Config
import org.lsposed.corepatch.XposedHelper.hookAfter
import org.lsposed.corepatch.XposedHelper.hostClassLoader

object InstallPackageHelperHook : BaseHook() {
    override val name = "InstallPackageHelperHook"

    @SuppressLint("PrivateApi")
    override fun hook() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val installPackageHelperClazz =
            hostClassLoader.loadClass("com.android.server.pm.InstallPackageHelper")
        val doesSignatureMatchForPermissionsMethod =
            installPackageHelperClazz.declaredMethods.first { m -> m.name == "doesSignatureMatchForPermissions" }
        hookAfter(doesSignatureMatchForPermissionsMethod) { callback ->
            if (Config.isBypassDigestEnabled() && Config.isUsePreviousSignaturesEnabled()) {
                // If we decide to crack this then at least make sure they are same apks, avoid another one that tries to impersonate.
                if (callback.result == false) {
                    val getPackageNameMethod =
                        callback.args[1]!!.javaClass.declaredMethods.first { m -> m.name == "getPackageName" }
                    val packageName = getPackageNameMethod.invoke(callback.args[1]) as String
                    if (packageName == callback.args[0] as String) {
                        callback.result = true
                    }
                }
            }
        }
    }
}
