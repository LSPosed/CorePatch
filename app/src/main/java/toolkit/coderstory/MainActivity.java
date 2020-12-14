package toolkit.coderstory;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Switch;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;


import java.io.File;

import eu.chainfire.libsuperuser.Shell;
import toolkit.coderstory.com.toolkit.R;

public class MainActivity extends AppCompatActivity {

    private static SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 修改包
        $(R.id.authcreak).setOnClickListener(v -> {
            getEditor().putBoolean("authcreak", ((Switch) v).isChecked());
            getEditor().commit();
        });

        $(R.id.alipay).setOnClickListener(v->{
            Uri uri = Uri.parse("http://paypal.me/code620");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        });

        // 不同签名
        $(R.id.digestCreak).setOnClickListener(v -> {
            getEditor().putBoolean("digestCreak", ((Switch) v).isChecked());
            getEditor().commit();
        });

        //降级
        $(R.id.downgrade).setOnClickListener(v -> {
            getEditor().putBoolean("downgrade", ((Switch) v).isChecked());
            getEditor().commit();
        });

        ((Switch) $(R.id.authcreak)).setChecked(getPrefs().getBoolean("authcreak", true));
        ((Switch) $(R.id.digestCreak)).setChecked(getPrefs().getBoolean("digestCreak", false));
        ((Switch) $(R.id.downgrade)).setChecked(getPrefs().getBoolean("downgrade", true));
        ((Switch) $(R.id.hideicon)).setChecked(getPrefs().getBoolean("hideIcon", false));

        $(R.id.hideicon).setOnClickListener(v -> {
            getEditor().putBoolean("hideIcon", ((Switch) v).isChecked());
            getEditor().commit();
            ComponentName localComponentName = new ComponentName(MainActivity.this, "toolkit.coderstory.MainActivity");
            PackageManager localPackageManager = getPackageManager();
            localPackageManager.getComponentEnabledSetting(localComponentName);
            PackageManager packageManager = getPackageManager();
            ComponentName componentName = new ComponentName(MainActivity.this, "toolkit.coderstory.MainActivity");

            if (((Switch) v).isChecked()) {
                packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
            } else {
                packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                        PackageManager.DONT_KILL_APP);
            }
        });

    }

    protected <T extends View> T $(int id) {
        return findViewById(id);
    }

    protected SharedPreferences.Editor getEditor() {
        if (editor == null) {
            editor = getPrefs().edit();
        }
        return editor;
    }
    
    protected SharedPreferences getPrefs() {
        SharedPreferences prefs = getSharedPreferences("conf.xml", Context.MODE_PRIVATE);
        return prefs;
    }
}
