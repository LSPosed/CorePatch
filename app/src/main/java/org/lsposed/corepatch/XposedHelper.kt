package org.lsposed.corepatch

import android.util.Log
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.annotations.AfterInvocation
import io.github.libxposed.api.annotations.BeforeInvocation
import io.github.libxposed.api.annotations.XposedHooker
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

object XposedHelper {
    lateinit var xposedModule: XposedModule
        private set
    lateinit var hostClassLoader: ClassLoader
        private set
    var moduleClassLoader: ClassLoader = XposedHelper::class.java.classLoader!!
    val prefs by lazy { xposedModule.getRemotePreferences("conf") }

    fun setXposedModule(module: XposedModule) {
        xposedModule = module
    }

    fun setHostClassLoader(classLoader: ClassLoader) {
        hostClassLoader = classLoader
    }

    interface BeforeCallback {
        fun before(callback: XposedInterface.BeforeHookCallback)
    }

    interface AfterCallback {
        fun after(callback: XposedInterface.AfterHookCallback)
    }

    @XposedHooker
    internal object CustomHooker : XposedInterface.Hooker {
        var beforeCallbacks: ConcurrentHashMap<Member, BeforeCallback> = ConcurrentHashMap()
        var afterCallbacks: ConcurrentHashMap<Member, AfterCallback> = ConcurrentHashMap()

        @BeforeInvocation
        @JvmStatic
        fun before(callback: XposedInterface.BeforeHookCallback) {
            beforeCallbacks[callback.member]?.before(callback)
        }

        @AfterInvocation
        @JvmStatic
        fun after(callback: XposedInterface.AfterHookCallback) {
            afterCallbacks[callback.member]?.after(callback)
        }
    }

    fun hookBefore(
        member: Member, callback: BeforeCallback
    ): XposedInterface.MethodUnhooker<out Executable> {
        CustomHooker.beforeCallbacks[member] = callback
        return when (member.javaClass) {
            Method::class.java -> xposedModule.hook(member as Method, CustomHooker::class.java)
            Constructor::class.java -> xposedModule.hook(
                member as Constructor<*>, CustomHooker::class.java
            )

            else -> throw IllegalArgumentException()
        }
    }

    fun hookAfter(
        member: Member, callback: AfterCallback
    ): XposedInterface.MethodUnhooker<out Executable> {
        CustomHooker.afterCallbacks[member] = callback
        return when (member.javaClass) {
            Method::class.java -> xposedModule.hook(member as Method, CustomHooker::class.java)
            Constructor::class.java -> xposedModule.hook(
                member as Constructor<*>, CustomHooker::class.java
            )

            else -> throw IllegalArgumentException()
        }
    }

    fun log(message: String, throwable: Throwable? = null) {
        val newMessage = "[CorePatch] $message"
        throwable?.let { xposedModule.log(newMessage, it) }
            ?: if (BuildConfig.DEBUG) return else Log.d("CorePatch", message)
    }

    fun deoptimize(method: Method): Boolean {
        return xposedModule.deoptimize(method)
    }
}