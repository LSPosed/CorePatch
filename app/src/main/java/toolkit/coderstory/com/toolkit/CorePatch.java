package toolkit.coderstory.com.toolkit;


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

    }


    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam paramLoadPackageParam) {

        if (("android".equals(paramLoadPackageParam.packageName)) && (paramLoadPackageParam.processName.equals("android"))) {

            final  Class localClass = XposedHelpers.findClass("com.android.server.pm.PackageManagerService", paramLoadPackageParam.classLoader);
            final Class packageClass = XposedHelpers.findClass("android.content.pm.PackageParser.Package", paramLoadPackageParam.classLoader);
            final Class packageParser = XposedHelpers.findClass("android.content.pm.PackageParser", paramLoadPackageParam.classLoader);

            XposedBridge.hookAllMethods(localClass, "checkDowngrade", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    super.beforeHookedMethod(methodHookParam);
                    Object packageInfoLite = methodHookParam.args[0];
                    prefs.reload();
                    if (prefs.getBoolean("downgrade", true)) {
                        Field field = packageClass.getField("mVersionCode");
                        field.set(packageInfoLite, 0);
                    }
                    ;
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
                    if (prefs.getBoolean("zipauthcreak", true)) {
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

            XposedBridge.hookAllMethods(packageParser, "getApkSigningVersion",  XC_MethodReplacement.returnConstant(1));
        }
    }
}
