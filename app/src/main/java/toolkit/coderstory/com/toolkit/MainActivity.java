package toolkit.coderstory.com.toolkit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Switch;

import java.io.File;

import eu.chainfire.libsuperuser.Shell;

public class MainActivity extends AppCompatActivity {
    private static SharedPreferences prefs;
    private static SharedPreferences.Editor editor;
    String ApplicationName = "com.coderstory.toolkit";
    public final String PREFS_FOLDER = " /data/data/" + ApplicationName + "/shared_prefs\n";
    public final String PREFS_FILE = " /data/data/" + ApplicationName + "/shared_prefs/" + "conf.xml" + ".xml\n";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        $(R.id.authcreak).setOnClickListener(v -> {
            getEditor().putBoolean("authcreak", ((Switch) v).isChecked());
            getEditor().apply();
            sudoFixPermissions();
        });

        $(R.id.zipauthcreak).setOnClickListener(v -> {
            getEditor().putBoolean("zipauthcreak", ((Switch) v).isChecked());
            getEditor().apply();
            sudoFixPermissions();
        });


        $(R.id.downgrade).setOnClickListener(v -> {
            getEditor().putBoolean("downgrade", ((Switch) v).isChecked());
            getEditor().apply();
            sudoFixPermissions();
        });

        ((Switch) $(R.id.authcreak)).setChecked(getPrefs().getBoolean("authcreak", false));
        ((Switch) $(R.id.zipauthcreak)).setChecked(getPrefs().getBoolean("zipauthcreak", false));
        ((Switch) $(R.id.downgrade)).setChecked(getPrefs().getBoolean("downgrade", false));

        new Thread(() -> {
            if (!Shell.SU.available()) {
                myHandler.sendMessage(new Message());
            }
        }).start();

    }

    protected <T extends View> T $(int id) {
        return (T) findViewById(id);
    }

    protected SharedPreferences.Editor getEditor() {
        if (editor == null) {
            editor = prefs.edit();
        }
        return editor;

    }

    protected void sudoFixPermissions() {
        new Thread(() -> {
            File pkgFolder = new File("/data/data/" + ApplicationName);
            if (pkgFolder.exists()) {
                pkgFolder.setExecutable(true, false);
                pkgFolder.setReadable(true, false);
            }
            Shell.SU.run("chmod  755 " + PREFS_FOLDER);
            // Set preferences file permissions to be world readable
            Shell.SU.run("chmod  644 " + PREFS_FILE);
        }).start();
    }

    protected SharedPreferences getPrefs() {
        prefs = getSharedPreferences("UserSettings", Context.MODE_PRIVATE);
        return prefs;
    }
    @SuppressLint("HandlerLeak")
    Handler myHandler = new Handler() {
        public void handleMessage(Message msg) {
            final AlertDialog.Builder normalDialog =
                    new AlertDialog.Builder(MainActivity.this);
            normalDialog.setTitle("提示");
            normalDialog.setMessage("请先授权应用ROOT权限");
            normalDialog.setPositiveButton("确定",
                    (dialog, which) -> System.exit(0));
            // 显示
            normalDialog.show();
            super.handleMessage(msg);
        }
    };
}
