package toolkit.coderstory;


import android.content.pm.Signature;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;


public class CorePatch extends XposedHelper implements IXposedHookLoadPackage {

    // 随便拿的一个签名
    String SIGNATURE = "308203c6308202aea003020102021426d148b7c65944abcf3a683b4c3dd3b139c4ec85300d06092a864886f70d01010b05003074310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e205669657731143012060355040a130b476f6f676c6520496e632e3110300e060355040b1307416e64726f69643110300e06035504031307416e64726f6964301e170d3139303130323138353233385a170d3439303130323138353233385a3074310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e205669657731143012060355040a130b476f6f676c6520496e632e3110300e060355040b1307416e64726f69643110300e06035504031307416e64726f696430820122300d06092a864886f70d01010105000382010f003082010a028201010087fcde48d9beaeba37b733a397ae586fb42b6c3f4ce758dc3ef1327754a049b58f738664ece587994f1c6362f98c9be5fe82c72177260c390781f74a10a8a6f05a6b5ca0c7c5826e15526d8d7f0e74f2170064896b0cf32634a388e1a975ed6bab10744d9b371cba85069834bf098f1de0205cdee8e715759d302a64d248067a15b9beea11b61305e367ac71b1a898bf2eec7342109c9c5813a579d8a1b3e6a3fe290ea82e27fdba748a663f73cca5807cff1e4ad6f3ccca7c02945926a47279d1159599d4ecf01c9d0b62e385c6320a7a1e4ddc9833f237e814b34024b9ad108a5b00786ea15593a50ca7987cbbdc203c096eed5ff4bf8a63d27d33ecc963990203010001a350304e300c0603551d13040530030101ff301d0603551d0e04160414a361efb002034d596c3a60ad7b0332012a16aee3301f0603551d23041830168014a361efb002034d596c3a60ad7b0332012a16aee3300d06092a864886f70d01010b0500038201010022ccb684a7a8706f3ee7c81d6750fd662bf39f84805862040b625ddf378eeefae5a4f1f283deea61a3c7f8e7963fd745415153a531912b82b596e7409287ba26fb80cedba18f22ae3d987466e1fdd88e440402b2ea2819db5392cadee501350e81b8791675ea1a2ed7ef7696dff273f13fb742bb9625fa12ce9c2cb0b7b3d94b21792f1252b1d9e4f7012cb341b62ff556e6864b40927e942065d8f0f51273fcda979b8832dd5562c79acf719de6be5aee2a85f89265b071bf38339e2d31041bc501d5e0c034ab1cd9c64353b10ee70b49274093d13f733eb9d3543140814c72f8e003f301c7a00b1872cc008ad55e26df2e8f07441002c4bcb7dc746745f0db";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws IllegalAccessException, InvocationTargetException, InstantiationException {

        if (("android".equals(loadPackageParam.packageName)) && (loadPackageParam.processName.equals("android"))) {
            // 允许降级
            if (prefs.getBoolean("downgrade", false)) {
                hookAllMethods("com.android.server.pm.PackageManagerService", loadPackageParam.classLoader, "checkDowngrade", XC_MethodReplacement.returnConstant(null));
            }
            if (prefs.getBoolean("authcreak", false)) {
                // apk内文件修改后 digest校验会失败
                hookAllMethods("android.util.jar.StrictJarVerifier", loadPackageParam.classLoader, "verifyMessageDigest", XC_MethodReplacement.returnConstant(true));
                hookAllMethods("android.util.jar.StrictJarVerifier", loadPackageParam.classLoader, "verify", XC_MethodReplacement.returnConstant(true));
                hookAllMethods("java.security.MessageDigest", loadPackageParam.classLoader, "isEqual", XC_MethodReplacement.returnConstant(true));

                // Targeting R+ (version " + Build.VERSION_CODES.R + " and above) requires"
                // + " the resources.arsc of installed APKs to be stored uncompressed"
                // + " and aligned on a 4-byte boundary
                // target >=30 的情况下 resources.arsc 必须是未压缩的且4K对齐
                hookAllMethods("android.content.res.AssetManager", loadPackageParam.classLoader, "containsAllocatedTable", XC_MethodReplacement.returnConstant(false));

                // No signature found in package of version " + minSignatureSchemeVersion
                // + " or newer for package " + apkPath
                findAndHookMethod("android.util.apk.ApkSignatureVerifier", loadPackageParam.classLoader, "getMinimumSignatureSchemeVersionForTargetSdk", int.class, XC_MethodReplacement.returnConstant(0));
                findAndHookMethod("com.android.apksig.ApkVerifier", loadPackageParam.classLoader, "getMinimumSignatureSchemeVersionForTargetSdk", int.class, XC_MethodReplacement.returnConstant(0));

                // Package " + packageName + " signatures do not match previously installed version; ignoring!"
                // public boolean checkCapability(String sha256String, @CertCapabilities int flags) {
                // public boolean checkCapability(SigningDetails oldDetails, @CertCapabilities int flags)
                hookAllMethods("android.content.pm.PackageParser", loadPackageParam.classLoader, "checkCapability", XC_MethodReplacement.returnConstant(true));

                // 当verifyV1Signature抛出转换异常时，替换一个签名作为返回值
                Class<?> signingDetails = XposedHelpers.findClass("android.content.pm.PackageParser.SigningDetails", loadPackageParam.classLoader);
                Constructor<?> findConstructorExact = XposedHelpers.findConstructorExact(signingDetails, Signature[].class, Integer.TYPE);
                findConstructorExact.setAccessible(true);
                Class<?> packageParserException = XposedHelpers.findClass("android.content.pm.PackageParser.PackageParserException", loadPackageParam.classLoader);
                Field error = XposedHelpers.findField(packageParserException, "error");
                error.setAccessible(true);
                Object[] signingDetailsArgs = new Object[2];
                signingDetailsArgs[0] = new Signature[]{new Signature(SIGNATURE)};
                signingDetailsArgs[1] = 1;
                final Object newInstance = findConstructorExact.newInstance(signingDetailsArgs);
                hookAllMethods("android.util.apk.ApkSignatureVerifier", loadPackageParam.classLoader, "verifyV1Signature", new XC_MethodHook() {
                    public void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                        super.afterHookedMethod(methodHookParam);
                        if (prefs.getBoolean("digestCreak", true)) {
                            Throwable throwable = methodHookParam.getThrowable();
                            if (throwable != null) {
                                Throwable cause = throwable.getCause();
                                if (throwable.getClass() == packageParserException) {
                                    if (error.getInt(throwable) == -103) {
                                        methodHookParam.setResult(newInstance);
                                    }
                                }
                                if (cause != null && cause.getClass() == packageParserException) {
                                    if (error.getInt(cause) == -103) {
                                        methodHookParam.setResult(newInstance);
                                    }
                                }
                            }
                        }
                    }
                });
            }
            if (prefs.getBoolean("digestCreak", false)) {
                //New package has a different signature
                //处理覆盖安装但签名不一致
                Class<?> signingDetails = XposedHelpers.findClass("android.content.pm.PackageParser.SigningDetails", loadPackageParam.classLoader);
                hookAllMethods(signingDetails, "checkCapability", XC_MethodReplacement.returnConstant(true));
            }

        }
    }
}
