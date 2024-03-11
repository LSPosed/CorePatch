package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import android.os.Build
import io.github.libxposed.api.XposedInterface.AfterHookCallback
import org.lsposed.corepatch.Config
import org.lsposed.corepatch.XposedHelper.AfterCallback
import org.lsposed.corepatch.XposedHelper.hookAfter
import org.lsposed.corepatch.XposedHelper.hostClassLoader
import java.util.Arrays

object KeySetManagerServiceHook : BaseHook() {
    override val name = "KeySetManagerServiceHook"

    @SuppressLint("PrivateApi")
    override fun hook() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return

        val keySetManagerServiceClazz =
            hostClassLoader.loadClass("com.android.server.pm.KeySetManagerService")

        val shouldBypass = ThreadLocal<Boolean>()

        // https://cs.android.com/android/platform/superproject/+/android-9.0.0_r61:frameworks/base/services/core/java/com/android/server/pm/KeySetManagerService.java;l=346
        // public boolean shouldCheckUpgradeKeySetLocked(PackageSettingBase oldPs, int scanFlags)
        val shouldCheckUpgradeKeySetLockedMethod =
            keySetManagerServiceClazz.declaredMethods.first { m ->
                m.name == "shouldCheckUpgradeKeySetLocked" && m.returnType == Boolean::class.java
            }
        hookAfter(shouldCheckUpgradeKeySetLockedMethod, object : AfterCallback {
            override fun after(callback: AfterHookCallback) {
                if (Config.isBypassDigestEnabled() && Arrays.stream(
                        Thread.currentThread().stackTrace
                    )
                        // https://cs.android.com/android/platform/superproject/+/android-9.0.0_r61:frameworks/base/services/core/java/com/android/server/pm/PackageManagerService.java;l=17068
                        // private void installPackageLI(InstallArgs args, PackageInstalledInfo res)
                        // https://cs.android.com/android/platform/superproject/+/android-10.0.0_r47:frameworks/base/services/core/java/com/android/server/pm/PackageManagerService.java;l=17246
                        // https://cs.android.com/android/platform/superproject/+/android-13.0.0_r74:frameworks/base/services/core/java/com/android/server/pm/InstallPackageHelper.java;l=1074
                        // private PrepareResult preparePackageLI(InstallArgs args, PackageInstalledInfo res)
                        .anyMatch { o: StackTraceElement -> ( /* API 29 */"preparePackageLI" == o.methodName || /* API 28 */ "installPackageLI" == o.methodName) }
                ) {
                    shouldBypass.set(true)
                    callback.result = true
                } else {
                    shouldBypass.set(false)
                }
            }
        })

        // https://cs.android.com/android/platform/superproject/+/android-9.0.0_r61:frameworks/base/services/core/java/com/android/server/pm/KeySetManagerService.java;l=367
        // public boolean checkUpgradeKeySetLocked(PackageSettingBase oldPS, PackageParser.Package newPkg)
        val checkUpgradeKeySetLockedMethod = keySetManagerServiceClazz.declaredMethods.first { m ->
            m.name == "checkUpgradeKeySetLocked" && m.returnType == Boolean::class.java
        }
        hookAfter(checkUpgradeKeySetLockedMethod, object : AfterCallback {
            override fun after(callback: AfterHookCallback) {
                if (Config.isBypassDigestEnabled() && shouldBypass.get() == true) {
                    callback.result = true
                }
            }
        })
    }
}