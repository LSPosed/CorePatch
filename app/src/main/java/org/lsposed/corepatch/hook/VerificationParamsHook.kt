package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import android.os.Build
import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import org.lsposed.corepatch.Config
import org.lsposed.corepatch.XposedHelper.BeforeCallback
import org.lsposed.corepatch.XposedHelper.hookBefore
import org.lsposed.corepatch.XposedHelper.hostClassLoader

object VerificationParamsHook: BaseHook() {
    override val name = "VerificationParamsHook"

    @SuppressLint("PrivateApi")
    override fun hook() {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val verificationParamsClazz = hostClassLoader.loadClass("com.android.server.pm.VerificationParams")

        val isVerificationEnabledMethod = verificationParamsClazz.declaredMethods.first { m -> m.name == "isVerificationEnabled" }
        hookBefore(isVerificationEnabledMethod, object : BeforeCallback {
            override fun before(callback: BeforeHookCallback) {
                if (Config.isDisableVerificationAgentEnabled()) {
                    callback.returnAndSkip(false)
                }
            }
        })
    }
}