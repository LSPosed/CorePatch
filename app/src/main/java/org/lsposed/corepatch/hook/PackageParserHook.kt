package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import android.os.Build
import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import org.lsposed.corepatch.Config
import org.lsposed.corepatch.XposedHelper.BeforeCallback
import org.lsposed.corepatch.XposedHelper.hookBefore
import org.lsposed.corepatch.XposedHelper.hostClassLoader

object PackageParserHook : BaseHook() {
    override val name = "PackageParserHook"

    @SuppressLint("PrivateApi")
    override fun hook() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) return

        val packageParserClazz = hostClassLoader.loadClass("android.content.pm.PackageParser")
        // https://cs.android.com/android/platform/superproject/+/android-7.0.0_r1:frameworks/base/core/java/android/content/pm/PackageParser.java;l=1064
        // public static int getApkSigningVersion(Package pkg)
        val getApkSigningVersionMethod =
            packageParserClazz.declaredMethods.first { m -> m.name == "getApkSigningVersion" && m.returnType == Int::class.java }
        hookBefore(getApkSigningVersionMethod, object : BeforeCallback {
            override fun before(callback: BeforeHookCallback) {
                if (Config.isEnhancedModeEnabled()) {
                    callback.returnAndSkip(1)
                }
            }
        })
    }
}