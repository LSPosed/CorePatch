package toolkit.coderstory;


import android.app.AndroidAppHelper;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Log;

import com.coderstory.toolkit.BuildConfig;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class CorePatchForR extends XposedHelper implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    XSharedPreferences prefs = new XSharedPreferences(BuildConfig.APPLICATION_ID, "conf");

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws IllegalAccessException, InvocationTargetException, InstantiationException {

        Log.d(MainHook.TAG, "downgrade" + prefs.getBoolean("downgrade->", true));
        Log.d(MainHook.TAG, "authcreak" + prefs.getBoolean("authcreak->", true));
        Log.d(MainHook.TAG, "digestCreak" + prefs.getBoolean("digestCreak->", true));
        Log.d(MainHook.TAG, "UsePreSig" + prefs.getBoolean("UsePreSig->", false));

        // 允许降级
        findAndHookMethod("com.android.server.pm.PackageManagerService", loadPackageParam.classLoader,
                "checkDowngrade",
                "com.android.server.pm.parsing.pkg.AndroidPackage",
                "android.content.pm.PackageInfoLite",
                new ReturnConstant(prefs, "downgrade", null));

        // exists on flyme 9(Android 11) only
        findAndHookMethod("com.android.server.pm.PackageManagerService", loadPackageParam.classLoader,
                "checkDowngrade",
                "android.content.pm.PackageInfoLite",
                "android.content.pm.PackageInfoLite",
                new ReturnConstant(prefs, "downgrade", true));


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

        // No signature found in package of version " + minSignatureSchemeVersion
        // + " or newer for package " + apkPath
        findAndHookMethod("android.util.apk.ApkSignatureVerifier", loadPackageParam.classLoader, "getMinimumSignatureSchemeVersionForTargetSdk", int.class,
                new ReturnConstant(prefs, "authcreak", 0));
        findAndHookMethod("com.android.apksig.ApkVerifier", loadPackageParam.classLoader, "getMinimumSignatureSchemeVersionForTargetSdk", int.class,
                new ReturnConstant(prefs, "authcreak", 0));

        // Package " + packageName + " signatures do not match previously installed version; ignoring!"
        // public boolean checkCapability(String sha256String, @CertCapabilities int flags) {
        // public boolean checkCapability(SigningDetails oldDetails, @CertCapabilities int flags)
        hookAllMethods("android.content.pm.PackageParser", loadPackageParam.classLoader, "checkCapability", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                // Don't handle PERMISSION (grant SIGNATURE permissions to pkgs with this cert)
                // Or applications will have all privileged permissions
                // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/content/pm/PackageParser.java;l=5947?q=CertCapabilities
                if (prefs.getBoolean("authcreak", true)) {
                    if ((Integer) param.args[1] != 4) {
                        param.setResult(true);
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
        Class<?> signingDetails = XposedHelpers.findClass("android.content.pm.PackageParser.SigningDetails", loadPackageParam.classLoader);
        Constructor<?> findConstructorExact = XposedHelpers.findConstructorExact(signingDetails, Signature[].class, Integer.TYPE);
        findConstructorExact.setAccessible(true);
        Class<?> packageParserException = XposedHelpers.findClass("android.content.pm.PackageParser.PackageParserException", loadPackageParam.classLoader);
        Field error = XposedHelpers.findField(packageParserException, "error");
        error.setAccessible(true);
        Object[] signingDetailsArgs = new Object[2];
        signingDetailsArgs[1] = 1;
        hookAllMethods("android.util.jar.StrictJarVerifier", loadPackageParam.classLoader, "verifyBytes", new XC_MethodHook() {
            public void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if (prefs.getBoolean("digestCreak", true)) {
                    if (!prefs.getBoolean("UsePreSig", false)) {
                        final Object block = constructor.newInstance(param.args[0]);
                        Object[] infos = (Object[]) XposedHelpers.callMethod(block, "getSignerInfos");
                        Object info = infos[0];
                        List<X509Certificate> verifiedSignerCertChain = (List<X509Certificate>) XposedHelpers.callMethod(info, "getCertificateChain", block);
                        param.setResult(verifiedSignerCertChain.toArray(
                                new X509Certificate[0]));
                    }
                }
            }
        });
        hookAllMethods("android.util.apk.ApkSignatureVerifier", loadPackageParam.classLoader, "verifyV1Signature", new XC_MethodHook() {
            public void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                super.afterHookedMethod(methodHookParam);
                if (prefs.getBoolean("authcreak", true)) {
                    Throwable throwable = methodHookParam.getThrowable();
                    if (throwable != null) {
                        Signature[] lastSigs = null;
                        if (prefs.getBoolean("UsePreSig", false)) {
                            PackageManager PM = AndroidAppHelper.currentApplication().getPackageManager();
                            if (PM == null) {
                                XposedBridge.log("E: " + BuildConfig.APPLICATION_ID + " Cannot get the Package Manager... Are you using MiUI?");
                            } else {
                                PackageInfo pI = PM.getPackageArchiveInfo((String) methodHookParam.args[0], 0);
                                PackageInfo InstpI = PM.getPackageInfo(pI.packageName, PackageManager.GET_SIGNATURES);
                                lastSigs = InstpI.signatures;
                            }
                        } else {
                            if (prefs.getBoolean("digestCreak", true)) {
                                final Object origJarFile = constructorExact.newInstance(methodHookParam.args[0], true, false);
                                final ZipEntry manifestEntry = (ZipEntry) XposedHelpers.callMethod(origJarFile, "findEntry", "AndroidManifest.xml");
                                final Certificate[][] lastCerts = (Certificate[][]) XposedHelpers.callStaticMethod(ASV, "loadCertificates", origJarFile, manifestEntry);
                                lastSigs = (Signature[]) XposedHelpers.callStaticMethod(ASV, "convertToSignatures", (Object) lastCerts);
                            }
                        }
                        if (lastSigs != null) {
                            signingDetailsArgs[0] = lastSigs;
                        } else {
                            signingDetailsArgs[0] = new Signature[]{new Signature(SIGNATURE)};
                        }
                        Object newInstance = findConstructorExact.newInstance(signingDetailsArgs);

                        //修复 java.lang.ClassCastException: Cannot cast android.content.pm.PackageParser$SigningDetails to android.util.apk.ApkSignatureVerifier$SigningDetailsWithDigests
                        Class<?> signingDetailsWithDigests = XposedHelpers.findClassIfExists("android.util.apk.ApkSignatureVerifier.SigningDetailsWithDigests", loadPackageParam.classLoader);
                        if (signingDetailsWithDigests != null) {
                            Constructor<?> signingDetailsWithDigestsConstructorExact = XposedHelpers.findConstructorExact(signingDetailsWithDigests, signingDetails, Map.class);
                            signingDetailsWithDigestsConstructorExact.setAccessible(true);
                            newInstance = signingDetailsWithDigestsConstructorExact.newInstance(new Object[]{newInstance, null});
                        }

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
                }
            }
        });


        //New package has a different signature
        //处理覆盖安装但签名不一致
        hookAllMethods(signingDetails, "checkCapability", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                // Don't handle PERMISSION (grant SIGNATURE permissions to pkgs with this cert)
                // Or applications will have all privileged permissions
                // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/content/pm/PackageParser.java;l=5947?q=CertCapabilities
                if (((Integer) param.args[1] != 4) && prefs.getBoolean("digestCreak", true)) {
                    param.setResult(true);
                }
            }
        });
        // if app is system app, allow to use hidden api, even if app not using a system signature
        findAndHookMethod("android.content.pm.ApplicationInfo", loadPackageParam.classLoader, "isPackageWhitelistedForHiddenApis", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                if (prefs.getBoolean("digestCreak", true)) {
                    ApplicationInfo info = (ApplicationInfo) param.thisObject;
                    if ((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                            || (info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                        param.setResult(true);
                    }
                }
            }
        });
    }

    @Override
    public void initZygote(StartupParam startupParam) {

        hookAllMethods("android.content.pm.PackageParser", null, "getApkSigningVersion", XC_MethodReplacement.returnConstant(1));
        hookAllConstructors("android.util.jar.StrictJarVerifier", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (prefs.getBoolean("enhancedMode", false)) {
                    super.beforeHookedMethod(param);
                    param.args[3] = Boolean.FALSE;
                }
            }
        });
    }
}
