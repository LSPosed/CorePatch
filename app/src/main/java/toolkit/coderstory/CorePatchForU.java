package toolkit.coderstory;

import java.lang.reflect.InvocationTargetException;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class CorePatchForU extends CorePatchForT {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        super.handleLoadPackage(loadPackageParam);

        // ee11a9c (Rename AndroidPackageApi to AndroidPackage)
        findAndHookMethod("com.android.server.pm.PackageManagerServiceUtils", loadPackageParam.classLoader,
                "checkDowngrade",
                "com.android.server.pm.pkg.AndroidPackage",
                "android.content.pm.PackageInfoLite",
                new ReturnConstant(prefs, "downgrade", null));

        findAndHookMethod("com.nothing.server.ex.NtConfigListServiceImpl", loadPackageParam.classLoader,
                "isInstallingAppForbidden", java.lang.String.class,
                new ReturnConstant(prefs, "bypassBlock", false));

        findAndHookMethod("com.nothing.server.ex.NtConfigListServiceImpl", loadPackageParam.classLoader,
                "isStartingAppForbidden", java.lang.String.class,
                new ReturnConstant(prefs, "bypassBlock", false));
    }
}
