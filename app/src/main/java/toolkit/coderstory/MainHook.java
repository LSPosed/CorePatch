package toolkit.coderstory;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageParser;
import android.content.pm.Signature;
import android.content.pm.SigningDetails;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import com.coderstory.toolkit.BuildConfig;

import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    public static final String TAG = "CorePatch";
    public static Set<String> trustedCerts = new HashSet<>();
    private Handler mHandler;
    XSharedPreferences prefs;
    XposedHelper mHelper;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (("android".equals(lpparam.packageName)) && (lpparam.processName.equals("android")) && mHelper != null) {
            if (BuildConfig.DEBUG)
                XposedBridge.log("D/" + TAG + " handleLoadPackage");
            mHelper.handleLoadPackage(lpparam);
            prefs = new XSharedPreferences(BuildConfig.APPLICATION_ID, "trusted_certs");
            updateTrustedCerts();
            broadcastDaemon();
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void broadcastDaemon() {
        var hc = new HandlerThread("CorePatch");
        hc.start();
        mHandler = new Handler(hc.getLooper());
        var registerReceiver = new Runnable() {
            private int mRetry = 0;
            @Override
            public void run() {
                try {
                    var ctx = (Context) XposedHelpers.callMethod(
                            XposedHelpers.callStaticMethod(
                                    XposedHelpers.findClass("android.app.ActivityThread", ClassLoader.getSystemClassLoader()), "currentActivityThread"
                            ), "getSystemContext"
                    );
                    var filter = new IntentFilter();
                    filter.addAction(Constants.ACTION_UPDATE_CERTS);
                    filter.addAction(Constants.ACTION_UPDATE_CONF);
                    var receiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            if (Constants.ACTION_UPDATE_CERTS.equals(intent.getAction())) {
                                updateTrustedCerts();
                            } else if (Constants.ACTION_UPDATE_CONF.equals(intent.getAction())) {
                                if (mHelper != null) mHelper.prefs.reload();
                            }
                        }
                    };
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ctx.registerReceiver(receiver, filter, null, mHandler, Context.RECEIVER_EXPORTED);
                    } else {
                        ctx.registerReceiver(receiver, filter, null, mHandler);
                    }
                    XposedBridge.log("CorePatch Receiver registered");
                } catch (Throwable t) {
                    XposedBridge.log(t);
                    if (mRetry >= 10) return;
                    mHandler.postDelayed(this, 1000);
                    mRetry++;
                }
            }
        };
        var waitForAm = new Runnable() {
            @Override
            public void run() {
                try {
                    var am = XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.os.ServiceManager", ClassLoader.getSystemClassLoader()), "getService", "activity");
                    if (am != null) {
                        mHandler.post(registerReceiver);
                        return;
                    }
                    mHandler.postDelayed(this, 1000);
                } catch (Throwable t) {
                    XposedBridge.log(t);
                }
            }
        };
        mHandler.post(waitForAm);
    }

    private void updateTrustedCerts() {
        XposedBridge.log("updating trusted certs");
        prefs.reload();
        var certs = TrustedCertsUtils.getTrustedCerts(prefs, true);
        trustedCerts.clear();
        for (var c: certs) {
            trustedCerts.add(c.hex);
            XposedBridge.log("CorePatch: new sig " + c.hex);
        }
        XposedBridge.log("updated " + trustedCerts.size() + " trusted certs");
    }

    public static boolean isSignatureTrusted(Object detail) {
        if (trustedCerts.isEmpty()) return false;
        if (detail instanceof String) {
            return trustedCerts.contains(detail);
        } else if (detail != null) {
            Signature[] signatures = null;
            if (detail instanceof PackageParser.SigningDetails) {
                signatures = ((PackageParser.SigningDetails) detail).signatures;
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (detail instanceof SigningDetails) {
                    signatures = ((SigningDetails) detail).getSignatures();
                }
            }
            if (signatures == null) return false;
            for (var sig: signatures) {
                if (isSignatureTrusted(sig)) return true;
            }
        }
        return false;
    }

    public static boolean isSignatureTrusted(Signature signature) {
        return trustedCerts.contains(signature.toCharsString());
    }

    @Override
    public void initZygote(StartupParam startupParam) {
        if (startupParam.startsSystemServer) {
            if (BuildConfig.DEBUG)
                XposedBridge.log("D/" + TAG + " initZygote: Current sdk version " + Build.VERSION.SDK_INT);
            switch (Build.VERSION.SDK_INT) {
                case Build.VERSION_CODES.UPSIDE_DOWN_CAKE: // 34
                    mHelper = new CorePatchForU();
                    break;
                case Build.VERSION_CODES.TIRAMISU: // 33
                    mHelper = new CorePatchForT();
                    break;
                case Build.VERSION_CODES.S_V2: // 32
                case Build.VERSION_CODES.S: // 31
                    mHelper = new CorePatchForS();
                    break;
                case Build.VERSION_CODES.R: // 30
                    mHelper = new CorePatchForR();
                    break;
                case Build.VERSION_CODES.Q: // 29
                case Build.VERSION_CODES.P: // 28
                    mHelper = new CorePatchForQ();
                    break;
                default:
                    XposedBridge.log("W/" + TAG + " Unsupported Version of Android " + Build.VERSION.SDK_INT);
                    break;
            }
        }
    }
}
