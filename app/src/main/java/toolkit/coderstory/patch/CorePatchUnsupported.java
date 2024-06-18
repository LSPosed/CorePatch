package toolkit.coderstory.patch;

import android.os.Build;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import toolkit.coderstory.CorePatch;
import static toolkit.coderstory.MainHook.TAG;

public class CorePatchUnsupported extends CorePatch {

    private void logWarning() {
        XposedBridge.log("W/" + TAG + " Unsupported Version of Android " + Build.VERSION.SDK_INT);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        // Unsupported version of Android
        logWarning();
    }

    @Override
    public void initZygote(StartupParam startupParam) {
        // Unsupported version of Android
        logWarning();
    }

}
