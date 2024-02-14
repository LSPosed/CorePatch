package toolkit.coderstory;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;

import com.coderstory.toolkit.R;

import java.lang.reflect.Method;

@SuppressWarnings("deprecation")
public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        checkXSharedPreferences();
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, new SettingsFragment()).commit();
        }
    }

    @SuppressLint("WorldReadableFiles")
    private void checkXSharedPreferences() {
        try {
            // getSharedPreferences will hooked by LSPosed
            // will not throw SecurityException
            //noinspection deprecation
            getSharedPreferences("conf", Context.MODE_WORLD_READABLE);
        } catch (SecurityException exception) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.config_error)
                    .setMessage(R.string.not_supported)
                    .setPositiveButton(android.R.string.ok, (dialog12, which) -> finish())
                    .setNegativeButton(R.string.ignore, null)
                    .show();
        }
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        var sp = super.getSharedPreferences(name, mode);
        return App.createDelegate(sp, Constants.ACTION_UPDATE_CONF);
    }

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            getPreferenceManager().setSharedPreferencesName("conf");
            addPreferencesFromResource(R.xml.prefs);
            findPreference("trustedCerts").setOnPreferenceClickListener(this);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            view.setOnApplyWindowInsetsListener((v, windowInsets) -> {
                Insets insets = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    insets = windowInsets.getInsets(WindowInsets.Type.systemBars());
                } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    insets = windowInsets.getSystemWindowInsets();
                }
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    mlp.leftMargin = insets.left;
                    mlp.bottomMargin = insets.bottom;
                    mlp.rightMargin = insets.right;
                    mlp.topMargin = insets.top;
                } else {
                    mlp.leftMargin = windowInsets.getSystemWindowInsetLeft();
                    mlp.bottomMargin = windowInsets.getSystemWindowInsetBottom();
                    mlp.rightMargin = windowInsets.getSystemWindowInsetRight();
                    mlp.topMargin = windowInsets.getSystemWindowInsetTop();
                }
                v.setLayoutParams(mlp);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    return WindowInsets.CONSUMED;
                } else return windowInsets.consumeSystemWindowInsets();
            });
            super.onViewCreated(view, savedInstanceState);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("UsePreSig") && sharedPreferences.getBoolean(key, false)) {
                try {
                    @SuppressLint("PrivateApi") Class<?> c = Class.forName("android.os.SystemProperties");
                    Method get = c.getMethod("get", String.class);
                    if (!((String) get.invoke(c, "ro.miui.ui.version.code")).isEmpty()) {
                        new AlertDialog.Builder(getActivity()).setMessage(R.string.miui_usepresig_warn).setPositiveButton(android.R.string.ok, null).show();
                    }
                } catch (Exception ignored) {
                }

                new AlertDialog.Builder(getActivity()).setMessage(R.string.usepresig_warn).setPositiveButton(android.R.string.ok, null).show();
            }
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            if ("trustedCerts".equals(preference.getKey())) {
                startActivity(new Intent(getActivity(), TrustedCertsActivity.class));
            }
            return true;
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }
    }
}
