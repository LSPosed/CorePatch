package org.lsposed.corepatch.hook

import org.lsposed.corepatch.XposedHelper.hookBefore
import org.lsposed.corepatch.XposedHelper.hostClassLoader

object NtConfigListServiceImplHook : BaseHook() {
    override val name = "NtConfigListServiceImplHook"

    override fun hook() {
        // TODO: Check is Nothing Phone
        val ntConfigListServiceImplClazz = try {
            hostClassLoader.loadClass("com.nothing.server.ex.NtConfigListServiceImpl")
        } catch (e: ClassNotFoundException) {
            return
        }
        val isInstallingAppForbiddenMethod =
            ntConfigListServiceImplClazz.declaredMethods.first { m -> m.name == "isInstallingAppForbidden" }
        hookBefore(isInstallingAppForbiddenMethod) { callback ->
            // TODO: switch
            callback.returnAndSkip(false)
        }
        val isStartingAppForbiddenMethod =
            ntConfigListServiceImplClazz.declaredMethods.first { m -> m.name == "isStartingAppForbidden" }
        hookBefore(isStartingAppForbiddenMethod) { callback ->
            // TODO: switch
            callback.returnAndSkip(false)
        }
    }
}
