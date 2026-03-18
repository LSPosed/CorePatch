package org.lsposed.corepatch

import android.util.Log
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
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

    class BeforeHookCallback(private val chain: XposedInterface.Chain) {
        val member: Member get() = chain.executable as Member
        val thisObject: Any? get() = chain.thisObject
        val args: Array<Any?> get() = chain.args.toTypedArray()
        private var skipped = false
        private var skipResult: Any? = null

        fun returnAndSkip(result: Any?) {
            skipped = true
            skipResult = result
        }

        fun isSkipped() = skipped
        fun getSkipResult() = skipResult
    }

    class AfterHookCallback(
        private val chain: XposedInterface.Chain,
        var result: Any?,
        var throwable: Throwable?
    ) {
        val member: Member get() = chain.executable as Member
        val thisObject: Any? get() = chain.thisObject
        val args: Array<Any?> get() = chain.args.toTypedArray()
    }

    interface BeforeCallback {
        fun before(callback: BeforeHookCallback)
    }

    interface AfterCallback {
        fun after(callback: AfterHookCallback)
    }

    internal object CustomHooker : XposedInterface.Hooker {
        var beforeCallbacks: ConcurrentHashMap<Member, BeforeCallback> = ConcurrentHashMap()
        var afterCallbacks: ConcurrentHashMap<Member, AfterCallback> = ConcurrentHashMap()

        override fun intercept(chain: XposedInterface.Chain): Any? {
            val beforeCallback = beforeCallbacks[chain.executable]
            var result: Any? = null
            var throwable: Throwable? = null
            var skipped = false

            if (beforeCallback != null) {
                val callback = BeforeHookCallback(chain)
                beforeCallback.before(callback)
                if (callback.isSkipped()) {
                    result = callback.getSkipResult()
                    skipped = true
                }
            }

            if (!skipped) {
                try {
                    result = chain.proceed()
                } catch (t: Throwable) {
                    throwable = t
                }
            }

            val afterCallback = afterCallbacks[chain.executable]
            if (afterCallback != null) {
                val callback = AfterHookCallback(chain, result, throwable)
                afterCallback.after(callback)
                result = callback.result
                throwable = callback.throwable
            }

            if (throwable != null) {
                throw throwable
            }
            return result
        }
    }

    fun hookBefore(
        member: Member, callback: BeforeCallback
    ): XposedInterface.HookHandle {
        CustomHooker.beforeCallbacks[member] = callback
        return xposedModule.hook(member as Executable).intercept(CustomHooker)
    }

    fun hookAfter(
        member: Member, callback: AfterCallback
    ): XposedInterface.HookHandle {
        CustomHooker.afterCallbacks[member] = callback
        return xposedModule.hook(member as Executable).intercept(CustomHooker)
    }

    fun log(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e("CorePatch", message, throwable)
            xposedModule.log(Log.ERROR, "CorePatch", message, throwable)
        } else {
            if (BuildConfig.DEBUG) {
                Log.d("CorePatch", message)
            }
            xposedModule.log(Log.DEBUG, "CorePatch", message)
        }
    }

    fun deoptimize(method: Method): Boolean {
        return xposedModule.deoptimize(method)
    }
}