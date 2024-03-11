package org.lsposed.corepatch.hook

import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import org.lsposed.corepatch.Config
import org.lsposed.corepatch.XposedHelper
import org.lsposed.corepatch.XposedHelper.BeforeCallback
import org.lsposed.corepatch.XposedHelper.hostClassLoader

object MessageDigestHook : BaseHook() {
    override val name = "MessageDigestHook"

    override fun hook() {
        val messageDigestClazz = hostClassLoader.loadClass("java.security.MessageDigest")
        // https://cs.android.com/android/platform/superproject/main/+/main:libcore/ojluni/src/main/java/java/security/MessageDigest.java;l=518
        // public static boolean isEqual(byte[] digesta, byte[] digestb)
        val isEqualMethod = messageDigestClazz.getDeclaredMethod(
            "isEqual", ByteArray::class.java, ByteArray::class.java
        )
        XposedHelper.hookBefore(isEqualMethod, object : BeforeCallback {
            override fun before(callback: BeforeHookCallback) {
                if (Config.isBypassVerificationEnabled()) {
                    callback.returnAndSkip(true)
                }
            }
        })
    }
}