package org.lsposed.corepatch

import android.util.Log
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Executable
import java.lang.reflect.Method

typealias BeforeCallback = (XposedHelper.BeforeHookCallback) -> Unit
typealias AfterCallback = (XposedHelper.AfterHookCallback) -> Unit

object XposedHelper {
    lateinit var xposedModule: XposedModule
        private set
    lateinit var hostClassLoader: ClassLoader
        private set
    val prefs by lazy { xposedModule.getRemotePreferences("conf") }

    fun setXposedModule(module: XposedModule) {
        xposedModule = module
    }

    fun setHostClassLoader(classLoader: ClassLoader) {
        hostClassLoader = classLoader
    }

    class BeforeHookCallback(private val chain: XposedInterface.Chain) {
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
        val thisObject: Any? get() = chain.thisObject
        val args: Array<Any?> get() = chain.args.toTypedArray()
    }

    internal class CustomHooker(
        val beforeCallback: BeforeCallback = {},
        val afterCallback: AfterCallback = {},
    ) : XposedInterface.Hooker {
        override fun intercept(chain: XposedInterface.Chain): Any? {
            var result: Any? = null
            var throwable: Throwable? = null
            var skipped = false

            val bcb = BeforeHookCallback(chain)
            beforeCallback(bcb)
            if (bcb.isSkipped()) {
                result = bcb.getSkipResult()
                skipped = true
            }

            if (!skipped) {
                try {
                    result = chain.proceed()
                } catch (t: Throwable) {
                    throwable = t
                }
            }

            val acb = AfterHookCallback(chain, result, throwable)
            afterCallback(acb)
            result = acb.result
            throwable = acb.throwable

            if (throwable != null) {
                throw throwable
            }
            return result
        }
    }

    fun hookBefore(
        member: Executable, callback: BeforeCallback
    ): XposedInterface.HookHandle {
        return xposedModule.hook(member).intercept(CustomHooker(beforeCallback = callback))
    }

    fun hookAfter(
        executable: Executable, callback: AfterCallback
    ): XposedInterface.HookHandle {
        return xposedModule.hook(executable).intercept(CustomHooker(afterCallback = callback))
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
