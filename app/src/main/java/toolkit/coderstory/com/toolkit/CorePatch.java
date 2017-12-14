package toolkit.coderstory.com.toolkit;


import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Base64;

import java.lang.reflect.Field;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;


public class CorePatch extends XposedHelper implements IXposedHookZygoteInit, IXposedHookLoadPackage {

    public void initZygote(IXposedHookZygoteInit.StartupParam paramStartupParam) {

        XposedHelpers.findAndHookMethod("java.security.MessageDigest", null, "isEqual", byte[].class, byte[].class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam methodHookParam)
                    throws Throwable {
                prefs.reload();
                if (prefs.getBoolean("authcreak", true)) {
                    methodHookParam.setResult(true);
                }
            }
        });

        XposedBridge.hookAllMethods(XposedHelpers.findClass("com.android.org.conscrypt.OpenSSLSignature", null), "engineVerify", new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam paramAnonymousMethodHookParam)
                    throws Throwable {
                prefs.reload();
                if (prefs.getBoolean("authcreak", true)) {
                    paramAnonymousMethodHookParam.setResult(true);
                }
            }
        });

        final Class ApkSignatureSchemeV2Verifier = XposedHelpers.findClass("android.util.apk.ApkSignatureSchemeV2Verifier", null);
        final Class packageParser = XposedHelpers.findClass("android.content.pm.PackageParser", null);
        final Class strictJarVerifier = XposedHelpers.findClass("android.util.jar.StrictJarVerifier", null);
        final Class packageClass = XposedHelpers.findClass("android.content.pm.PackageParser.Package", null);


        XposedBridge.hookAllMethods(packageParser, "getApkSigningVersion", XC_MethodReplacement.returnConstant(1));

        XposedBridge.hookAllConstructors(strictJarVerifier, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                prefs.reload();
                if (prefs.getBoolean("authcreak", true)) {
                    param.args[3] = false;
                }
            }
        });

        XposedBridge.hookAllConstructors(ApkSignatureSchemeV2Verifier, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object packageInfoLite = param.thisObject;
                prefs.reload();
                if (prefs.getBoolean("authcreak", true)) {
                    Field field = packageClass.getField(" SF_ATTRIBUTE_ANDROID_APK_SIGNED_ID");
                    field.setAccessible(true);
                    field.set(packageInfoLite, -1);
                }
            }
        });
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam paramLoadPackageParam) {


        if (("android".equals(paramLoadPackageParam.packageName)) && (paramLoadPackageParam.processName.equals("android"))) {

            final Class localClass = XposedHelpers.findClass("com.android.server.pm.PackageManagerService", paramLoadPackageParam.classLoader);
            final Class packageClass = XposedHelpers.findClass("android.content.pm.PackageParser.Package", paramLoadPackageParam.classLoader);

            XposedBridge.hookAllMethods(localClass, "checkDowngrade", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    super.beforeHookedMethod(methodHookParam);
                    Object packageInfoLite = methodHookParam.args[0];
                    prefs.reload();
                    if (prefs.getBoolean("downgrade", true)) {
                        Field field = packageClass.getField("mVersionCode");
                        field.setAccessible(true);
                        field.set(packageInfoLite, -1);
                    }
                }
            });

            XposedBridge.hookAllMethods(localClass, "verifySignaturesLP", new XC_MethodHook() {

                protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    prefs.reload();
                    if (prefs.getBoolean("authcreak", true)) {
                        methodHookParam.setResult(true);
                    }
                }
            });

            XposedBridge.hookAllMethods(localClass, "compareSignatures", new XC_MethodHook() {
                protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    prefs.reload();
                    if (prefs.getBoolean("zipauthcreak", false) ) {

                        String platform = prefs.getString("platform","DEFAULT");

                        if (platform.equals("DEFAULT")){
                            XposedBridge.log("警告:核心破解上未初始化,请至少打开一次APP!");
                        }

                        Signature[] signatures = (Signature[]) methodHookParam.args[0];
                        if (signatures != null && signatures.length > 0) {
                            for (Signature signature : signatures) {
                                if (new String(Base64.encode(signature.toByteArray(), Base64.DEFAULT)).replaceAll("\n", "").equals(platform)) {
                                    return;
                                }
                            }
                        }

                        signatures = (Signature[]) methodHookParam.args[1];
                        if (signatures != null && signatures.length > 0) {
                            for (Signature signature : signatures) {
                                if (new String(Base64.encode(signature.toByteArray(), Base64.DEFAULT)).replaceAll("\n", "").equals(platform)) {
                                    return;
                                }
                            }
                        }

                        methodHookParam.setResult(0);
                    }
                }
            });

            XposedBridge.hookAllMethods(localClass, "compareSignaturesCompat", new XC_MethodHook() {
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam paramAnonymousMethodHookParam) {
                    prefs.reload();
                    if (prefs.getBoolean("authcreak", true)) {
                        paramAnonymousMethodHookParam.setResult(0);
                    }
                }
            });
            XposedBridge.hookAllMethods(localClass, "compareSignaturesRecover", new XC_MethodHook() {
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam paramAnonymousMethodHookParam) {
                    prefs.reload();
                    if (prefs.getBoolean("authcreak", true)) {
                        paramAnonymousMethodHookParam.setResult(0);
                    }
                }
            });
        }
    }
}
