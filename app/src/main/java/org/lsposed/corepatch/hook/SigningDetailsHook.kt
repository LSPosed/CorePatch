package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import android.os.Build
import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import org.lsposed.corepatch.Config
import org.lsposed.corepatch.XposedHelper.BeforeCallback
import org.lsposed.corepatch.XposedHelper.hookBefore
import org.lsposed.corepatch.XposedHelper.hostClassLoader
import java.util.Arrays


object SigningDetailsHook : BaseHook() {
    override val name = "SigningDetailsHook"

    @SuppressLint("PrivateApi")
    override fun hook() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val signingDetailsClazz =
            hostClassLoader.loadClass("android.content.pm.PackageParser\$SigningDetails")

        // https://cs.android.com/android/platform/superproject/+/android-9.0.0_r61:frameworks/base/core/java/android/content/pm/PackageParser.java;l=5851
        // public boolean checkCapability(SigningDetails oldDetails, @CertCapabilities int flags)

        val checkCapabilityMethod = signingDetailsClazz.getDeclaredMethod(
            "checkCapability", signingDetailsClazz, Int::class.java
        )
        hookBefore(checkCapabilityMethod, object : BeforeCallback {
            override fun before(callback: BeforeHookCallback) {
                if (Config.isBypassDigestEnabled() && Config.isBypassVerificationEnabled()) {
                    if (callback.args[1] != 4 && callback.args[1] != 16) {
                        callback.returnAndSkip(true)
                    }
                }
            }
        })

        // https://cs.android.com/android/platform/superproject/+/android-9.0.0_r61:frameworks/base/core/java/android/content/pm/PackageParser.java;l=5962
        // public boolean checkCapabilityRecover(SigningDetails oldDetails, @CertCapabilities int flags)
        // New package has a different signature
        // 处理覆盖安装但签名不一致
        val checkCapabilityRecoverMethod = signingDetailsClazz.getDeclaredMethod(
            "checkCapabilityRecover", signingDetailsClazz, Int::class.java
        )
        hookBefore(checkCapabilityRecoverMethod, object : BeforeCallback {
            override fun before(callback: BeforeHookCallback) {
                if (Config.isBypassDigestEnabled() && Config.isBypassVerificationEnabled()) {
                    // Don't handle PERMISSION (grant SIGNATURE permissions to pkgs with this cert)
                    // Or applications will have all privileged permissions
                    // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/content/pm/PackageParser.java;l=5947
                    if (callback.args[1] != 4) {
                        callback.returnAndSkip(true)
                    }
                }
            }
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // for SharedUser
            // "Package " + packageName + " has a signing lineage " + "that diverges from the lineage of the sharedUserId"
            // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r1:frameworks/base/services/core/java/com/android/server/pm/PackageManagerServiceUtils.java;l=725
            val hasCommonAncestorMethod = signingDetailsClazz.getDeclaredMethod(
                "hasCommonAncestor", signingDetailsClazz
            )
            hookBefore(hasCommonAncestorMethod, object : BeforeCallback {
                override fun before(callback: BeforeHookCallback) {
                    if (Config.isBypassDigestEnabled() && Config.isBypassSharedUserEnabled()
                        // because of LSPosed's bug, we can't hook verifySignatures while deoptimize it
                        && Arrays.stream(
                            Thread.currentThread().stackTrace
                        ).anyMatch { o: StackTraceElement -> "verifySignatures" == o.methodName }
                    ) {
                        callback.returnAndSkip(true)
                    }
                }
            })
        }
    }
}