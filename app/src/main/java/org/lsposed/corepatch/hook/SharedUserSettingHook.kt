package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.os.Build
import org.lsposed.corepatch.Config
import org.lsposed.corepatch.XposedHelper.getOriginInvoker
import org.lsposed.corepatch.XposedHelper.hookAfter
import org.lsposed.corepatch.XposedHelper.hookBefore
import org.lsposed.corepatch.XposedHelper.hostClassLoader

object SharedUserSettingHook : BaseHook() {
    override val name = "SharedUserSettingHook"

    @SuppressLint("PrivateApi")
    override fun hook() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return

        val sharedUserSettingClazz =
            hostClassLoader.loadClass("com.android.server.pm.SharedUserSetting")
        val uidFlagsField = sharedUserSettingClazz.getDeclaredField("uidFlags")
        uidFlagsField.isAccessible = true
        // 12 final ArraySet<PackageSetting> packages;
        // 13 private final WatchedArraySet<PackageSetting> mPackages, get ArraySet mStorage
        val packagesField =
            sharedUserSettingClazz.declaredFields.first { f -> f.name == "packages" || f.name == "mPackages" }
        packagesField.isAccessible = true

        val packageSignaturesClazz =
            hostClassLoader.loadClass("com.android.server.pm.PackageSignatures")
        val signingDetailsField = packageSignaturesClazz.getDeclaredField("mSigningDetails")
        signingDetailsField.isAccessible = true

        val signingDetailsClazz = signingDetailsField.type
        val checkCapabilityMethod =
            signingDetailsClazz.getDeclaredMethod(
                "checkCapability", signingDetailsClazz, Int::class.java
            )
        val checkCapabilityInvoker = getOriginInvoker(checkCapabilityMethod) ?: return
        val mergeLineageWithMethod =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                signingDetailsClazz.getDeclaredMethod(
                    "mergeLineageWith", signingDetailsClazz, Int::class.java
                )
            } else {
                signingDetailsClazz.getDeclaredMethod("mergeLineageWith", signingDetailsClazz)
            }

        fun mergeLineageWith(first: Any, second: Any): Any? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                mergeLineageWithMethod.invoke(first, second, 2 /* MERGE_RESTRICTED_CAPABILITY */)
            } else {
                mergeLineageWithMethod.invoke(first, second)
            }

        val removePackageMethod =
            sharedUserSettingClazz.declaredMethods.first { m -> m.name == "removePackage" }
        hookBefore(removePackageMethod) { callback ->
            val thisObject = callback.thisObject ?: return@hookBefore
            if (!Config.isBypassDigestEnabled() || !Config.isBypassSharedUserEnabled()) {
                return@hookBefore
            }
            val uidFlags = uidFlagsField.get(thisObject) as Int
            if (uidFlags and ApplicationInfo.FLAG_SYSTEM != 0) {
                return@hookBefore // do not modify system's signature
            }
            val toRemove = callback.args[0] ?: return@hookBefore
            var removed = false
            val sharedUserSig = getSigningDetails(thisObject) ?: return@hookBefore
            var newSignatures: Any? = null

            val packagesSettings = getPackageStorage(packagesField.get(thisObject) ?: return@hookBefore)
            val valueAtMethod =
                packagesSettings.javaClass.declaredMethods.first { m -> m.name == "valueAt" }
            val pkgSize =
                packagesSettings.javaClass.declaredMethods.first { m -> m.name == "size" }
                    .invoke(packagesSettings) as Int
            if (pkgSize == 0) return@hookBefore
            for (i in 0 until pkgSize) {
                val pkg = valueAtMethod.invoke(packagesSettings, i) ?: continue
                // skip the removed package
                if (pkg == toRemove) {
                    removed = true
                    continue
                }
                val packagesSignatures = getSigningDetails(pkg) ?: continue
                val b1 = checkCapabilityInvoker.invoke(
                    packagesSignatures, sharedUserSig, 0
                ) as Boolean
                val b2 = checkCapabilityInvoker.invoke(
                    sharedUserSig, packagesSignatures, 0
                ) as Boolean
                // if old signing exists, return
                if (b1 || b2) {
                    return@hookBefore
                }
                // otherwise, choose the first signature we meet, and merge with others if possible
                // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/services/core/java/com/android/server/pm/ReconcilePackageUtils.java;l=193;drc=c9a8baf585e8eb0f3272443930301a61331b65c1
                // respect to system
                newSignatures = if (newSignatures == null) packagesSignatures
                else mergeLineageWith(newSignatures, packagesSignatures)
            }
            if (!removed || newSignatures == null) return@hookBefore
            setSigningDetails(thisObject, newSignatures)
        }

        val addPackageMethod =
            sharedUserSettingClazz.declaredMethods.first { m -> m.name == "addPackage" }
        // Read packages after addPackage
        hookAfter(addPackageMethod) { callback ->
            if (callback.throwable != null) return@hookAfter
            val thisObject = callback.thisObject ?: return@hookAfter
            if (!Config.isBypassDigestEnabled() || !Config.isBypassSharedUserEnabled()) {
                return@hookAfter
            }
            val uidFlags = uidFlagsField.get(thisObject) as Int
            if (uidFlags and ApplicationInfo.FLAG_SYSTEM != 0) {
                return@hookAfter // do not modify system's signature
            }
            val sharedUserSig = getSigningDetails(thisObject) ?: return@hookAfter
            var newSignatures: Any? = null
            val packagesSettings = getPackageStorage(packagesField.get(thisObject) ?: return@hookAfter)
            val valueAtMethod =
                packagesSettings.javaClass.declaredMethods.first { m -> m.name == "valueAt" }
            val pkgSize =
                packagesSettings.javaClass.declaredMethods.first { m -> m.name == "size" }
                    .invoke(packagesSettings) as Int
            for (i in 0 until pkgSize) {
                val pkg = valueAtMethod.invoke(packagesSettings, i) ?: continue
                val packagesSignatures = getSigningDetails(pkg) ?: continue
                val b1 = checkCapabilityInvoker.invoke(
                    packagesSignatures, sharedUserSig, 0
                ) as Boolean
                val b2 = checkCapabilityInvoker.invoke(
                    sharedUserSig, packagesSignatures, 0
                ) as Boolean
                // if old signing exists, return
                if (b1 || b2) return@hookAfter
                // otherwise, choose the first signature we meet, and merge with others if possible
                // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/services/core/java/com/android/server/pm/ReconcilePackageUtils.java;l=193;drc=c9a8baf585e8eb0f3272443930301a61331b65c1
                // respect to system
                newSignatures = if (newSignatures == null) packagesSignatures
                else mergeLineageWith(newSignatures, packagesSignatures)
            }
            if (newSignatures == null) return@hookAfter
            setSigningDetails(thisObject, newSignatures)
        }
    }

    fun getPackageStorage(packagesSettings: Any): Any {
        val storageField = packagesSettings.javaClass.declaredFields.firstOrNull { it.name == "mStorage" }
        storageField?.isAccessible = true
        return storageField?.get(packagesSettings) ?: packagesSettings
    }

    fun getSigningDetails(pkgOrSharedUser: Any): Any? {
        val signaturesField = try {
            pkgOrSharedUser.javaClass.getDeclaredField("signatures")
        } catch (_: NoSuchFieldException) {
            pkgOrSharedUser.javaClass.superclass?.getDeclaredField("signatures")
        }
        signaturesField?.isAccessible = true
        val signatures = signaturesField?.get(pkgOrSharedUser)
        val mSigningDetailsField = signatures?.javaClass?.getDeclaredField("mSigningDetails")
        mSigningDetailsField?.isAccessible = true
        return mSigningDetailsField?.get(signatures)
    }

    fun setSigningDetails(pkgOrSharedUser: Any, signingDetails: Any?) {
        val signaturesField = try {
            pkgOrSharedUser.javaClass.getDeclaredField("signatures")
        } catch (_: NoSuchFieldException) {
            pkgOrSharedUser.javaClass.superclass?.getDeclaredField("signatures") ?: return
        }
        signaturesField.isAccessible = true
        val signatures = signaturesField.get(pkgOrSharedUser)
        val mSigningDetailsField = signatures.javaClass.getDeclaredField("mSigningDetails")
        mSigningDetailsField.isAccessible = true
        mSigningDetailsField.set(signatures, signingDetails)
    }
}
