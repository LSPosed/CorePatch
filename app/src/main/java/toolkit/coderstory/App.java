package toolkit.coderstory;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.coderstory.toolkit.BuildConfig;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App extends Application {
    private final ExecutorService service = Executors.newCachedThreadPool();
    private static App self;

    @Override
    public void onCreate() {
        super.onCreate();
        self = this;
    }

    public static SharedPreferences createDelegate(SharedPreferences sp, String action) {
        return new SharedPreferencesDelegate(sp, action);
    }

    private static class SharedPreferencesDelegate implements SharedPreferences {
        private final SharedPreferences sp;
        private final String action;
        SharedPreferencesDelegate(SharedPreferences sp, String action) {
            this.sp = sp;
            this.action = action;
        }

        @Override
        public Map<String, ?> getAll() {
            return sp.getAll();
        }

        @Override
        public String getString(String s, String s1) {
            return sp.getString(s, s1);
        }

        @Override
        public Set<String> getStringSet(String s, Set<String> set) {
            return sp.getStringSet(s, set);
        }

        @Override
        public int getInt(String s, int i) {
            return sp.getInt(s, i);
        }

        @Override
        public long getLong(String s, long l) {
            return sp.getLong(s, l);
        }

        @Override
        public float getFloat(String s, float v) {
            return sp.getFloat(s, v);
        }

        @Override
        public boolean getBoolean(String s, boolean b) {
            return sp.getBoolean(s, b);
        }

        @Override
        public boolean contains(String s) {
            return sp.contains(s);
        }

        @Override
        public Editor edit() {
            var editor = sp.edit();
            return new SharedPreferenceEditorDelegate(editor, action);
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
            sp.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
        }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
            sp.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
        }
    }

    private static class SharedPreferenceEditorDelegate implements SharedPreferences.Editor {
        private final SharedPreferences.Editor editor;
        private final String action;

        SharedPreferenceEditorDelegate(SharedPreferences.Editor editor, String action) {
            this.editor = editor;
            this.action = action;
        }

        @Override
        public SharedPreferences.Editor putString(String s, String s1) {
            return editor.putString(s, s1);
        }

        @Override
        public SharedPreferences.Editor putStringSet(String s, Set<String> set) {
            return editor.putStringSet(s, set);
        }

        @Override
        public SharedPreferences.Editor putInt(String s, int i) {
            return editor.putInt(s, i);
        }

        @Override
        public SharedPreferences.Editor putLong(String s, long l) {
            return editor.putLong(s, l);
        }

        @Override
        public SharedPreferences.Editor putFloat(String s, float v) {
            return editor.putFloat(s, v);
        }

        @Override
        public SharedPreferences.Editor putBoolean(String s, boolean b) {
            return editor.putBoolean(s, b);
        }

        @Override
        public SharedPreferences.Editor remove(String s) {
            return editor.remove(s);
        }

        @Override
        public SharedPreferences.Editor clear() {
            return editor.clear();
        }

        @Override
        public boolean commit() {
            var result = editor.commit();
            Log.d("CorePatch", "sending broadcast " + action);
            self.sendBroadcast(new Intent(action).setPackage("android"));
            return result;
        }

        @Override
        public void apply() {
            self.service.submit(this::commit);
        }
    }
}
