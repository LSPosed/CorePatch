package toolkit.coderstory;


import android.content.pm.Signature;

import com.coderstory.toolkit.BuildConfig;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class CorePatchForQ extends XposedHelper implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    XSharedPreferences prefs = new XSharedPreferences(BuildConfig.APPLICATION_ID, "conf");

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // 允许降级
        if (prefs.getBoolean("downgrade", true)) {
            Class<?> packageClazz = XposedHelpers.findClass("android.content.pm.PackageParser.Package", loadPackageParam.classLoader);
            hookAllMethods("com.android.server.pm.PackageManagerService", loadPackageParam.classLoader, "checkDowngrade", new XC_MethodHook() {
                public void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    super.beforeHookedMethod(methodHookParam);
                    Object packageInfoLite = methodHookParam.args[0];

                    if (prefs.getBoolean("downgrade", true)) {
                        Field field = packageClazz.getField("mVersionCode");
                        field.setAccessible(true);
                        field.set(packageInfoLite, 0);
                        field = packageClazz.getField("mVersionCodeMajor");
                        field.setAccessible(true);
                        field.set(packageInfoLite, 0);
                    }
                }
            });
        }
        if (prefs.getBoolean("authcreak", true)) {
            hookAllMethods("android.util.jar.StrictJarVerifier", loadPackageParam.classLoader, "verifyMessageDigest", XC_MethodReplacement.returnConstant(true));
            hookAllMethods("android.util.jar.StrictJarVerifier", loadPackageParam.classLoader, "verify", XC_MethodReplacement.returnConstant(true));
            hookAllMethods("java.security.MessageDigest", loadPackageParam.classLoader, "isEqual", XC_MethodReplacement.returnConstant(true));

            hookAllMethods("com.android.server.pm.PackageManagerServiceUtils", loadPackageParam.classLoader, "verifySignatures", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    if (prefs.getBoolean("zipauthcreak", true)) {
                        param.setResult(Boolean.FALSE);
                    }
                }
            });

            Class<?> signingDetails = XposedHelpers.findClass("android.content.pm.PackageParser.SigningDetails", loadPackageParam.classLoader);
            Constructor<?> findConstructorExact = XposedHelpers.findConstructorExact(signingDetails, Signature[].class, Integer.TYPE);
            findConstructorExact.setAccessible(true);
            Class<?> packageParserException = XposedHelpers.findClass("android.content.pm.PackageParser.PackageParserException", loadPackageParam.classLoader);
            Field error = XposedHelpers.findField(packageParserException, "error");
            error.setAccessible(true);
            Object[] signingDetailsArgs = new Object[2];
            signingDetailsArgs[0] = new Signature[]{new Signature(SIGNATURE)};
            signingDetailsArgs[1] = 1;
            final Object newInstance = findConstructorExact.newInstance(signingDetailsArgs);
            hookAllMethods("android.util.apk.ApkSignatureVerifier", loadPackageParam.classLoader, "verifyV1Signature", new XC_MethodHook() {
                public void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    super.afterHookedMethod(methodHookParam);
                    Throwable throwable = methodHookParam.getThrowable();
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
                }
            });
        }
        if (prefs.getBoolean("digestCreak", true)) {
            //New package has a different signature
            //处理覆盖安装但签名不一致
            Class<?> signingDetails = XposedHelpers.findClass("android.content.pm.PackageParser.SigningDetails", loadPackageParam.classLoader);
            hookAllMethods(signingDetails, "checkCapability", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    if ((Integer) param.args[1] != 4 && prefs.getBoolean("authcreak", true)) {
                        param.setResult(Boolean.TRUE);
                    }
                }
            });
            hookAllMethods(signingDetails, "checkCapabilityRecover",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            if ((Integer) param.args[1] != 4 && prefs.getBoolean("authcreak", true)) {
                                param.setResult(Boolean.TRUE);
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
