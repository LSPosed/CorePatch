package toolkit.coderstory;

import java.lang.reflect.InvocationTargetException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class CorePatchForS extends CorePatchForR {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        super.handleLoadPackage(loadPackageParam);
        if (prefs.getBoolean("digestCreak", true) && prefs.getBoolean("UsePreSig", false)) {
            findAndHookMethod("com.android.server.pm.PackageManagerService", loadPackageParam.classLoader, "doesSignatureMatchForPermissions", String.class, "com.android.server.pm.parsing.pkg.ParsedPackage", int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    //If we decide to crack this then at least make sure they are same apks, avoid another one that tries to impersonate.
                    if (param.getResult().equals(false)) {
                        String pPname = (String) XposedHelpers.callMethod(param.args[1], "getPackageName");
                        if (pPname.contentEquals((String) param.args[0])) {
                            param.setResult(true);
                        }
                    }
                }
            });
        }
    }
}
