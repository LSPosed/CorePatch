package toolkit.coderstory;


import android.content.pm.ApplicationInfo;
import android.content.pm.Signature;
import android.util.Log;

import com.coderstory.toolkit.BuildConfig;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.zip.ZipEntry;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;


public class CorePatchForR extends XposedHelper implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    XSharedPreferences prefs = new XSharedPreferences(BuildConfig.APPLICATION_ID, "conf");

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws IllegalAccessException, InvocationTargetException, InstantiationException {

        Log.d(MainHook.TAG, "downgrade" + prefs.getBoolean("downgrade->", true));
        Log.d(MainHook.TAG, "authcreak" + prefs.getBoolean("authcreak->", true));
        Log.d(MainHook.TAG, "digestCreak" + prefs.getBoolean("digestCreak->", true));

        // 允许降级
        if (prefs.getBoolean("downgrade", true)) {
            findAndHookMethod("com.android.server.pm.PackageManagerService", loadPackageParam.classLoader,
                    "checkDowngrade",
                    "com.android.server.pm.parsing.pkg.AndroidPackage",
                    "android.content.pm.PackageInfoLite",
                    XC_MethodReplacement.returnConstant(null));
            // exists on flyme 9(Android 11) only
            findAndHookMethod("com.android.server.pm.PackageManagerService", loadPackageParam.classLoader,
                    "checkDowngrade",
                    "android.content.pm.PackageInfoLite",
                    "android.content.pm.PackageInfoLite",
                    XC_MethodReplacement.returnConstant(1));
        }
        if (prefs.getBoolean("authcreak", true)) {
            // apk内文件修改后 digest校验会失败
            hookAllMethods("android.util.jar.StrictJarVerifier", loadPackageParam.classLoader, "verifyMessageDigest", XC_MethodReplacement.returnConstant(true));
            hookAllMethods("android.util.jar.StrictJarVerifier", loadPackageParam.classLoader, "verify", XC_MethodReplacement.returnConstant(true));
            hookAllMethods("java.security.MessageDigest", loadPackageParam.classLoader, "isEqual", XC_MethodReplacement.returnConstant(true));

            // Targeting R+ (version " + Build.VERSION_CODES.R + " and above) requires"
            // + " the resources.arsc of installed APKs to be stored uncompressed"
            // + " and aligned on a 4-byte boundary
            // target >=30 的情况下 resources.arsc 必须是未压缩的且4K对齐
            hookAllMethods("android.content.res.AssetManager", loadPackageParam.classLoader, "containsAllocatedTable", XC_MethodReplacement.returnConstant(false));

            // No signature found in package of version " + minSignatureSchemeVersion
            // + " or newer for package " + apkPath
            findAndHookMethod("android.util.apk.ApkSignatureVerifier", loadPackageParam.classLoader, "getMinimumSignatureSchemeVersionForTargetSdk", int.class, XC_MethodReplacement.returnConstant(0));
            findAndHookMethod("com.android.apksig.ApkVerifier", loadPackageParam.classLoader, "getMinimumSignatureSchemeVersionForTargetSdk", int.class, XC_MethodReplacement.returnConstant(0));

            // Package " + packageName + " signatures do not match previously installed version; ignoring!"
            // public boolean checkCapability(String sha256String, @CertCapabilities int flags) {
            // public boolean checkCapability(SigningDetails oldDetails, @CertCapabilities int flags)
            hookAllMethods("android.content.pm.PackageParser", loadPackageParam.classLoader, "checkCapability", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    // Don't handle PERMISSION (grant SIGNATURE permissions to pkgs with this cert)
                    // Or applications will have all privileged permissions
                    // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/content/pm/PackageParser.java;l=5947?q=CertCapabilities
                    if ((Integer) param.args[1] != 4) {
                        param.setResult(true);
                    }
                }
            });

            // 当verifyV1Signature抛出转换异常时，替换一个签名作为返回值
            // 如果用户已安装apk，并且其定义了私有权限，则安装时会因签名与模块内硬编码的不一致而被拒绝。尝试从待安装apk中获取签名。如果其中apk的签名和已安装的一致（只动了内容）就没有问题。此策略可能有潜在的安全隐患。
            Class<?> pkc = XposedHelpers.findClass("sun.security.pkcs.PKCS7",loadPackageParam.classLoader);
            Constructor<?> pkcc = XposedHelpers.findConstructorExact(pkc,byte[].class);
            pkcc.setAccessible(true);
            Class<?> ASV = XposedHelpers.findClass("android.util.apk.ApkSignatureVerifier", loadPackageParam.classLoader);
            Class<?> Sjarclass = XposedHelpers.findClass("android.util.jar.StrictJarFile", loadPackageParam.classLoader);
            Constructor<?> Sjarfile = XposedHelpers.findConstructorExact(Sjarclass, String.class, boolean.class,boolean.class);
            Sjarfile.setAccessible(true);
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
                    final Object block =  pkcc.newInstance(param.args[0]);
                    Object[] infos = (Object[]) XposedHelpers.callMethod(block,"getSignerInfos");
                    Object info = infos[0];
                    List<X509Certificate> verifiedSignerCertChain = (List<X509Certificate>) XposedHelpers.callMethod(info,"getCertificateChain",block);
                    param.setResult(verifiedSignerCertChain.toArray(
                            new X509Certificate[verifiedSignerCertChain.size()]));
                }
            });
            hookAllMethods("android.util.apk.ApkSignatureVerifier", loadPackageParam.classLoader, "verifyV1Signature", new XC_MethodHook() {
                public void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    super.afterHookedMethod(methodHookParam);
                    if (prefs.getBoolean("digestCreak", true)) {
                        Throwable throwable = methodHookParam.getThrowable();
                        if (throwable != null) {
                            final Object origJarFile = Sjarfile.newInstance(methodHookParam.args[0],true,false);
                            final ZipEntry manifestEntry = (ZipEntry) XposedHelpers.callMethod(origJarFile,"findEntry","AndroidManifest.xml");
                            final Certificate[][] lastCerts = (Certificate[][]) XposedHelpers.callStaticMethod(ASV,"loadCertificates",origJarFile,manifestEntry);
                            final Signature[] lastSigs = (Signature[]) XposedHelpers.callStaticMethod(ASV,"convertToSignatures", (Object) lastCerts);
                            if(lastSigs != null){
                                signingDetailsArgs[0] = lastSigs;
                            }else {
                                signingDetailsArgs[0] = new Signature[]{new Signature(SIGNATURE)};
                            }
                            final Object newInstance = findConstructorExact.newInstance(signingDetailsArgs);
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
        }
        if (prefs.getBoolean("digestCreak", true)) {
            //New package has a different signature
            //处理覆盖安装但签名不一致
            Class<?> signingDetails = XposedHelpers.findClass("android.content.pm.PackageParser.SigningDetails", loadPackageParam.classLoader);
            hookAllMethods(signingDetails, "checkCapability", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    // Don't handle PERMISSION (grant SIGNATURE permissions to pkgs with this cert)
                    // Or applications will have all privileged permissions
                    // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/content/pm/PackageParser.java;l=5947?q=CertCapabilities
                    if ((Integer) param.args[1] != 4) {
                        param.setResult(true);
                    }
                }
            });

            // if app is system app, allow to use hidden api, even if app not using a system signature
            findAndHookMethod("android.content.pm.ApplicationInfo", loadPackageParam.classLoader, "isPackageWhitelistedForHiddenApis", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    ApplicationInfo info = (ApplicationInfo) param.thisObject;
                    if ((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                            || (info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                        param.setResult(true);
                    }
                }
            });
        }
    }

    @Override
    public void initZygote(StartupParam startupParam) {
        if (prefs.getBoolean("enhancedMode", false)) {
            hookAllMethods("android.content.pm.PackageParser", null, "getApkSigningVersion", XC_MethodReplacement.returnConstant(1));
            hookAllConstructors("android.util.jar.StrictJarVerifier", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    param.args[3] = Boolean.FALSE;
                }
            });
        }
    }
}
