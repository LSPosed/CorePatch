package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.os.Build
import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import org.lsposed.corepatch.Config
import org.lsposed.corepatch.XposedHelper.BeforeCallback
import org.lsposed.corepatch.XposedHelper.hookBefore
import org.lsposed.corepatch.XposedHelper.hostClassLoader
import org.lsposed.corepatch.XposedHelper.xposedModule

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
        // 13 private final WatchedArraySet<PackageSetting> mPackages;
        val packagesField =
            sharedUserSettingClazz.declaredFields.first { f -> f.name == "packages" || f.name == "mPackages" }
        packagesField.isAccessible = true

        val packageSignaturesClazz =
            hostClassLoader.loadClass("com.android.server.pm.PackageSignatures")
        val signingDetailsField = packageSignaturesClazz.getDeclaredField("mSigningDetails")
        signingDetailsField.isAccessible = true

        val signingDetailsClazz = signingDetailsField.type
        val checkCapabilityMethod =
            signingDetailsClazz.declaredMethods.first { m -> m.name == "checkCapability" && m.returnType == Boolean::class.java }
        val mergeLineageWithMethod =
            signingDetailsClazz.declaredMethods.first { m -> m.name == "mergeLineageWith" }

        val removePackageMethod =
            sharedUserSettingClazz.declaredMethods.first { m -> m.name == "removePackage" }
        hookBefore(removePackageMethod, object : BeforeCallback {
            override fun before(callback: BeforeHookCallback) {
                val thisObject = callback.thisObject ?: return
                if (!Config.isBypassVerificationEnabled() || !Config.isBypassSharedUserEnabled()) {
                    return
                }
                val uidFlags = uidFlagsField.get(thisObject) as Int
                if (uidFlags and ApplicationInfo.FLAG_SYSTEM != 0) {
                    return // do not modify system's signature
                }
                val toRemove = callback.args[0] ?: return
                var removed = false
                val sharedUserSig = getSigningDetails(thisObject) ?: return
                var newSignatures: Any? = null

                val packagesSettings = packagesField.get(thisObject)
                val pkgSize =
                    packagesSettings.javaClass.declaredMethods.first { m -> m.name == "size" }
                        .invoke(packagesSettings) as Int
                if (pkgSize == 0) return
                for (i in 0..pkgSize) {
                    val pkg =
                        packagesSettings.javaClass.declaredMethods.first { m -> m.name == "valueAt" }
                            .invoke(packagesSettings, i)
                    // skip the removed package
                    if (pkg == toRemove) {
                        removed = true
                        continue
                    }
                    val packagesSignatures = getSigningDetails(pkg) ?: continue
                    val b1 = xposedModule.invokeOrigin(
                        checkCapabilityMethod, packagesSignatures, sharedUserSig, 0
                    ) as Boolean
                    val b2 = xposedModule.invokeOrigin(
                        checkCapabilityMethod, sharedUserSig, packagesSignatures, 0
                    ) as Boolean
                    // if old signing exists, return
                    if (b1 || b2) {
                        return
                    }
                    // otherwise, choose the first signature we meet, and merge with others if possible
                    // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/services/core/java/com/android/server/pm/ReconcilePackageUtils.java;l=193;drc=c9a8baf585e8eb0f3272443930301a61331b65c1
                    // respect to system
                    newSignatures = if (newSignatures == null) packagesSignatures
                    else mergeLineageWithMethod.invoke(newSignatures, packagesSignatures)
                }
                if (!removed || newSignatures == null) return
                setSigningDetails(thisObject, newSignatures)
            }
        })

        val addPackageMethod =
            sharedUserSettingClazz.declaredMethods.first { m -> m.name == "addPackage" }
        hookBefore(addPackageMethod, object : BeforeCallback {
            override fun before(callback: BeforeHookCallback) {
                val thisObject = callback.thisObject ?: return
                if (!Config.isBypassVerificationEnabled() || !Config.isBypassSharedUserEnabled()) {
                    return
                }
                val uidFlags = uidFlagsField.get(thisObject) as Int
                if (uidFlags and ApplicationInfo.FLAG_SYSTEM != 0) {
                    return // do not modify system's signature
                }
                val toAdd = callback.args[0] ?: return
                var added = false
                val sharedUserSig = getSigningDetails(thisObject) ?: return
                var newSignatures: Any? = null
                val packagesSettings = packagesField.get(thisObject)
                val pkgSize =
                    packagesSettings.javaClass.declaredMethods.first { m -> m.name == "size" }
                        .invoke(packagesSettings) as Int
                if (pkgSize == 0) return
                for (i in 0..pkgSize) {
                    var pkg =
                        packagesSettings.javaClass.declaredMethods.first { m -> m.name == "valueAt" }
                            .invoke(packagesSettings, i)
                    // skip the added package
                    if (pkg == toAdd) {
                        added = true
                        pkg = toAdd
                    }
                    val packagesSignatures = getSigningDetails(pkg) ?: continue
                    val b1 = xposedModule.invokeOrigin(
                        checkCapabilityMethod, packagesSignatures, sharedUserSig, 0
                    ) as Boolean
                    val b2 = xposedModule.invokeOrigin(
                        checkCapabilityMethod, sharedUserSig, packagesSignatures, 0
                    ) as Boolean
                    // if old signing exists, return
                    if (b1 || b2) return
                    // otherwise, choose the first signature we meet, and merge with others if possible
                    // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/services/core/java/com/android/server/pm/ReconcilePackageUtils.java;l=193;drc=c9a8baf585e8eb0f3272443930301a61331b65c1
                    // respect to system
                    newSignatures = if (newSignatures == null) packagesSignatures
                    else mergeLineageWithMethod.invoke(sharedUserSig, packagesSignatures)
                }
                if (!added || newSignatures == null) return
                setSigningDetails(thisObject, newSignatures)
            }
        })
    }

    fun getSigningDetails(pkgOrSharedUser: Any): Any? {
        val signaturesField = try {
            pkgOrSharedUser.javaClass.getDeclaredField("signatures")
        } catch (ignored: NoSuchFieldException) {
            pkgOrSharedUser.javaClass.superclass.getDeclaredField("signatures")
        }
        signaturesField.isAccessible = true
        val signatures = signaturesField.get(pkgOrSharedUser)
        val mSigningDetailsField = signatures.javaClass.getDeclaredField("mSigningDetails")
        mSigningDetailsField.isAccessible = true
        return mSigningDetailsField.get(signatures)
    }

    fun setSigningDetails(pkgOrSharedUser: Any, signingDetails: Any?) {
        val signaturesField = pkgOrSharedUser.javaClass.getDeclaredField("signatures")
        signaturesField.isAccessible = true
        val signatures = signaturesField.get(pkgOrSharedUser)
        val mSigningDetailsField = signatures.javaClass.getDeclaredField("mSigningDetails")
        mSigningDetailsField.isAccessible = true
        mSigningDetailsField.set(signatures, signingDetails)
    }
}