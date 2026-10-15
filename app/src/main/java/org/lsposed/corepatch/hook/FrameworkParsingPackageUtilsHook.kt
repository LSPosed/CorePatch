package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import org.lsposed.corepatch.Config
import org.lsposed.corepatch.XposedHelper.findClassIfExists
import org.lsposed.corepatch.XposedHelper.hookBefore

object FrameworkParsingPackageUtilsHook : BaseHook() {
    override val name = "FrameworkParsingPackageUtilsHook"

    @SuppressLint("PrivateApi")
    override fun hook() {
        // https://cs.android.com/android/platform/superproject/+/android-9.0.0_r61:frameworks/base/core/java/android/content/pm/PackageParser.java;l=2625
        // public static int computeMinSdkVersion(int minVers, String minCode, int platformSdkVersion, String[] platformSdkCodenames, String[] outError)
        // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r1:frameworks/base/core/java/android/content/pm/parsing/ParsingPackageUtils.java;l=1382
        // https://cs.android.com/android/platform/superproject/+/android-13.0.0_r1:frameworks/base/core/java/android/content/pm/parsing/FrameworkParsingPackageUtils.java;l=298
        // public static ParseResult<Integer> computeMinSdkVersion(int minVers, String minCode, int platformSdkVersion, String[] platformSdkCodenames, ParseInput input)
        listOf(
            "android.content.pm.PackageParser",
            "android.content.pm.parsing.ParsingPackageUtils",
            "android.content.pm.parsing.FrameworkParsingPackageUtils",
        ).mapNotNull { findClassIfExists(it) }
            .flatMap { clazz ->
                clazz.declaredMethods.filter { m ->
                    m.name == "computeMinSdkVersion" && m.parameterTypes.take(3) ==
                        listOf(Int::class.java, String::class.java, Int::class.java)
                }
            }
            .forEach { computeMinSdkVersionMethod ->
                hookBefore(computeMinSdkVersionMethod) { callback ->
                    if (Config.isBypassMinSdkEnabled()) {
                        val minVers = callback.args[0] as Int
                        val minCode = callback.args[1] as String?
                        val platformSdkVersion = callback.args[2] as Int
                        if (minCode == null && minVers > platformSdkVersion) {
                            callback.args[0] = platformSdkVersion
                        }
                    }
                }
            }
    }
}
