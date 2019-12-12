package toolkit.coderstory;


import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import toolkit.coderstory.com.toolkit.BuildConfig;


public class XposedHelper {

    protected XSharedPreferences prefs = new XSharedPreferences("com.coderstory.toolkit", "config");

    {
        prefs.makeWorldReadable();
        prefs.reload();
    }

    protected static void findAndHookMethod(String p1, String p2, Object[] p3) throws ClassNotFoundException {
        try {
            XposedHelpers.findAndHookMethod(Class.forName(p1), p2, p3);
        } catch (Exception e) {
            if (BuildConfig.DEBUG)
                XposedBridge.log(e);
        }
    }


    public static void findAndHookMethod(String p1, ClassLoader lpparam, String p2, Object... parameterTypesAndCallback) {
        try {
            if (findClass(p1, lpparam) != null) {
                XposedHelpers.findAndHookMethod(p1, lpparam, p2, parameterTypesAndCallback);
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG)
                XposedBridge.log(e);
        }
    }

    public static void hookAllConstructors(String p1, XC_MethodHook parameterTypesAndCallback) {
        try {
            Class packageParser = findClass(p1, null);
            hookAllConstructors(packageParser, parameterTypesAndCallback);
        } catch (Exception e) {
            if (BuildConfig.DEBUG)
                XposedBridge.log(e);
        }
    }

    private static Set<XC_MethodHook.Unhook> hookAllConstructors(Class<?> hookClass, XC_MethodHook callback) {
        try {
            return XposedBridge.hookAllConstructors(hookClass, callback);
        } catch (Exception e) {
            if (BuildConfig.DEBUG)
                XposedBridge.log(e);
            return null;
        }

    }

    public static void hookAllMethods(String p1, ClassLoader lpparam, String methodName, XC_MethodHook parameterTypesAndCallback) {
        try {
            Class packageParser = findClass(p1, lpparam);
            XposedBridge.hookAllMethods(packageParser, methodName, parameterTypesAndCallback);
        } catch (Exception e) {
            if (BuildConfig.DEBUG)
                XposedBridge.log(e);
        }

    }

    public void hookAllMethods(Class packageManagerServiceUtils, String verifySignatures, XC_MethodHook methodHook) {
        try {
            XposedBridge.hookAllMethods(packageManagerServiceUtils, verifySignatures, methodHook);
        } catch (Exception e) {
            if (BuildConfig.DEBUG)
                XposedBridge.log(e);
        }
    }

    @SuppressWarnings("unchecked")
    protected static Object getDrmResultSUCCESS() {
        Class<Enum> drmSuccess = (Class<Enum>) XposedHelpers.findClass("miui.drm.DrmManager.DrmResult", null);
        return Enum.valueOf(drmSuccess, "DRM_SUCCESS");
    }


    public static Class<?> findClass(String className, ClassLoader classLoader) {
        try {
            return Class.forName(className,false,classLoader);
        } catch (Exception e) {
            if (BuildConfig.DEBUG)
                XposedBridge.log(e);
        }
        return null;
    }

    public static Class<?> findClassWithOutLog(String className, ClassLoader classLoader) {
        try {
            return Class.forName(className,false,classLoader);
        } catch (Exception e) {
            // 忽略
        }
        return null;
    }


}
