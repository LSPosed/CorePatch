package toolkit.coderstory;

import android.os.Build;

import com.coderstory.toolkit.BuildConfig;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    public static final String TAG = "CorePatch";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (("android".equals(lpparam.packageName)) && (lpparam.processName.equals("android"))) {
            if (BuildConfig.DEBUG)
                XposedBridge.log("D/" + TAG + " handleLoadPackage");
            switch (Build.VERSION.SDK_INT) {
                case Build.VERSION_CODES.VANILLA_ICE_CREAM: // 35
                    new CorePatchForV().handleLoadPackage(lpparam);
                    break;
                case Build.VERSION_CODES.UPSIDE_DOWN_CAKE: // 34
                    new CorePatchForV().handleLoadPackage(lpparam);
                    break;
                case Build.VERSION_CODES.TIRAMISU: // 33
                    new CorePatchForT().handleLoadPackage(lpparam);
                    break;
                case Build.VERSION_CODES.S_V2: // 32
                case Build.VERSION_CODES.S: // 31
                    new CorePatchForS().handleLoadPackage(lpparam);
                    break;
                case Build.VERSION_CODES.R: // 30
                    new CorePatchForR().handleLoadPackage(lpparam);
                    break;
                case Build.VERSION_CODES.Q: // 29
                case Build.VERSION_CODES.P: // 28
                    new CorePatchForQ().handleLoadPackage(lpparam);
                    break;
                default:
                    XposedBridge.log("W/" + TAG + " Unsupported Version of Android " + Build.VERSION.SDK_INT);
                    break;
            }
        }
    }

    @Override
    public void initZygote(StartupParam startupParam) {
        if (startupParam.startsSystemServer) {
            if (BuildConfig.DEBUG)
                XposedBridge.log("D/" + TAG + " initZygote: Current sdk version " + Build.VERSION.SDK_INT);
            switch (Build.VERSION.SDK_INT) {
                case Build.VERSION_CODES.UPSIDE_DOWN_CAKE: // 34
                    new CorePatchForU().initZygote(startupParam);
                    break;
                case Build.VERSION_CODES.TIRAMISU: // 33
                    new CorePatchForT().initZygote(startupParam);
                    break;
                case Build.VERSION_CODES.S_V2: // 32
                case Build.VERSION_CODES.S: // 31
                    new CorePatchForS().initZygote(startupParam);
                    break;
                case Build.VERSION_CODES.R: // 30
                    new CorePatchForR().initZygote(startupParam);
                    break;
                case Build.VERSION_CODES.Q: // 29
                case Build.VERSION_CODES.P: // 28
                    new CorePatchForQ().initZygote(startupParam);
                    break;
                default:
                    XposedBridge.log("W/" + TAG + " Unsupported Version of Android " + Build.VERSION.SDK_INT);
                    break;
            }
        }
    }
}
