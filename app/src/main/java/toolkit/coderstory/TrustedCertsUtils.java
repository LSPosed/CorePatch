package toolkit.coderstory;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public class TrustedCertsUtils {
    private static final String KEY_COUNT = "count";
    private static final String KEY_ENABLED_PREFIX = "enabled_";
    private static final String KEY_DISABLED_PREFIX = "disabled_";
    private static final String KEY_DESC_PREFIX = "desc_";
    public static void persistTrustedCerts(SharedPreferences sp, List<TrustedCert> certs) {
        SharedPreferences.Editor editor = sp.edit();
        editor.clear();
        editor.putInt(KEY_COUNT, certs.size());
        for (int i = 0; i < certs.size(); i++) {
            var cert = certs.get(i);
            editor.putString((cert.enabled ? KEY_ENABLED_PREFIX : KEY_DISABLED_PREFIX) + i, cert.hex);
            editor.putString(KEY_DESC_PREFIX + i, cert.desc);
        }
        editor.apply();
    }

    public static List<TrustedCert> getTrustedCerts(SharedPreferences sp, boolean enabledOnly) {
        int count = sp.getInt(KEY_COUNT, 0);
        List<TrustedCert> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TrustedCert cert = null;
            if (sp.contains(KEY_ENABLED_PREFIX + i)) {
                cert = new TrustedCert();
                cert.enabled = true;
                cert.hex = sp.getString(KEY_ENABLED_PREFIX + i, "(null)");
            }
            if (!enabledOnly && sp.contains(KEY_DISABLED_PREFIX + i)) {
                cert = new TrustedCert();
                cert.enabled = false;
                cert.hex = sp.getString(KEY_DISABLED_PREFIX + i, "(null)");
            }
            if (cert != null) {
                cert.desc = sp.getString(KEY_DESC_PREFIX + i, "(no description)");
                list.add(cert);
            }
        }
        return list;
    }

    public static void addToCerts(List<TrustedCert> certs, TrustedCert cert) {
        for (var c: certs) {
            if (c.hex.equals(cert.hex)) {
                c.desc = cert.desc;
                return;
            }
        }
        certs.add(0, cert);
    }
}
