package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import org.lsposed.corepatch.Config
import org.lsposed.corepatch.XposedHelper.deoptimize
import org.lsposed.corepatch.XposedHelper.hookBefore
import org.lsposed.corepatch.XposedHelper.hostClassLoader
import org.lsposed.corepatch.XposedHelper.log

object PackageInstallerSessionHook : BaseHook() {
    override val name = "PackageInstallerSessionHook"

    @SuppressLint("PrivateApi")
    override fun hook() {
        val packageInstallerSessionClazz =
            hostClassLoader.loadClass("com.android.server.pm.PackageInstallerSession")
        // https://android.googlesource.com/platform/frameworks/base/+/android16-qpr2-release/services/core/java/com/android/server/pm/PackageInstallerSession.java#3051
        val shouldUseVerificationServiceMethod =
            packageInstallerSessionClazz.declaredMethods.firstOrNull { method ->
                method.name == "shouldUseVerificationService" &&
                    method.parameterCount == 0 && method.returnType == Boolean::class.java
            } ?: return

        // Deoptimize caller so inlined checks still reach hook
        val startDeveloperVerificationSessionMethod =
            packageInstallerSessionClazz.declaredMethods.first { method ->
                method.name == "startDeveloperVerificationSession"
            }
        if (!deoptimize(startDeveloperVerificationSessionMethod)) {
            log("failed to deoptimize startDeveloperVerificationSession")
        }

        // Let the system complete the skipped verification and continue installation
        hookBefore(shouldUseVerificationServiceMethod) { callback ->
            if (Config.isDisableDeveloperVerificationEnabled()) {
                callback.returnAndSkip(false)
            }
        }
    }
}
