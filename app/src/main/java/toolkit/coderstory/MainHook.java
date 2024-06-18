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
            if (BuildConfig.DEBUG) {
                XposedBridge.log("D/" + TAG + " handleLoadPackage");
            }

            final var corePatchImpl = CorePatch.getImpl();
            corePatchImpl.handleLoadPackage(lpparam);
        }
    }

    @Override
    public void initZygote(StartupParam startupParam) {
        if (startupParam.startsSystemServer) {
            if (BuildConfig.DEBUG) {
                XposedBridge.log("D/" + TAG + " initZygote: Current sdk version " + Build.VERSION.SDK_INT);
            }

            final var corePatchImpl = CorePatch.getImpl();
            corePatchImpl.initZygote(startupParam);
        }
    }
}
