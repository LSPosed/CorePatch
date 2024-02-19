package toolkit.coderstory;

import android.content.pm.PackageParser;
import android.content.pm.SigningDetails;
import android.util.Log;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class CorePatchForT extends CorePatchForS {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        super.handleLoadPackage(loadPackageParam);
        var checkDowngrade = XposedHelpers.findMethodExactIfExists("com.android.server.pm.PackageManagerServiceUtils", loadPackageParam.classLoader,
                "checkDowngrade",
                "com.android.server.pm.parsing.pkg.AndroidPackage",
                "android.content.pm.PackageInfoLite");
        if (checkDowngrade != null) {
            XposedBridge.hookMethod(checkDowngrade, new ReturnConstant(prefs, "downgrade", null));
        }

        Class<?> signingDetails = getSigningDetails(loadPackageParam.classLoader);
        //New package has a different signature
        //处理覆盖安装但签名不一致
        hookAllMethods(signingDetails, "checkCapability", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                // Don't handle PERMISSION (grant SIGNATURE permissions to pkgs with this cert)
                // Or applications will have all privileged permissions
                // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/content/pm/PackageParser.java;l=5947?q=CertCapabilities
                if ((int) param.args[1] != 4) {
                    param.setResult(trustedDigestCrackEnabled(param.args[0]));
                } else {
                    param.setResult(trustedSigPermEnabled(param.args[0]));
                }
            }
        });

        findAndHookMethod("com.android.server.pm.InstallPackageHelper", loadPackageParam.classLoader,
                "doesSignatureMatchForPermissions", String.class,
                "com.android.server.pm.parsing.pkg.ParsedPackage", int.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (prefs.getBoolean("digestCreak", true) && prefs.getBoolean("UsePreSig", false)) {
                            //If we decide to crack this then at least make sure they are same apks, avoid another one that tries to impersonate.
                            if (param.getResult().equals(false)) {
                                String pPname = (String) XposedHelpers.callMethod(param.args[1], "getPackageName");
                                if (pPname.contentEquals((String) param.args[0])) {
                                    param.setResult(true);
                                }
                            }
                        }
                    }
                });

        var assertMinSignatureSchemeIsValid = XposedHelpers.findMethodExactIfExists("com.android.server.pm.ScanPackageUtils", loadPackageParam.classLoader,
                "assertMinSignatureSchemeIsValid",
                "com.android.server.pm.parsing.pkg.AndroidPackage", int.class);
        if (assertMinSignatureSchemeIsValid != null) {
            XposedBridge.hookMethod(assertMinSignatureSchemeIsValid, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (prefs.getBoolean("authcreak", false)) {
                        param.setResult(null);
                    }
                }
            });
        }

        Class<?> strictJarVerifier = findClass("android.util.jar.StrictJarVerifier", loadPackageParam.classLoader);
        if (strictJarVerifier != null) {
            XposedBridge.hookAllConstructors(strictJarVerifier, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (prefs.getBoolean("authcreak", false)) {
                        XposedHelpers.setBooleanField(param.thisObject, "signatureSchemeRollbackProtectionsEnforced", false);
                    }
                }
            });
        }

        // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/services/core/java/com/android/server/pm/permission/PermissionManagerServiceImpl.java;l=3437;drc=a6142601d7cc5b79e9e7f209c2a0cb9f5e79fd2d
        var pms = XposedHelpers.findClassIfExists("com.android.server.pm.permission.PermissionManagerServiceImpl", loadPackageParam.classLoader);
        if (pms != null) {
            deoptimizeMethod(pms, "shouldGrantPermissionBySignature");
        }

        // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/services/core/java/com/android/server/pm/PackageManagerServiceUtils.java;l=621;drc=2e50991320cbef77d3e8504a4b284adae8c2f4d2
        var utils = XposedHelpers.findClassIfExists("com.android.server.pm.PackageManagerServiceUtils", loadPackageParam.classLoader);
        if (utils != null) {
            deoptimizeMethod(utils, "canJoinSharedUserId");
        }

        hookAllMethods(signingDetails, "hasCommonAncestor", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (isVerifyingSignature.get() && trustedSharedUserEnabled(param.thisObject))
                    param.setResult(true);
            }
        });
    }

    Class<?> getSigningDetails(ClassLoader classLoader) {
        return XposedHelpers.findClassIfExists("android.content.pm.SigningDetails", classLoader);
    }

    @Override
    protected void dumpSigningDetails(Object signingDetails, PrintWriter pw) {
        if (signingDetails instanceof PackageParser.SigningDetails) {
            var i = 0;
            for (var sign : ((PackageParser.SigningDetails) signingDetails).signatures) {
                i++;
                pw.println(i + ": " + sign.toCharsString() + " trusted=" + MainHook.isSignatureTrusted(sign));
            }
        } else if (signingDetails instanceof SigningDetails) {
            var i = 0;
            for (var sign : ((SigningDetails) signingDetails).getSignatures()) {
                i++;
                pw.println(i + ": " + sign.toCharsString() + " trusted=" + MainHook.isSignatureTrusted(sign));
            }
        }
    }

    @Override
    protected Object SharedUserSetting_packages(Object sharedUser) {
        return XposedHelpers.getObjectField(sharedUser, "mPackages");
    }
}
