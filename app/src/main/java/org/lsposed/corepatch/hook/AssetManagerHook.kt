package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import android.os.Build
import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import org.lsposed.corepatch.Config
import org.lsposed.corepatch.XposedHelper.BeforeCallback
import org.lsposed.corepatch.XposedHelper.hookBefore
import org.lsposed.corepatch.XposedHelper.hostClassLoader

object AssetManagerHook : BaseHook() {
    override val name = "AssetManagerHook"

    @SuppressLint("BlockedPrivateApi")
    override fun hook() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return

        val assetManagerClazz = hostClassLoader.loadClass("android.content.res.AssetManager")

        // Targeting R+ (version " + Build.VERSION_CODES.R + " and above) requires"
        // + " the resources.arsc of installed APKs to be stored uncompressed"
        // + " and aligned on a 4-byte boundary
        // target >=30 的情况下 resources.arsc 必须是未压缩的且4K对齐
        // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r48:frameworks/base/core/java/android/content/res/AssetManager.java;l=828
        // public boolean containsAllocatedTable()
        val containsAllocatedTableMethod =
            assetManagerClazz.getDeclaredMethod("containsAllocatedTable")
        hookBefore(containsAllocatedTableMethod, object : BeforeCallback {
            override fun before(callback: BeforeHookCallback) {
                if (Config.isBypassVerificationEnabled()) {
                    callback.returnAndSkip(false)
                }
            }
        })
    }
}