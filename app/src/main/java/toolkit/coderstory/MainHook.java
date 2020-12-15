package toolkit.coderstory;

import android.os.Build;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (Build.VERSION.SDK_INT == 30) {
            new CorePatchForR().handleLoadPackage(lpparam);
        } else if (Build.VERSION.SDK_INT == 29) {
            new CorePatchForQ().handleLoadPackage(lpparam);
        } else {
            XposedBridge.log("Warning: Unsupported Version of Android " + Build.VERSION.SDK_INT);
        }
    }
}
