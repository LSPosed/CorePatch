package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import io.github.libxposed.api.XposedInterface.AfterHookCallback
import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import org.lsposed.corepatch.Config
import org.lsposed.corepatch.XposedHelper.AfterCallback
import org.lsposed.corepatch.XposedHelper.BeforeCallback
import org.lsposed.corepatch.XposedHelper.hookAfter
import org.lsposed.corepatch.XposedHelper.hookBefore
import org.lsposed.corepatch.XposedHelper.hostClassLoader

object StrictJarVerifierHook : BaseHook() {
    override val name = "StrictJarVerifierHook"

    @SuppressLint("PrivateApi")
    override fun hook() {
        val strictJarVerifierClazz = hostClassLoader.loadClass("android.util.jar.StrictJarVerifier")

        // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/java/android/util/jar/StrictJarVerifier.java;l=529
        // private static boolean verifyMessageDigest(byte[] expected, byte[] encodedActual)
        val verifyMessageDigestMethod =
            strictJarVerifierClazz.declaredMethods.first { m -> m.name == "verifyMessageDigest" && m.returnType == Boolean::class.java }
        hookBefore(verifyMessageDigestMethod, object : BeforeCallback {
            override fun before(callback: BeforeHookCallback) {
                if (Config.isBypassVerificationEnabled()) {
                    callback.returnAndSkip(true)
                }
            }
        })

        // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/java/android/util/jar/StrictJarVerifier.java;l=502
        // private boolean verify(
        //     Attributes attributes,
        //     String entry,
        //     byte[] data,
        //     int start,
        //     int end,
        //     boolean ignoreSecondEndline,
        //     boolean ignorable)
        val verifyMethod =
            strictJarVerifierClazz.declaredMethods.first { m -> m.name == "verify" && m.returnType == Boolean::class.java }
        hookBefore(verifyMethod, object : BeforeCallback {
            override fun before(callback: BeforeHookCallback) {
                if (Config.isBypassVerificationEnabled()) {
                    callback.returnAndSkip(true)
                }
            }
        })

        val strictJarVerifierConstructor = strictJarVerifierClazz.declaredConstructors.first()
        hookBefore(strictJarVerifierConstructor, object : BeforeCallback {
            override fun before(callback: BeforeHookCallback) {
                if (Config.isEnhancedModeEnabled()) {
                    callback.args[3] = false
                }
            }
        })

        val signatureSchemeRollbackProtectionsEnforcedField =
            strictJarVerifierClazz.declaredFields.first { f -> f.name == "signatureSchemeRollbackProtectionsEnforced" }
        signatureSchemeRollbackProtectionsEnforcedField.isAccessible = true
        hookAfter(strictJarVerifierConstructor, object : AfterCallback {
            override fun after(callback: AfterHookCallback) {
                if (Config.isBypassVerificationEnabled()) {
                    signatureSchemeRollbackProtectionsEnforcedField.set(
                        callback.thisObject, false
                    )
                }
            }
        })
    }
}