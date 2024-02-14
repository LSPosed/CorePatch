package toolkit.coderstory;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coderstory.toolkit.R;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class TrustedCertsActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final List<TrustedCert> list = new ArrayList<>();
    private Adapter adapter;
    private SharedPreferences sp;
    private ListView listView;
    private TextView emptyView;
    private TrustedCert selectedCert;
    private static final Pattern PATTERN_SIGNATURE = Pattern.compile("[0-9a-fA-F]+");

    private class Adapter extends ArrayAdapter<TrustedCert> {

        public Adapter() {
            super(TrustedCertsActivity.this, 0, list);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            var cert = getItem(position);
            assert cert != null;
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(TrustedCertsActivity.this).inflate(R.layout.item_cert, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.desc = convertView.findViewById(R.id.desc);
                viewHolder.signature = convertView.findViewById(R.id.signature);
                viewHolder.checkBox = convertView.findViewById(R.id.checkbox);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.desc.setText(cert.desc);
            viewHolder.signature.setText(cert.hex);
            viewHolder.checkBox.setOnCheckedChangeListener(null);
            viewHolder.checkBox.setChecked(cert.enabled);
            viewHolder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                    onItemChecked(cert, isChecked));
            convertView.setOnClickListener((v) -> onItemChecked(cert, !cert.enabled));
            convertView.setOnLongClickListener((v) -> {
                selectedCert = cert;
                return false;
            });
            convertView.setOnCreateContextMenuListener((menu, v, menuInfo) -> {
                getMenuInflater().inflate(R.menu.cert_item_menu, menu);
            });
            return convertView;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        var id = item.getItemId();
        item.getMenuInfo();
        if (id == R.id.remove) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.remove)
                    .setPositiveButton(R.string.yes, (_1, _2) -> {
                        removeCert(selectedCert);
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
            return true;
        } else if (id == R.id.edit) {
            showEditDialog(selectedCert);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    private static class ViewHolder {
        TextView desc;
        TextView signature;
        CheckBox checkBox;
    }

    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trusted_certs);
        listView = findViewById(R.id.list);
        emptyView = findViewById(R.id.empty);
        adapter = new Adapter();
        listView.setAdapter(adapter);
        sp = App.createDelegate(getSharedPreferences("trusted_certs", Context.MODE_WORLD_READABLE), Constants.ACTION_UPDATE_CERTS);
        sp.registerOnSharedPreferenceChangeListener(this);
        sp.getAll();
        refreshList();
        if (getIntent().hasExtra(Intent.EXTRA_STREAM)) handleAddCertsFromIntent();
    }

    @Override
    protected void onDestroy() {
        sp.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.trusted_certs, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        var id = item.getItemId();
        if (id == R.id.add) {
            showEditDialog(null);
        } else if (id == R.id.clear) {
            if (!list.isEmpty())
                new AlertDialog.Builder(this)
                        .setMessage(R.string.remove_all)
                        .setPositiveButton(R.string.yes, (_1, _2) -> {
                            removeAllCerts();
                        })
                        .show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (!"count".equals(s)) return;
        refreshList();
    }

    private void refreshList() {
        list.clear();
        list.addAll(TrustedCertsUtils.getTrustedCerts(sp, false));
        emptyView.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        listView.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
        adapter.notifyDataSetChanged();
    }

    private void addCerts(List<TrustedCert> newCerts) {
        var list = new ArrayList<>(this.list);
        for (var c: newCerts) {
            TrustedCertsUtils.addToCerts(list, c);
        }
        TrustedCertsUtils.persistTrustedCerts(sp, list);
    }

    private void removeCert(TrustedCert cert) {
        var list = new ArrayList<>(this.list);
        list.remove(cert);
        TrustedCertsUtils.persistTrustedCerts(sp, list);
    }

    private void onItemChecked(TrustedCert cert, boolean isChecked) {
        var list = new ArrayList<>(this.list);
        for (var c: list) {
            if (c.equals(cert)) {
                c.enabled = isChecked;
                break;
            }
        }
        TrustedCertsUtils.persistTrustedCerts(sp, list);
    }

    private void removeAllCerts() {
        TrustedCertsUtils.persistTrustedCerts(sp, new ArrayList<>());
    }

    private void addCertFrom(String path, AlertDialog loadingDialog) {
        var info = getPackageManager().getPackageArchiveInfo(path, PackageManager.GET_SIGNING_CERTIFICATES);
        assert info != null;
        var packageName = info.packageName;
        var signers = info.signingInfo.getApkContentsSigners();
        runOnUiThread(() -> {
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.msg_get_signatures, signers.length))
                    .setPositiveButton(R.string.add, (_1, _2) -> {
                        var list = new ArrayList<TrustedCert>();
                        for (var s: signers) {
                            var c = new TrustedCert();
                            c.enabled = false;
                            c.desc = packageName;
                            c.hex = s.toCharsString();
                            list.add(c);
                        }
                        addCerts(list);
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            loadingDialog.dismiss();
        });
    }

    private void handleAddCertsFromIntent() {
        var uri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
        if (!(uri instanceof Uri)) return;
        var loadingDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.title_adding_signatures)
                .setMessage(R.string.msg_loading)
                .setCancelable(false)
                .show();
        new Thread(() -> {
            try (var fd = getContentResolver().openFileDescriptor((Uri) uri, "r")) {
                assert fd != null;
                addCertFrom("/proc/self/fd/" + fd.getFd(), loadingDialog);
            } catch (Throwable t) {
                Log.e("CorePatch", "failed to resolve apk file from proc fd", t);
                var tmpPath = Paths.get(getCacheDir().getAbsolutePath(), "tmp.apk");
                try (var input = getContentResolver().openInputStream((Uri) uri)) {
                    Files.copy(input, tmpPath, StandardCopyOption.REPLACE_EXISTING);
                    addCertFrom(tmpPath.toString(), loadingDialog);
                } catch (Throwable t2) {
                    Log.e("CorePatch", "failed to resolve apk file from copied file", t2);
                    runOnUiThread(() -> {
                        loadingDialog.dismiss();
                        new AlertDialog.Builder(this)
                                .setMessage(R.string.msg_failed_to_resolve_apk)
                                .show();
                    });
                } finally {
                    try {
                        Files.delete(tmpPath);
                    } catch (IOException e) {
                        Log.e("CorePatch", "failed to remove copied file", e);
                    }
                }
            }
        }).start();
    }

    private void showEditDialog(TrustedCert cert) {
        var isAdd = cert == null;
        var root = LayoutInflater.from(this).inflate(R.layout.dialog_edit, null, false);
        var dialog = new AlertDialog.Builder(this)
                .setTitle(isAdd ? R.string.add : R.string.edit)
                .setView(root)
                .show();
        Button button = root.findViewById(R.id.ok);
        EditText descText = root.findViewById(R.id.desc);
        EditText signatureText = root.findViewById(R.id.signature);
        if (!isAdd) {
            descText.setText(cert.desc);
            signatureText.setText(cert.hex);
        }
        button.setOnClickListener((v) -> {
            var desc = descText.getText().toString().strip();
            var sig = signatureText.getText().toString();
            if (desc.isEmpty()) {
                Toast.makeText(this, R.string.desc_empty_toast, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!PATTERN_SIGNATURE.matcher(sig).matches()) {
                Toast.makeText(this, R.string.sig_not_hex_toast, Toast.LENGTH_SHORT).show();
                return;
            }
            var newCert = new TrustedCert();
            if (isAdd)
                newCert.enabled = false;
            else
                newCert.enabled = cert.enabled;
            newCert.desc = desc;
            newCert.hex = sig;
            var list = new ArrayList<>(this.list);
            if (cert != null) list.remove(cert);
            TrustedCertsUtils.addToCerts(list, newCert);
            TrustedCertsUtils.persistTrustedCerts(sp, list);
            dialog.dismiss();
        });
    }
}
