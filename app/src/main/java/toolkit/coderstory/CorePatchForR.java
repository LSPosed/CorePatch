package toolkit.coderstory;


import android.annotation.TargetApi;
import android.app.AndroidAppHelper;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.util.Log;

import com.coderstory.toolkit.BuildConfig;

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

@TargetApi(Build.VERSION_CODES.R)
public class CorePatchForR extends XposedHelper implements IXposedHookLoadPackage {
    private final static Method deoptimizeMethod;

    static {
        Method m = null;
        try {
            m = XposedBridge.class.getDeclaredMethod("deoptimizeMethod", Member.class);
        } catch (Throwable t) {
            XposedBridge.log("E/" + MainHook.TAG + " " + Log.getStackTraceString(t));
        }
        deoptimizeMethod = m;
    }

    static void deoptimizeMethod(Class<?> c, String n) throws InvocationTargetException, IllegalAccessException {
        for (Method m : c.getDeclaredMethods()) {
            if (deoptimizeMethod != null && m.getName().equals(n)) {
                deoptimizeMethod.invoke(null, m);
                if (BuildConfig.DEBUG)
                    XposedBridge.log("D/" + MainHook.TAG + " Deoptimized " + m.getName());
            }
        }
    }

    final XSharedPreferences prefs = new XSharedPreferences(BuildConfig.APPLICATION_ID, "conf");

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        if (BuildConfig.DEBUG) {
            XposedBridge.log("D/" + MainHook.TAG + " downgrade=" + prefs.getBoolean("downgrade", true));
            XposedBridge.log("D/" + MainHook.TAG + " authcreak=" + prefs.getBoolean("authcreak", false));
            XposedBridge.log("D/" + MainHook.TAG + " digestCreak=" + prefs.getBoolean("digestCreak", true));
            XposedBridge.log("D/" + MainHook.TAG + " exactSigCheck=" + prefs.getBoolean("exactSigCheck", false));
            XposedBridge.log("D/" + MainHook.TAG + " UsePreSig=" + prefs.getBoolean("UsePreSig", false));
            XposedBridge.log("D/" + MainHook.TAG + " bypassBlock=" + prefs.getBoolean("bypassBlock", true));
            XposedBridge.log("D/" + MainHook.TAG + " sharedUser=" + prefs.getBoolean("sharedUser", false));
            XposedBridge.log("D/" + MainHook.TAG + " disableVerificationAgent=" + prefs.getBoolean("disableVerificationAgent", true));
        }

        var pmService = XposedHelpers.findClassIfExists("com.android.server.pm.PackageManagerService",
                loadPackageParam.classLoader);
        if (pmService != null) {
            var checkDowngrade = XposedHelpers.findMethodExactIfExists(pmService, "checkDowngrade",
                    "com.android.server.pm.parsing.pkg.AndroidPackage",
                    "android.content.pm.PackageInfoLite");
            if (checkDowngrade != null) {
                // 允许降级
                XposedBridge.hookMethod(checkDowngrade, new ReturnConstant(prefs, "downgrade", null));
            }
            // exists on flyme 9(Android 11) only
            var flymeCheckDowngrade = XposedHelpers.findMethodExactIfExists(pmService, "checkDowngrade",
                    "android.content.pm.PackageInfoLite",
                    "android.content.pm.PackageInfoLite");
            if (flymeCheckDowngrade != null)
                XposedBridge.hookMethod(flymeCheckDowngrade, new ReturnConstant(prefs, "downgrade", true));
        }

        // apk内文件修改后 digest校验会失败
        hookAllMethods("android.util.jar.StrictJarVerifier", loadPackageParam.classLoader, "verifyMessageDigest",
                new ReturnConstant(prefs, "authcreak", true));
        hookAllMethods("android.util.jar.StrictJarVerifier", loadPackageParam.classLoader, "verify",
                new ReturnConstant(prefs, "authcreak", true));
        hookAllMethods("java.security.MessageDigest", loadPackageParam.classLoader, "isEqual",
                new ReturnConstant(prefs, "authcreak", true));

        // Targeting R+ (version " + Build.VERSION_CODES.R + " and above) requires"
        // + " the resources.arsc of installed APKs to be stored uncompressed"
        // + " and aligned on a 4-byte boundary
        // target >=30 的情况下 resources.arsc 必须是未压缩的且4K对齐
        hookAllMethods("android.content.res.AssetManager", loadPackageParam.classLoader, "containsAllocatedTable",
                new ReturnConstant(prefs, "authcreak", false));

        // Signature scheme version constants (self-documenting)
        // int SIGNATURE_SCHEME_VERSION__UNKNOWN = 0;
        int SIGNATURE_SCHEME_VERSION__JAR = 1;
        // int SIGNATURE_SCHEME_VERSION__SIGNING_BLOCK_V2 = 2;
        int SIGNATURE_SCHEME_VERSION__SIGNING_BLOCK_V3 = 3;
        // int SIGNATURE_SCHEME_VERSION__SIGNING_BLOCK_V4 = 4;

        // No signature found in package of version " + minSignatureSchemeVersion
        // + " or newer for package " + apkPath
        findAndHookMethod("android.util.apk.ApkSignatureVerifier", loadPackageParam.classLoader, "getMinimumSignatureSchemeVersionForTargetSdk", int.class,
                new ReturnConstant(prefs, "authcreak", 0));
        var apkVerifierClass = XposedHelpers.findClassIfExists("com.android.apksig.ApkVerifier",
                loadPackageParam.classLoader);
        if (apkVerifierClass != null) {
            findAndHookMethod(apkVerifierClass, "getMinimumSignatureSchemeVersionForTargetSdk", int.class,
                    new ReturnConstant(prefs, "authcreak", 0));
        }

        // ApkSignatureVerifier.verifySignatures(apkPath, minVersion, verifyFull)
        // https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-12.1.0_r27/core/java/android/util/apk/ApkSignatureVerifier.java#107
        // All scheme versions pass through this, and they all respect `verifyFull`
        hookAllMethods("android.util.apk.ApkSignatureVerifier", loadPackageParam.classLoader, "verifySignatures",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (prefs.getBoolean("authcreak", false)) {
                            // Force verifyFull parameter to false
                            if (param.args.length > 2 && param.args[2] instanceof Boolean) {
                                param.args[2] = false;
                                XposedBridge.log("I/" + MainHook.TAG + " Forcing verifyFull=false for " + param.args[0]);
                            }
                        }
                    }
                });

        // 当verifyV1Signature抛出转换异常时，替换一个签名作为返回值
        // 如果用户已安装apk，并且其定义了私有权限，则安装时会因签名与模块内硬编码的不一致而被拒绝。尝试从待安装apk中获取签名。如果其中apk的签名和已安装的一致（只动了内容）就没有问题。此策略可能有潜在的安全隐患。
        Class<?> pkc = XposedHelpers.findClass("sun.security.pkcs.PKCS7", loadPackageParam.classLoader);
        Constructor<?> constructor = XposedHelpers.findConstructorExact(pkc, byte[].class);
        constructor.setAccessible(true);
        Class<?> ASV = XposedHelpers.findClass("android.util.apk.ApkSignatureVerifier", loadPackageParam.classLoader);
        Class<?> sJarClass = XposedHelpers.findClass("android.util.jar.StrictJarFile", loadPackageParam.classLoader);
        Constructor<?> constructorExact = XposedHelpers.findConstructorExact(sJarClass, String.class, boolean.class, boolean.class);
        constructorExact.setAccessible(true);
        Class<?> signingDetails = getSigningDetails(loadPackageParam.classLoader);
        Constructor<?> findConstructorExact = XposedHelpers.findConstructorExact(signingDetails, Signature[].class, Integer.TYPE);
        findConstructorExact.setAccessible(true);
        Class<?> packageParserException = XposedHelpers.findClass("android.content.pm.PackageParser.PackageParserException", loadPackageParam.classLoader);
        Field error = XposedHelpers.findField(packageParserException, "error");
        error.setAccessible(true);
        Object[] signingDetailsArgs = new Object[2];
        signingDetailsArgs[1] = SIGNATURE_SCHEME_VERSION__JAR;
        Class<?> parseResult = XposedHelpers.findClassIfExists("android.content.pm.parsing.result.ParseResult", loadPackageParam.classLoader);
        hookAllMethods("android.util.jar.StrictJarVerifier", loadPackageParam.classLoader, "verifyBytes", new XC_MethodHook() {
            public void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (prefs.getBoolean("digestCreak", true)) {
                    if (!prefs.getBoolean("UsePreSig", false)) {
                        final Object block = constructor.newInstance(param.args[0]);
                        Object[] infos = (Object[]) XposedHelpers.callMethod(block, "getSignerInfos");
                        Object info = infos[0];
                        @SuppressWarnings("unchecked")
                        List<X509Certificate> verifiedSignerCertChain = (List<X509Certificate>) XposedHelpers.callMethod(info, "getCertificateChain", block);
                        param.setResult(verifiedSignerCertChain.toArray(
                                new X509Certificate[0]));
                    }
                }
            }
        });
        hookAllMethods("android.util.apk.ApkSignatureVerifier", loadPackageParam.classLoader, "verifyV1Signature", new XC_MethodHook() {
            public void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                if (prefs.getBoolean("authcreak", false)) {
                    Throwable throwable = methodHookParam.getThrowable();
                    Integer parseErr = null;
                    if (parseResult != null && ((Method) methodHookParam.method).getReturnType() == parseResult) {
                        Object result = methodHookParam.getResult();
                        if ((boolean) XposedHelpers.callMethod(result, "isError")) {
                            parseErr = (int) XposedHelpers.callMethod(result, "getErrorCode");
                        }
                    }
                    if (throwable != null || parseErr != null) {
                        Signature[] lastSigs = null;
                        try {
                            if (prefs.getBoolean("UsePreSig", false)) {
                                PackageManager PM = AndroidAppHelper.currentApplication().getPackageManager();
                                if (PM == null) {
                                    XposedBridge.log("E/" + MainHook.TAG + " " + BuildConfig.APPLICATION_ID + " Cannot get the Package Manager... Are you using MiUI?");
                                } else {
                                    PackageInfo pI;
                                    if (parseErr != null) {
                                        pI = PM.getPackageArchiveInfo((String) methodHookParam.args[1], 0);
                                    } else {
                                        pI = PM.getPackageArchiveInfo((String) methodHookParam.args[0], 0);
                                    }
                                    PackageInfo InstpI = PM.getPackageInfo(pI.packageName, PackageManager.GET_SIGNING_CERTIFICATES);
                                    lastSigs = InstpI.signingInfo.getSigningCertificateHistory();
                                }
                            }
                        } catch (Throwable ignored) {

                        }
                        try {
                            if (lastSigs == null && prefs.getBoolean("digestCreak", true)) {
                                final Object origJarFile = constructorExact.newInstance(methodHookParam.args[parseErr == null ? 0 : 1], true, false);
                                final ZipEntry manifestEntry = (ZipEntry) XposedHelpers.callMethod(origJarFile, "findEntry", "AndroidManifest.xml");
                                final Certificate[][] lastCerts;
                                if (parseErr != null) {
                                    lastCerts = (Certificate[][]) XposedHelpers.callMethod(XposedHelpers.callStaticMethod(ASV, "loadCertificates", methodHookParam.args[0], origJarFile, manifestEntry), "getResult");
                                } else {
                                    lastCerts = (Certificate[][]) XposedHelpers.callStaticMethod(ASV, "loadCertificates", origJarFile, manifestEntry);
                                }
                                lastSigs = (Signature[]) XposedHelpers.callStaticMethod(ASV, "convertToSignatures", (Object) lastCerts);
                            }
                        } catch (Throwable ignored) {
                        }
                        signingDetailsArgs[0] = Objects.requireNonNullElseGet(lastSigs, () -> new Signature[]{new Signature(SIGNATURE)});
                        Object newInstance = findConstructorExact.newInstance(signingDetailsArgs);

                        //修复 java.lang.ClassCastException: Cannot cast android.content.pm.PackageParser$SigningDetails to android.util.apk.ApkSignatureVerifier$SigningDetailsWithDigests
                        Class<?> signingDetailsWithDigests = XposedHelpers.findClassIfExists("android.util.apk.ApkSignatureVerifier.SigningDetailsWithDigests", loadPackageParam.classLoader);
                        if (signingDetailsWithDigests != null) {
                            Constructor<?> signingDetailsWithDigestsConstructorExact = XposedHelpers.findConstructorExact(signingDetailsWithDigests, signingDetails, Map.class);
                            signingDetailsWithDigestsConstructorExact.setAccessible(true);
                            newInstance = signingDetailsWithDigestsConstructorExact.newInstance(newInstance, null);
                        }
                        if (throwable != null) {
                            Throwable cause = throwable.getCause();
                            if (throwable.getClass() == packageParserException) {
                                if (error.getInt(throwable) == -103) {
                                    methodHookParam.setResult(newInstance);
                                }
                            }
                            if (cause != null && cause.getClass() == packageParserException) {
                                if (error.getInt(cause) == -103) {
                                    methodHookParam.setResult(newInstance);
                                }
                            }
                        }
                        if (parseErr != null && parseErr == -103) {
                            Object input = methodHookParam.args[0];
                            XposedHelpers.callMethod(input, "reset");
                            methodHookParam.setResult(XposedHelpers.callMethod(input, "success", newInstance));
                        }
                    }
                }
            }
        });

        // ============================================================================
        // V3 SIGNATURE HOOK (for V2/V3-only APKs)
        // ============================================================================
        // DIFF vs V1: Hooks verifyV3Signature instead of verifyV1Signature
        hookAllMethods("android.util.apk.ApkSignatureVerifier", loadPackageParam.classLoader, "verifyV3Signature",
                new XC_MethodHook() {
                    @Override
                    public void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                        if (!prefs.getBoolean("authcreak", false))
                            return;

                        // Step 1: Check if verification failed (either by exception or error code)
                        // SAME AS V1
                        Throwable throwable = methodHookParam.getThrowable();
                        Integer parseErr = null;

                        if (parseResult != null &&
                                ((Method) methodHookParam.method).getReturnType() == parseResult) {
                            Object result = methodHookParam.getResult();
                            if ((boolean) XposedHelpers.callMethod(result, "isError")) {
                                parseErr = (int) XposedHelpers.callMethod(result, "getErrorCode");
                            }
                        }

                        if (throwable == null && parseErr == null)
                            return; // No error, don't intervene

                        XposedBridge.log("I/" + MainHook.TAG + " V3 signature verification failed, attempting bypass");

                        // Step 2: Extract certificates
                        // DIFF vs V1: Uses ApkSignatureSchemeV3Verifier.unsafeGetCertsWithoutVerification()
                        //             instead of JAR-based loadCertificates() from META-INF/*.RSA
                        Signature[] signerSigs = null;
                        Signature[] pastSignerSigs = null; // DIFF vs V1: proof-of-rotation (V3-specific, not in V1)
                        Map<Integer, byte[]> contentDigests = null; // DIFF vs V1: content digests (V3-specific)

                        try {
                            String apkPath = (String) methodHookParam.args[parseErr == null ? 0 : 1];

                            // DIFF vs V1: V3 uses APK Signing Block, not JAR manifest
                            Class<?> v3Verifier = XposedHelpers.findClassIfExists(
                                    "android.util.apk.ApkSignatureSchemeV3Verifier",
                                    loadPackageParam.classLoader);

                            if (v3Verifier != null) {
                                // DIFF vs V1: unsafeGetCertsWithoutVerification extracts certs without verification
                                //             V1 uses StrictJarFile + loadCertificates from JAR entries
                                Object vSigner = XposedHelpers.callStaticMethod(v3Verifier,
                                        "unsafeGetCertsWithoutVerification", apkPath);

                                // Extract certificates from VerifiedSigner
                                Certificate[] certs = (Certificate[]) XposedHelpers.getObjectField(vSigner, "certs");
                                Certificate[][] signerCerts = new Certificate[][] { certs };
                                signerSigs = (Signature[]) XposedHelpers.callStaticMethod(ASV,
                                        "convertToSignatures", (Object) signerCerts);

                                // DIFF vs V1: Extract proof-of-rotation (V3-specific, doesn't exist in V1)
                                // POR allows key rotation while maintaining trust chain
                                Object por = XposedHelpers.getObjectField(vSigner, "por");
                                if (por != null) {
                                    @SuppressWarnings("unchecked")
                                    List<X509Certificate> porCerts = (List<X509Certificate>) XposedHelpers
                                            .getObjectField(por, "certs");

                                    if (porCerts != null && !porCerts.isEmpty()) {
                                        pastSignerSigs = new Signature[porCerts.size()];
                                        for (int i = 0; i < pastSignerSigs.length; i++) {
                                            X509Certificate cert = porCerts.get(i);
                                            pastSignerSigs[i] = new Signature(cert.getEncoded());
                                        }
                                    }
                                }

                                // DIFF vs V1: Extract content digests (V3-specific, not used in V1)
                                // Maps digest algorithm ID to digest bytes
                                @SuppressWarnings("unchecked")
                                Map<Integer, byte[]> digests = (Map<Integer, byte[]>) XposedHelpers
                                        .getObjectField(vSigner, "contentDigests");
                                contentDigests = digests;
                            }
                        } catch (Throwable e) {
                            XposedBridge
                                    .log("W/" + MainHook.TAG + " Failed to extract V3 certificates: " + e.getMessage());
                        }

                        // Fallback to hardcoded signature if extraction failed (SAME AS V1)
                        if (signerSigs == null) {
                            signerSigs = new Signature[] { new Signature(SIGNATURE) };
                        }

                        // Step 3: Build SigningDetails
                        // DIFF vs V1: scheme version is 3 (SIGNING_BLOCK_V3) instead of 1 (JAR)
                        // DIFF vs V1: May include pastSignerSigs for proof-of-rotation
                        Object newSigningDetails;
                        try {
                            if (pastSignerSigs != null) {
                                // DIFF vs V1: Use 3-arg constructor with past signatures for POR
                                // V1 only uses 2-arg constructor (no proof-of-rotation)
                                Constructor<?> detailsConstructor = XposedHelpers.findConstructorExact(
                                        signingDetails,
                                        Signature[].class,
                                        Integer.TYPE,
                                        Signature[].class);
                                detailsConstructor.setAccessible(true);
                                newSigningDetails = detailsConstructor.newInstance(signerSigs, SIGNATURE_SCHEME_VERSION__SIGNING_BLOCK_V3, pastSignerSigs);
                            } else {
                                // Use simpler constructor: SigningDetails(Signature[], int)
                                // DIFF vs V1: scheme=3 instead of scheme=1
                                newSigningDetails = findConstructorExact.newInstance(new Object[] { signerSigs, SIGNATURE_SCHEME_VERSION__SIGNING_BLOCK_V3 });
                            }
                        } catch (Throwable e) {
                            // Fallback to basic constructor
                            newSigningDetails = findConstructorExact.newInstance(new Object[] { signerSigs, SIGNATURE_SCHEME_VERSION__SIGNING_BLOCK_V3 });
                        }

                        // Step 4: Wrap in SigningDetailsWithDigests if needed
                        // DIFF vs V1: Passes contentDigests (V1 passes null)
                        Class<?> signingDetailsWithDigests = XposedHelpers.findClassIfExists(
                                "android.util.apk.ApkSignatureVerifier.SigningDetailsWithDigests",
                                loadPackageParam.classLoader);

                        Object finalResult;
                        if (signingDetailsWithDigests != null) {
                            Constructor<?> wrapperConstructor = XposedHelpers.findConstructorExact(
                                    signingDetailsWithDigests,
                                    signingDetails,
                                    Map.class);
                            wrapperConstructor.setAccessible(true);
                            // DIFF vs V1: Passes extracted contentDigests; V1 passes null
                            finalResult = wrapperConstructor.newInstance(newSigningDetails, contentDigests);
                        } else {
                            finalResult = newSigningDetails;
                        }

                        // Step 5: Replace the failed result (only for "no certificates" error)
                        // SAME AS V1
                        int ERROR_INSTALL_PARSE_FAILED_NO_CERTIFICATES = -103;

                        if (throwable != null) {
                            Throwable cause = throwable.getCause();
                            if (throwable.getClass() == packageParserException) {
                                if (error.getInt(throwable) == ERROR_INSTALL_PARSE_FAILED_NO_CERTIFICATES) {
                                    methodHookParam.setResult(finalResult);
                                    XposedBridge.log("I/" + MainHook.TAG + " V3 signature bypass successful (exception)");
                                }
                            }
                            if (cause != null && cause.getClass() == packageParserException) {
                                if (error.getInt(cause) == ERROR_INSTALL_PARSE_FAILED_NO_CERTIFICATES) {
                                    methodHookParam.setResult(finalResult);
                                    XposedBridge.log("I/" + MainHook.TAG + " V3 signature bypass successful (cause)");
                                }
                            }
                        }
                        if (parseErr != null && parseErr == ERROR_INSTALL_PARSE_FAILED_NO_CERTIFICATES) {
                            Object input = methodHookParam.args[0];
                            XposedHelpers.callMethod(input, "reset");
                            methodHookParam.setResult(XposedHelpers.callMethod(input, "success", finalResult));
                            XposedBridge.log("I/" + MainHook.TAG + " V3 signature bypass successful (parseErr)");
                        }
                    }
                });

        //New package has a different signature
        //处理覆盖安装但签名不一致
        hookAllMethods(signingDetails, "checkCapability", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                // Don't handle PERMISSION & AUTH
                // Or applications will have all privileged permissions
                // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/content/pm/PackageParser.java;l=5947?q=CertCapabilities
                // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/services/core/java/com/android/server/accounts/AccountManagerService.java;l=5867
                if ((Integer) param.args[1] != 4 && (Integer) param.args[1] != 16 && prefs.getBoolean("digestCreak", true)) {
                    param.setResult(true);
                }
            }
        });
        // if app is system app, allow to use hidden api, even if app not using a system signature
        findAndHookMethod("android.content.pm.ApplicationInfo", loadPackageParam.classLoader, "isPackageWhitelistedForHiddenApis", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (prefs.getBoolean("digestCreak", true)) {
                    ApplicationInfo info = (ApplicationInfo) param.thisObject;
                    if ((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                            || (info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                        param.setResult(true);
                    }
                }
            }
        });

        var keySetManagerClass = findClass("com.android.server.pm.KeySetManagerService", loadPackageParam.classLoader);
        if (keySetManagerClass != null) {
            var shouldBypass = new ThreadLocal<Boolean>();
            hookAllMethods(keySetManagerClass, "shouldCheckUpgradeKeySetLocked", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    // 检查权限定义的签名的时候，如果定义包名相同，会使用 KeySetManagerService ，
                    // 我们利用这一点让它通过检查，也就是同包不同签名权限可覆盖
                    // R-Sv2: PackageManagerService#preparePackageLI
                    // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r21:frameworks/base/services/core/java/com/android/server/pm/PackageManagerService.java;l=17188;drc=960ffca13a519b0fb9e0942665577c62f97d0eea
                    // T-V: InstallPackageHelper#preparePackageLI
                    // https://cs.android.com/android/platform/superproject/+/android-14.0.0_r2:frameworks/base/services/core/java/com/android/server/pm/InstallPackageHelper.java;l=1097;drc=5ea7e53c3a787e25af86b0f31933ddd68ae3514e
                    // 16: InstallPackageHelper#preparePackage
                    // https://cs.android.com/android/platform/superproject/+/android-16.0.0_r2:frameworks/base/services/core/java/com/android/server/pm/InstallPackageHelper.java;l=1459;drc=d14620262929e39a409b55d11cb542c1d1c4d2f6
                    if (prefs.getBoolean("digestCreak", true) &&
                            Arrays.stream(Thread.currentThread().getStackTrace())
                                    .anyMatch((o) -> o.getMethodName().startsWith("preparePackage")
                                    )
                    ) {
                        shouldBypass.set(true);
                        param.setResult(true);
                    } else {
                        shouldBypass.set(false);
                    }
                }
            });
            hookAllMethods(keySetManagerClass, "checkUpgradeKeySetLocked", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (prefs.getBoolean("digestCreak", true) && shouldBypass.get()) {
                        param.setResult(true);
                    }
                }
            });
        }

        // for SharedUser
        // "Package " + packageName + " has a signing lineage " + "that diverges from the lineage of the sharedUserId"
        // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r21:frameworks/base/services/core/java/com/android/server/pm/PackageManagerServiceUtils.java;l=728;drc=02a58171a9d41ad0048d6a1a48d79dee585c22a5
        hookAllMethods(signingDetails, "hasCommonAncestor", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (prefs.getBoolean("digestCreak", true)
                        && prefs.getBoolean("sharedUser", false)
                        // because of LSPosed's bug, we can't hook verifySignatures while deoptimize it
                        && Arrays.stream(Thread.currentThread().getStackTrace()).anyMatch((o) -> "verifySignatures".equals(o.getMethodName()))
                )
                    param.setResult(true);
            }
        });

        var utilClass = findClass("com.android.server.pm.PackageManagerServiceUtils", loadPackageParam.classLoader);
        if (utilClass != null) {
            try {
                deoptimizeMethod(utilClass, "verifySignatures");
            } catch (Throwable e) {
                XposedBridge.log("E/" + MainHook.TAG + " deoptimizing failed" + Log.getStackTraceString(e));
            }
        }

        // choose a signature after all old signed packages are removed
        var sharedUserSettingClass = XposedHelpers.findClass("com.android.server.pm.SharedUserSetting", loadPackageParam.classLoader);
        XposedBridge.hookAllMethods(
                sharedUserSettingClass,
                "removePackage",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!prefs.getBoolean("digestCreak", true) || !prefs.getBoolean("sharedUser", false))
                            return;
                        var flags = (int) XposedHelpers.getObjectField(param.thisObject, "uidFlags");
                        if ((flags & ApplicationInfo.FLAG_SYSTEM) != 0)
                            return; // do not modify system's signature
                        var toRemove = param.args[0]; // PackageSetting
                        if (toRemove == null) return;
                        var removed = false; // Is toRemove really needed to be removed
                        var sharedUserSig = Setting_getSigningDetails(param.thisObject);
                        Object newSig = null;
                        var packages = /*Watchable?ArraySet<PackageSetting>*/ SharedUserSetting_packages(param.thisObject);
                        var size = (int) XposedHelpers.callMethod(packages, "size");
                        for (var i = 0; i < size; i++) {
                            var p = XposedHelpers.callMethod(packages, "valueAt", i);
                            // skip the removed package
                            if (toRemove.equals(p)) {
                                removed = true;
                                continue;
                            }
                            var packageSig = Setting_getSigningDetails(p);
                            // if old signing exists, return
                            if ((boolean) callOriginMethod(packageSig, "checkCapability", sharedUserSig, 0) || (boolean) callOriginMethod(sharedUserSig, "checkCapability", packageSig, 0)) {
                                return;
                            }
                            // otherwise, choose the first signature we meet, and merge with others if possible
                            // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/services/core/java/com/android/server/pm/ReconcilePackageUtils.java;l=193;drc=c9a8baf585e8eb0f3272443930301a61331b65c1
                            // respect to system
                            if (newSig == null) newSig = packageSig;
                            else newSig = SigningDetails_mergeLineageWith(newSig, packageSig);
                        }
                        if (!removed || newSig == null) return;
                        XposedBridge.log("updating signature in sharedUser during remove: " + param.thisObject);
                        Setting_setSigningDetails(param.thisObject, newSig);
                    }
                }
        );

        XposedBridge.hookAllMethods(
                sharedUserSettingClass,
                "addPackage",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!prefs.getBoolean("digestCreak", true) || !prefs.getBoolean("sharedUser", false))
                            return;
                        var flags = (int) XposedHelpers.getObjectField(param.thisObject, "uidFlags");
                        if ((flags & ApplicationInfo.FLAG_SYSTEM) != 0)
                            return; // do not modify system's signature
                        var toAdd = param.args[0]; // PackageSetting
                        if (toAdd == null) return;
                        var added = false;
                        var sharedUserSig = Setting_getSigningDetails(param.thisObject);
                        Object newSig = null;
                        var packages = /*Watchable?ArraySet<PackageSetting>*/ SharedUserSetting_packages(param.thisObject);
                        var size = (int) XposedHelpers.callMethod(packages, "size");
                        for (var i = 0; i < size; i++) {
                            var p = XposedHelpers.callMethod(packages, "valueAt", i);
                            if (toAdd.equals(p)) {
                                // must be an existing package
                                added = true;
                                p = toAdd;
                            }
                            var packageSig = Setting_getSigningDetails(p);
                            // if old signing exists, return
                            if ((boolean) callOriginMethod(packageSig, "checkCapability", sharedUserSig, 0) || (boolean) callOriginMethod(sharedUserSig, "checkCapability", packageSig, 0)) {
                                return;
                            }
                            // otherwise, choose the first signature we meet, and merge with others if possible
                            // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/services/core/java/com/android/server/pm/ReconcilePackageUtils.java;l=193;drc=c9a8baf585e8eb0f3272443930301a61331b65c1
                            // respect to system
                            if (newSig == null) newSig = packageSig;
                            else newSig = SigningDetails_mergeLineageWith(newSig, packageSig);
                        }
                        if (!added || newSig == null) return;
                        XposedBridge.log("CorePatch: updating signature in sharedUser during add " + toAdd + ": " + param.thisObject);
                        Setting_setSigningDetails(param.thisObject, newSig);
                    }
                }
        );

        hookAllMethods(getIsVerificationEnabledClass(loadPackageParam.classLoader), "isVerificationEnabled", new ReturnConstant(prefs, "disableVerificationAgent", false));

        // Allow apk splits with different signatures to be installed together
        hookAllMethods(signingDetails, "signaturesMatchExactly", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (prefs.getBoolean("exactSigCheck", false))
                    param.setResult(true);
            }
        });

        if (BuildConfig.DEBUG) initializeDebugHook(loadPackageParam);
    }

    static Object callOriginMethod(Object obj, String methodName, Object... args) {
        try {
            var method = XposedHelpers.findMethodBestMatch(obj.getClass(), methodName, args);
            return XposedBridge.invokeOriginalMethod(method, obj, args);
        } catch (IllegalAccessException e) {
            // should not happen
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    Class<?> getIsVerificationEnabledClass(ClassLoader classLoader) {
        return XposedHelpers.findClass("com.android.server.pm.PackageManagerService", classLoader);
    }

    Class<?> getSigningDetails(ClassLoader classLoader) {
        return XposedHelpers.findClass("android.content.pm.PackageParser.SigningDetails", classLoader);
    }

    Object mPMS = null;

    void initializeDebugHook(XC_LoadPackage.LoadPackageParam lpparam) throws IllegalAccessException, InvocationTargetException {
        XposedBridge.hookAllMethods(
                XposedHelpers.findClass("com.android.server.pm.PackageManagerShellCommand", lpparam.classLoader),
                "onCommand",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            var pms = mPMS;
                            if (pms == null) return;
                            var cmd = (String) param.args[0];
                            if (!"corepatch".equals(cmd)) return;
                            var self = param.thisObject;
                            var pw = (PrintWriter) XposedHelpers.callMethod(self, "getOutPrintWriter");
                            var type = (String) XposedHelpers.callMethod(self, "getNextArgRequired");
                            var settings = XposedHelpers.getObjectField(pms, "mSettings");
                            if ("p".equals(type) || "package".equals(type)) {
                                var packageName = (String) XposedHelpers.callMethod(self, "getNextArgRequired");
                                var packageSetting = XposedHelpers.callMethod(settings, "getPackageLPr", packageName);
                                if (packageSetting != null) {
                                    dumpPackageSetting(packageSetting, pw, settings);
                                } else {
                                    pw.println("no package " + packageName + " found");
                                }
                            } else if ("su".equals(type) || "shareduser".equals(type)) {
                                var name = (String) XposedHelpers.callMethod(self, "getNextArgRequired");
                                var su = getSharedUser(name, settings);
                                if (su != null) {
                                    dumpSharedUserSetting(su, pw);
                                } else {
                                    pw.println("no shared user " + name + " found");
                                }
                            } else {
                                pw.println("usage: <p|package|su|shareduser> <name>");
                            }
                            param.setResult(0);
                        } catch (Throwable t) {
                            XposedBridge.log(t);
                            param.setThrowable(t);
                        }
                    }
                }
        );

        var pmsClass = XposedHelpers.findClassIfExists("com.android.server.pm.PackageManagerService",
                lpparam.classLoader);

        XposedBridge.hookAllConstructors(pmsClass, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mPMS = param.thisObject;
                    }
                }
        );

        deoptimizeMethod(pmsClass, "onShellCommand");
    }

    void dumpPackageSetting(Object packageSetting, PrintWriter pw, Object /*Settings*/ settings) {
        var signingDetails = Setting_getSigningDetails(packageSetting);
        pw.println("signing for package " + packageSetting);
        dumpSigningDetails(signingDetails, pw);
        var pkg = XposedHelpers.getObjectField(packageSetting, "pkg"); // AndroidPackage
        if (pkg == null) {
            pw.println("android package is null!");
            return;
        }
        var id = (String) XposedHelpers.callMethod(pkg, "getSharedUserId");
        pw.println("shared user id:" + id);
        if (settings != null) {
            var su = getSharedUser(id, settings);
            if (su != null) {
                dumpSharedUserSetting(su, pw);
            }
        }
    }

    Object getSharedUser(String id, Object /*Settings*/ settings) {
        // TODO: use Setting.getSharedUserSettingLPr(appId)?
        var sharedUserSettings = XposedHelpers.getObjectField(settings, "mSharedUsers");
        if (sharedUserSettings == null) return null;
        return XposedHelpers.callMethod(sharedUserSettings, "get", id);
    }

    void dumpSharedUserSetting(Object sharedUser, PrintWriter pw) {
        var signingDetails = Setting_getSigningDetails(sharedUser);
        pw.println("signing for shared user " + sharedUser);
        dumpSigningDetails(signingDetails, pw);
    }

    protected void dumpSigningDetails(Object signingDetails, PrintWriter pw) {
        var i = 0;
        for (var sign : (Signature[]) XposedHelpers.getObjectField(signingDetails, "signatures")) {
            i++;
            pw.println(i + ": " + sign.toCharsString());
        }
    }

    /**
     * Get signing details for PackageSetting or SharedUserSetting
     */
    Object Setting_getSigningDetails(Object pkgOrSharedUser) {
        // PackageSettingBase(A11)|PackageSetting(A13)|SharedUserSetting.<PackageSignatures>signatures.<PackageParser.SigningDetails>mSigningDetails
        return XposedHelpers.getObjectField(XposedHelpers.getObjectField(pkgOrSharedUser, "signatures"), "mSigningDetails");
    }

    /**
     * Set signing details for PackageSetting or SharedUserSetting
     */
    void Setting_setSigningDetails(Object pkgOrSharedUser, Object signingDetails) {
        XposedHelpers.setObjectField(XposedHelpers.getObjectField(pkgOrSharedUser, "signatures"), "mSigningDetails", signingDetails);
    }

    protected Object SharedUserSetting_packages(Object /*SharedUserSetting*/ sharedUser) {
        return XposedHelpers.getObjectField(sharedUser, "packages");
    }

    protected Object SigningDetails_mergeLineageWith(Object self, Object other) {
        return XposedHelpers.callMethod(self, "mergeLineageWith", other);
    }
}
