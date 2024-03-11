package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import android.os.Build
import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import org.lsposed.corepatch.Config
import org.lsposed.corepatch.XposedHelper
import org.lsposed.corepatch.XposedHelper.BeforeCallback
import org.lsposed.corepatch.XposedHelper.hookBefore
import org.lsposed.corepatch.XposedHelper.hostClassLoader

object PackageManagerServiceUtilsHook : BaseHook() {
    override val name = "PackageManagerServiceUtilsHook"

    @SuppressLint("PrivateApi")
    override fun hook() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return

        val packageManagerServiceUtilsClazz =
            hostClassLoader.loadClass("com.android.server.pm.PackageManagerServiceUtils")

        // https://cs.android.com/android/platform/superproject/+/android-9.0.0_r61:frameworks/base/services/core/java/com/android/server/pm/PackageManagerServiceUtils.java;l=552
        // public static boolean verifySignatures(
        //     PackageSetting pkgSetting,
        //     PackageSetting disabledPkgSetting,
        //     PackageParser.SigningDetails parsedSignatures,
        //     boolean compareCompat,
        //     boolean compareRecover)
        // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r34:frameworks/base/services/core/java/com/android/server/pm/PackageManagerServiceUtils.java;l=625
        // public static boolean verifySignatures(
        //     PackageSetting pkgSetting,
        //     PackageSetting disabledPkgSetting,
        //     PackageParser.SigningDetails parsedSignatures,
        //     boolean compareCompat,
        //     boolean compareRecover,
        //     boolean isRollback)
        val verifySignaturesMethod =
            packageManagerServiceUtilsClazz.declaredMethods.first { m -> m.name == "verifySignatures" && m.returnType == Boolean::class.java }
        XposedHelper.deoptimize(verifySignaturesMethod)
        hookBefore(verifySignaturesMethod, object : BeforeCallback {
            override fun before(callback: BeforeHookCallback) {
                if (Config.isBypassVerificationEnabled()) {
                    callback.returnAndSkip(true)
                }
            }
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // https://cs.android.com/android/platform/superproject/+/android-13.0.0_r1:frameworks/base/services/core/java/com/android/server/pm/PackageManagerServiceUtils.java;l=1375
            // public static void checkDowngrade(com.android.server.pm.parsing.pkg.AndroidPackage before, PackageInfoLite after)
            // https://cs.android.com/android/platform/superproject/+/android-14.0.0_r1:frameworks/base/services/core/java/com/android/server/pm/PackageManagerServiceUtils.java;l=1499
            // public static void checkDowngrade(com.android.server.pm.pkg.AndroidPackage before, PackageInfoLite after)
            val checkDowngradeMethod =
                packageManagerServiceUtilsClazz.declaredMethods.first { m -> m.name == "checkDowngrade" }
            hookBefore(checkDowngradeMethod, object : BeforeCallback {
                override fun before(callback: BeforeHookCallback) {
                    if (Config.isBypassDowngradeEnabled()) {
                        callback.returnAndSkip(null)
                    }
                }
            })
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // ensure verifySignatures success
            // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/services/core/java/com/android/server/pm/PackageManagerServiceUtils.java;l=621
            val canJoinSharedUserIdMethod =
                packageManagerServiceUtilsClazz.declaredMethods.first { m -> m.name == "canJoinSharedUserId" }
            val success = XposedHelper.deoptimize(canJoinSharedUserIdMethod)
            if (success) {
            } else {
            }
        }
    }
}