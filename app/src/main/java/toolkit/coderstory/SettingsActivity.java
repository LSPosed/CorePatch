package toolkit.coderstory;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.View;

import com.coderstory.toolkit.R;

import java.lang.reflect.Method;

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

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesName("conf");
            addPreferencesFromResource(R.xml.prefs);
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            View list = view.findViewById(android.R.id.list);
            list.setOnApplyWindowInsetsListener((v, insets) -> {
                list.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getStableInsetBottom());
                return insets.consumeSystemWindowInsets();
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
        public void onDestroy() {
            super.onDestroy();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }
    }
}
