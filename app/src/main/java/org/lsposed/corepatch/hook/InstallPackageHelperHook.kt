package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import android.os.Build
import org.lsposed.corepatch.Config
import org.lsposed.corepatch.XposedHelper.getOriginInvoker
import org.lsposed.corepatch.XposedHelper.hookAfter
import org.lsposed.corepatch.XposedHelper.hostClassLoader
import org.lsposed.corepatch.XposedHelper.log

object InstallPackageHelperHook : BaseHook() {
    override val name = "InstallPackageHelperHook"

    @SuppressLint("PrivateApi")
    override fun hook() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val installPackageHelperClazz =
            hostClassLoader.loadClass("com.android.server.pm.InstallPackageHelper")
        val doesSignatureMatchForPermissionsMethod =
            installPackageHelperClazz.declaredMethods.first { m -> m.name == "doesSignatureMatchForPermissions" }
        val doesSignatureMatchForPermissionsInvoker =
            getOriginInvoker(doesSignatureMatchForPermissionsMethod)
        val parsedPackageClass = doesSignatureMatchForPermissionsMethod.parameterTypes[1]
        val getPackageNameMethod = parsedPackageClass.methods.first { method ->
            method.name == "getPackageName" && method.parameterCount == 0
        }.apply { isAccessible = true }
        val installedPackages = createInstalledPackageLookup(installPackageHelperClazz)

        hookAfter(doesSignatureMatchForPermissionsMethod) hook@{ callback ->
            if (!Config.isBypassDigestEnabled() || !Config.isUsePreviousSignaturesEnabled()) {
                return@hook
            }
            if (callback.result != false) return@hook

            val parsedPackage = callback.args[1] ?: return@hook
            val packageName = getPackageNameMethod.invoke(parsedPackage) as? String ?: return@hook
            val sourcePackageName = callback.args[0] as? String ?: return@hook
            if (packageName == sourcePackageName) {
                callback.result = true
                return@hook
            }

            val helper = callback.thisObject ?: return@hook
            val oldPackage = installedPackages?.invoke(helper, packageName) ?: return@hook
            if (!parsedPackageClass.isInstance(oldPackage)) return@hook

            val matches = runCatching {
                doesSignatureMatchForPermissionsInvoker?.invoke(
                    helper,
                    sourcePackageName,
                    oldPackage,
                    callback.args[2],
                ) as? Boolean == true
            }.getOrElse { throwable ->
                log("[$name] failed to check the installed package signature", throwable)
                false
            }
            if (matches) callback.result = true
        }
    }

    private fun createInstalledPackageLookup(
        installPackageHelperClazz: Class<*>
    ): ((Any, String) -> Any?)? =
        runCatching {
            val packageManagerField = installPackageHelperClazz.getDeclaredField("mPm").apply {
                isAccessible = true
            }
            val packageManagerClazz = packageManagerField.type
            val snapshotComputerMethod = packageManagerClazz.declaredMethods.first { method ->
                method.name == "snapshotComputer" && method.parameterCount == 0
            }.apply { isAccessible = true }
            val getPackageStateMethod =
                snapshotComputerMethod.returnType.declaredMethods.first { method ->
                    method.name == "getPackageStateInternal" &&
                        method.parameterTypes.contentEquals(arrayOf(String::class.java))
                }.apply { isAccessible = true }
            val getAndroidPackageMethod =
                getPackageStateMethod.returnType.methods.first { method ->
                    method.name == "getAndroidPackage" && method.parameterCount == 0
                }.apply { isAccessible = true }

            val lookup: (Any, String) -> Any? = { helper, packageName ->
                runCatching {
                    val packageManager = packageManagerField.get(helper)
                    val computer = snapshotComputerMethod.invoke(packageManager)
                    val packageState = getPackageStateMethod.invoke(computer, packageName)
                    packageState?.let { getAndroidPackageMethod.invoke(it) }
                }.getOrElse { throwable ->
                    log("[$name] failed to find the installed package", throwable)
                    null
                }
            }
            lookup
        }.getOrElse { throwable ->
            log("[$name] installed package lookup is unavailable", throwable)
            null
        }
}
