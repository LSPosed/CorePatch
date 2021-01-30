package toolkit.coderstory;

import android.os.Build;
import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    public static final String TAG = "CorePatch";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (("android".equals(lpparam.packageName)) && (lpparam.processName.equals("android"))) {
            Log.d(TAG, "Current sdk version " + Build.VERSION.SDK_INT);
            if (Build.VERSION.SDK_INT == 30) {
                new CorePatchForR().handleLoadPackage(lpparam);
            } else if (Build.VERSION.SDK_INT == 29) {
                new CorePatchForQ().handleLoadPackage(lpparam);
            } else {
                XposedBridge.log("Warning: Unsupported Version of Android " + Build.VERSION.SDK_INT);
            }
        }
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        if (startupParam.startsSystemServer) {
            Log.d(TAG, "Current sdk version " + Build.VERSION.SDK_INT);
            if (Build.VERSION.SDK_INT == 30) {
                new CorePatchForR().initZygote(startupParam);
            } else if (Build.VERSION.SDK_INT == 29) {
                new CorePatchForQ().initZygote(startupParam);
            } else {
                XposedBridge.log("Warning: Unsupported Version of Android " + Build.VERSION.SDK_INT);
            }
        }
    }
}
