package toolkit.coderstory.patch;

import android.annotation.TargetApi;
import android.os.Build;

import java.lang.reflect.InvocationTargetException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

@TargetApi(Build.VERSION_CODES.S)
public class CorePatchImplApiS extends CorePatchImplApiR {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        super.handleLoadPackage(loadPackageParam);
        var pmService = XposedHelpers.findClassIfExists("com.android.server.pm.PackageManagerService",
                loadPackageParam.classLoader);
        if (pmService != null) {
            var doesSignatureMatchForPermissions = XposedHelpers.findMethodExactIfExists(pmService, "doesSignatureMatchForPermissions",
                    String.class, "com.android.server.pm.parsing.pkg.ParsedPackage", int.class);
            if (doesSignatureMatchForPermissions != null) {
                XposedBridge.hookMethod(doesSignatureMatchForPermissions, new XC_MethodHook() {
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
            }
        }
    }
}
