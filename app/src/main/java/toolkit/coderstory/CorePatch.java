package toolkit.coderstory;


import android.content.pm.PackageInfo;
import android.content.pm.Signature;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;


public class CorePatch extends XposedHelper implements IXposedHookZygoteInit, IXposedHookLoadPackage {

    private static final String SIGNATURE = "308203553082023da0030201020204378edaaa300d06092a864886f70d01010b0500305a310d300b0603550406130466616b65310d300b0603550408130466616b65310d300b0603550407130466616b65310d300b060355040a130466616b65310d300b060355040b130466616b65310d300b0603550403130466616b653020170d3138303533303034343434385a180f32313237313230353034343434385a305a310d300b0603550406130466616b65310d300b0603550408130466616b65310d300b0603550407130466616b65310d300b060355040a130466616b65310d300b060355040b130466616b65310d300b0603550403130466616b6530820122300d06092a864886f70d01010105000382010f003082010a0282010100b766ff6afd8a53edd4cee4985bc90e0c515157b5e9f731818961f7250d0d1ac7c7fb80eb5aeb8c28478732e8ff38cff574bfa0eba8039f73af1532f939c4ef9684719efbaba2dd3c583a20907c1c55248a63098c6da23dcfc877763d5fe6061dddd399cf2f49e3250e23f9e687a4d182bcd0662179ba4c9983448e34b4c83e5abbf4f87e87add9157c75fd40de3416744507a3517915f35b6fcad78766e8e1879df8ab823a6ffa335e4790f6e29c87393732025b63ce3a38e42cb0d48cdceb902f191d7d45823db9a0678895e8bfc59b2af7526ca4c2dc3dbe7e70c7c840e666b9629d36e5ddf1d9a80c37f1ab1bc1fb30432914008fbde95d5d3db7853565510203010001a321301f301d0603551d0e04160414d8513e1ae21c64e9ebeee3507e24ea375eef958e300d06092a864886f70d01010b0500038201010088bf20b36428558359536dddcfff16fe233656a92364cb544d8acc43b0859f880a8da339dd430616085edf035e4e6e6dd2281ceb14adde2f05e9ac58d547a09083eece0c6d405289cb7918f85754ee545eefe35e30c103cad617905e94eb4fb68e6920a60d30577855f9feb6e3a664856f74aa9f824aa7d4a3adf85e162c67b9a4261e3185f038ead96112ae3e574d280425e90567352fb82bc9173302122025eaecfabd94d0f9be69a85c415f7cf7759c9651734300952027b316c37aaa1b2418865a3fc7b6bd1072c92ccaacdaa1cf9586d9b8310ceee066ce68859107dfc45ccce729ad9e75b53b584fa37dcd64da8673b1279c6c5861ed3792deac156c8a";
    private HashSet<Signature> signatures;

    public void initZygote(IXposedHookZygoteInit.StartupParam paramStartupParam) {

        XposedHelpers.findAndHookMethod("java.security.MessageDigest", null, "isEqual", byte[].class, byte[].class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                if (prefs.getBoolean("zipauthcreak", true)) {
                    param.setResult(Boolean.TRUE);
                }
            }
        });

        hookAllMethods(findClass("com.android.org.conscrypt.OpenSSLSignature", null), "engineVerify", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                if (prefs.getBoolean("zipauthcreak", true)) {
                    param.setResult(Boolean.TRUE);
                }
            }
        });

        final Class packageClazz = XposedHelpers.findClass("android.util.apk.ApkSignatureSchemeV2Verifier", null);
        hookAllMethods("android.content.pm.PackageParser", null, "getApkSigningVersion", XC_MethodReplacement.returnConstant(1));
        hookAllConstructors("android.util.jar.StrictJarVerifier", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                if (prefs.getBoolean("zipauthcreak", true)) {
                    param.args[3] = Boolean.FALSE;
                }
            }
        });
        hookAllConstructors("android.util.apk.ApkSignatureSchemeV2Verifier", new XC_MethodHook() {

            public void afterHookedMethod(MethodHookParam methodHookParam) throws NoSuchFieldException, IllegalAccessException {
                if (prefs.getBoolean("zipauthcreak", true)) {
                    Field field = packageClazz.getField("SF_ATTRIBUTE_ANDROID_APK_SIGNED_ID");
                    field.setAccessible(true);
                    field.set(methodHookParam.thisObject, -1);
                }
            }
        });
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws IllegalAccessException, InvocationTargetException, InstantiationException {

        if (("android".equals(loadPackageParam.packageName)) && (loadPackageParam.processName.equals("android"))) {

            Class packageClazz = XposedHelpers.findClass("android.content.pm.PackageParser.Package", loadPackageParam.classLoader);
            Class signingDetails = XposedHelpers.findClass("android.content.pm.PackageParser.SigningDetails", loadPackageParam.classLoader);

            Constructor findConstructorExact = XposedHelpers.findConstructorExact(signingDetails, Signature[].class, Integer.TYPE);
            findConstructorExact.setAccessible(true);

            Class packageParserException = XposedHelpers.findClass("android.content.pm.PackageParser.PackageParserException", loadPackageParam.classLoader);
            Field error = XposedHelpers.findField(packageParserException, "error");
            error.setAccessible(true);

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
            hookAllMethods(signingDetails, "checkCapability", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    if (prefs.getBoolean("authcreak", true)) {
                        param.setResult(Boolean.TRUE);
                    }
                }
            });
            hookAllMethods(signingDetails, "checkCapabilityRecover", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    if (prefs.getBoolean("authcreak", true)) {
                        param.setResult(Boolean.TRUE);
                    }
                }
            });
            hookAllMethods("com.android.server.pm.PackageManagerServiceUtils", loadPackageParam.classLoader, "verifySignatures", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    if (prefs.getBoolean("zipauthcreak", true)) {
                        param.setResult(Boolean.FALSE);
                    }
                }
            });
            Object[] signingDetailsArgs = new Object[2];
            signingDetailsArgs[0] = new Signature[]{new Signature(SIGNATURE)};
            signingDetailsArgs[1] = 1;
            final Object newInstance = findConstructorExact.newInstance(signingDetailsArgs);
            hookAllMethods("android.util.apk.ApkSignatureVerifier", loadPackageParam.classLoader, "verifyV1Signature", new XC_MethodHook() {
                public void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    super.afterHookedMethod(methodHookParam);

                    if (prefs.getBoolean("zipauthcreak", true)) {
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
                }
            });
            hookAllMethods("com.android.server.pm.PackageManagerService", loadPackageParam.classLoader, "systemReady", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    PackageInfo packageInfo = (PackageInfo) XposedHelpers.callMethod(param.thisObject, "getPackageInfo", new Object[]{"android", 64, 0});
                    if (packageInfo.signatures != null) {
                        CorePatch.this.signatures = new HashSet<>(Arrays.asList(packageInfo.signatures));
                    }
                }
            });
            hookAllMethods("com.android.server.pm.PackageManagerService", loadPackageParam.classLoader, "compareSignatures", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    super.beforeHookedMethod(methodHookParam);
                    if (prefs.getBoolean("authcreak", false) && CorePatch.this.signatures != null) {
                        Signature[] signatureArr = (Signature[]) methodHookParam.args[0];
                        if (signatureArr != null && signatureArr.length > 0) {
                            for (Signature signature : signatures) {
                                if (!CorePatch.this.signatures.contains(signature)) {
                                    return;
                                }
                            }
                        }
                        signatureArr = (Signature[]) methodHookParam.args[1];
                        if (signatureArr != null && signatureArr.length > 0) {
                            for (Signature signature : signatures) {
                                if (!CorePatch.this.signatures.contains(signature)) {
                                    return;
                                }
                            }
                        }
                        methodHookParam.setResult(0);
                    }
                }
            });

            final Class packageManagerService = findClass("com.android.server.pm.PackageManagerService", loadPackageParam.classLoader);
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                //allow_no_sig
                hookAllMethods(packageManagerService, "compareSignaturesCompat", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        super.beforeHookedMethod(param);
                        if (prefs.getBoolean("authcreak", true)) {
                            param.setResult(0);
                        }
                    }
                });

                hookAllMethods(packageManagerService, "compareSignaturesRecover", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        super.beforeHookedMethod(param);
                        if (prefs.getBoolean("authcreak", true)) {
                            param.setResult(0);
                        }
                    }
                });


                findAndHookMethod("com.android.server.pm.PackageManagerServiceInjector", loadPackageParam.classLoader, "isAllowedInstall", XC_MethodReplacement.returnConstant(true));

                //disable_verify
                hookAllMethods(packageManagerService, "canSkipFullPackageVerification", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        super.beforeHookedMethod(param);
                        if (prefs.getBoolean("zipauthcreak", true)) {
                            param.setResult(Boolean.TRUE);
                        }
                    }
                });
                // disable_verify
                hookAllMethods(packageManagerService, "canSkipFullApkVerification", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        super.beforeHookedMethod(param);
                        if (prefs.getBoolean("zipauthcreak", true)) {
                            param.setResult(Boolean.TRUE);
                        }
                    }
                });
            }
        }
    }
}
