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
            switch (Build.VERSION.SDK_INT) {
                case Build.VERSION_CODES.TIRAMISU: // 33
                    new CorePatchForT().handleLoadPackage(lpparam);
                    break;
                case Build.VERSION_CODES.S_V2: // 32
                    new CorePatchForSv2().handleLoadPackage(lpparam);
                    break;
                case Build.VERSION_CODES.S: // 31
                    new CorePatchForS().handleLoadPackage(lpparam);
                    break;
                case Build.VERSION_CODES.R: // 30
                    new CorePatchForR().handleLoadPackage(lpparam);
                    break;
                case Build.VERSION_CODES.Q: // 29
                    new CorePatchForQ().handleLoadPackage(lpparam);
                    break;
                default:
                    XposedBridge.log(TAG + ": Warning: Unsupported Version of Android " + Build.VERSION.SDK_INT);
                    break;
            }
        }
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        if (startupParam.startsSystemServer) {
            Log.d(TAG, "Current sdk version " + Build.VERSION.SDK_INT);
            switch (Build.VERSION.SDK_INT) {
                case Build.VERSION_CODES.TIRAMISU: // 33
                    new CorePatchForT().initZygote(startupParam);
                    break;
                case Build.VERSION_CODES.S_V2: // 32
                    new CorePatchForSv2().initZygote(startupParam);
                    break;
                case Build.VERSION_CODES.S: // 31
                    new CorePatchForS().initZygote(startupParam);
                    break;
                case Build.VERSION_CODES.R: // 30
                    new CorePatchForR().initZygote(startupParam);
                    break;
                case Build.VERSION_CODES.Q: // 29
                    new CorePatchForQ().initZygote(startupParam);
                    break;
                default:
                    XposedBridge.log(TAG + ": Warning: Unsupported Version of Android " + Build.VERSION.SDK_INT);
                    break;
            }
        }
    }
}
