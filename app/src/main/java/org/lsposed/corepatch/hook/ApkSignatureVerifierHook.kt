package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.AfterHookCallback
import org.lsposed.corepatch.Config
import org.lsposed.corepatch.Constant
import org.lsposed.corepatch.XposedHelper.AfterCallback
import org.lsposed.corepatch.XposedHelper.BeforeCallback
import org.lsposed.corepatch.XposedHelper.hookAfter
import org.lsposed.corepatch.XposedHelper.hookBefore
import org.lsposed.corepatch.XposedHelper.hostClassLoader
import org.lsposed.corepatch.XposedHelper.log

object ApkSignatureVerifierHook : BaseHook() {
    override val name = "ApkSignatureVerifierHook"

    @SuppressLint("PrivateApi", "SoonBlockedPrivateApi", "DiscouragedPrivateApi")
    override fun hook() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return

        val apkSignatureVerifierClazz =
            hostClassLoader.loadClass("android.util.apk.ApkSignatureVerifier")

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            val signingDetailsClazz =
                hostClassLoader.loadClass("android.content.pm.PackageParser\$SigningDetails")
            // SigningDetails(Signature[] signatures, int signatureSchemeVersion)
            val signingDetailsConstructor =
                signingDetailsClazz.declaredConstructors.first { c -> c.parameterCount == 2 && c.parameterTypes[1] == Int::class.java }

            val packageParserExceptionClazz =
                hostClassLoader.loadClass("android.content.pm.PackageParser\$PackageParserException")
            val errorField = packageParserExceptionClazz.getDeclaredField("error")

            val strictJarFileClazz = hostClassLoader.loadClass("android.util.jar.StrictJarFile")
            val strictJarFileConstructor = strictJarFileClazz.getDeclaredConstructor(
                String::class.java, Boolean::class.java, Boolean::class.java
            )

            // https://cs.android.com/android/platform/superproject/+/android-9.0.0_r59:frameworks/base/core/java/android/util/apk/ApkSignatureVerifier.java;l=162
            // private static PackageParser.SigningDetails verifyV1Signature(
            //     String apkPath, boolean verifyFull)
            // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r34:frameworks/base/core/java/android/util/apk/ApkSignatureVerifier.java;l=355
            // private static SigningDetailsWithDigests verifyV1Signature(String apkPath, boolean verifyFull)
            // https://cs.android.com/android/platform/superproject/+/android-13.0.0_r74:frameworks/base/core/java/android/util/apk/ApkSignatureVerifier.java;l=362
            // private static ParseResult<SigningDetailsWithDigests> verifyV1Signature(
            //     ParseInput input, String apkPath, boolean verifyFull)
            val verifyV1SignatureMethod = apkSignatureVerifierClazz.getDeclaredMethod(
                "verifyV1Signature", String::class.java, Boolean::class.java
            )
            hookAfter(verifyV1SignatureMethod, object : AfterCallback {
                override fun after(callback: AfterHookCallback) {
                    if (Config.isBypassVerificationEnabled()) {
                        val signingDetailsArgs: Array<Any> =
                            arrayOf(arrayOf(Signature(Constant.SIGNATURE)), 1)

                        var errorCode: Int? = null
                        // 13
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val parseResult = callback.result as Any
                            val isError = parseResult.javaClass.getDeclaredMethod("isError")
                                .invoke(parseResult) as Boolean
                            if (isError) {
                                errorCode = parseResult.javaClass.getDeclaredMethod("getErrorCode")
                                    .invoke(parseResult) as Int
                            }
                        }

                        var signaturesBefore: Any? = null
                        // use previous signatures, get from package manager
                        if (Config.isUsePreviousSignaturesEnabled()) {
                            try {
                                val activityThreadClazz =
                                    hostClassLoader.loadClass("android.app.ActivityThread")
                                val currentApplicationMethod =
                                    activityThreadClazz.getDeclaredMethod("currentApplication")
                                val application =
                                    currentApplicationMethod.invoke(null) as Application
                                val packageManager = application.packageManager
                                if (packageManager == null) {
                                    log("Cannot get the Package Manager... Are you using MiUI?")
                                } else {
                                    // 13
                                    val packageInfo =
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            packageManager.getPackageArchiveInfo(
                                                callback.args[1] as String, 0
                                            )
                                        } else { // 9
                                            packageManager.getPackageArchiveInfo(
                                                callback.args[0] as String, 0
                                            )
                                        }
                                    packageInfo?.let { pi ->
                                        val installedPackageInfo = packageManager.getPackageInfo(
                                            pi.packageName, PackageManager.GET_SIGNING_CERTIFICATES
                                        )
                                        signaturesBefore =
                                            installedPackageInfo.signingInfo.signingCertificateHistory
                                    }
                                }
                            } catch (t: Throwable) {
                                log("cannot get signatures from installed package: ${t.message}")
                            }
                        }
                        // if previous signatures not found, parse it from apk
                        if (signaturesBefore == null && Config.isBypassDigestEnabled()) {
                            val originalJarFile = strictJarFileConstructor.newInstance(
                                callback.args[if (errorCode == null) 0 else 1], true, false
                            )
                            val manifestEntry =
                                originalJarFile.javaClass.declaredMethods.first { m -> m.name == "findEntry" && m.parameterCount == 1 && m.parameterTypes[0] == String::class.java }
                                    .invoke(originalJarFile, "AndroidManifest.xml")

                            //  9 private static Certificate[][] loadCertificates(StrictJarFile jarFile, ZipEntry entry)
                            // 13 private static ParseResult<Certificate[][]> loadCertificates(
                            //     ParseInput input, StrictJarFile jarFile, ZipEntry entry)
                            val loadCertificatesMethod =
                                apkSignatureVerifierClazz.declaredMethods.first { m -> m.name == "loadCertificates" }
                            loadCertificatesMethod.isAccessible = true
                            val lastCerts =
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    loadCertificatesMethod.isAccessible = true
                                    val certs = loadCertificatesMethod.invoke(
                                        null, originalJarFile, manifestEntry, false
                                    )
                                    certs.javaClass.declaredMethods.first { m -> m.name == "getResult" }
                                        .invoke(certs) as Array<*>
                                } else {
                                    loadCertificatesMethod.invoke(
                                        null, originalJarFile, manifestEntry
                                    ) as Array<*>
                                }
                            val convertToSignaturesMethod =
                                apkSignatureVerifierClazz.declaredMethods.first { m -> m.name == "convertToSignatures" }
                            signaturesBefore = convertToSignaturesMethod.invoke(
                                null, lastCerts
                            )
                        }
                        if (signaturesBefore != null) {
                            signingDetailsArgs[0] = signaturesBefore!!
                        }
                        val signingDetails =
                            signingDetailsConstructor.newInstance(*signingDetailsArgs)

                        val newResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val signingDetailsWithDigestsClazz =
                                hostClassLoader.loadClass("android.util.apk.ApkSignatureVerifier.SigningDetailsWithDigests")
                            val signingDetailsWithDigestsConstructor =
                                signingDetailsWithDigestsClazz.getDeclaredConstructor(
                                    signingDetailsClazz, Map::class.java
                                )
                            signingDetailsWithDigestsConstructor.isAccessible = true
                            signingDetailsWithDigestsConstructor.newInstance(signingDetails, null)
                        } else {
                            signingDetails
                        }

                        val throwable = callback.throwable ?: return
                        if (throwable.javaClass == packageParserExceptionClazz && errorField.getInt(
                                throwable
                            ) == -103
                        ) {
                            callback.result = newResult
                        }
                        val cause = throwable.cause ?: return
                        if (cause.javaClass == packageParserExceptionClazz && errorField.getInt(
                                cause
                            ) == -103
                        ) {
                            callback.result = newResult
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && errorCode == -103) {
                            val input = callback.args[0]
                            input.javaClass.declaredMethods.first { m -> m.name == "reset" }
                                .invoke(input)
                            callback.result =
                                input.javaClass.declaredMethods.first { m -> m.name == "success" }
                                    .invoke(input, newResult)
                        }
                    }
                }
            })
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // No signature found in package of version " + minSignatureSchemeVersion
            // + " or newer for package " + apkPath
            // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r48:frameworks/base/core/java/android/util/apk/ApkSignatureVerifier.java;l=460
            // public static int getMinimumSignatureSchemeVersionForTargetSdk(int targetSdk)
            val getMinimumSignatureSchemeVersionForTargetSdkMethod =
                apkSignatureVerifierClazz.getDeclaredMethod(
                    "getMinimumSignatureSchemeVersionForTargetSdk", Int::class.java
                )
            hookBefore(getMinimumSignatureSchemeVersionForTargetSdkMethod, object : BeforeCallback {
                override fun before(callback: XposedInterface.BeforeHookCallback) {
                    if (Config.isBypassVerificationEnabled()) {
                        callback.returnAndSkip(0)
                    }
                }
            })
        }
    }
}