package org.lsposed.corepatch.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.lsposed.corepatch.LauncherIcon
import org.lsposed.corepatch.LocaleHelper
import org.lsposed.corepatch.R
import org.lsposed.corepatch.SettingsBackup
import org.lsposed.corepatch.ThemeActivity
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

/** Interface, language and data management settings. */
@Composable
fun SettingsScreen(
    isBlurEnabled: Boolean,
    extraBottomPadding: Dp = 0.dp,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            try {
                val json = SettingsBackup.export(context)
                context.contentResolver.openOutputStream(it)?.use { out ->
                    out.write(json.toByteArray())
                }
                Toast.makeText(context, R.string.export_success, Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, R.string.export_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val json = context.contentResolver.openInputStream(it)?.use { stream ->
                    stream.readBytes().decodeToString()
                } ?: error("no stream")
                SettingsBackup.import(context, json)
                Toast.makeText(context, R.string.import_success, Toast.LENGTH_SHORT).show()
                activity?.recreate()
            } catch (_: Exception) {
                Toast.makeText(context, R.string.import_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    PageScaffold(
        title = stringResource(R.string.tab_settings),
        isBlurEnabled = isBlurEnabled,
        extraBottomPadding = extraBottomPadding,
    ) {
        item {
            Column {
                SmallTitle(text = stringResource(R.string.settings_interface))
                Card(modifier = Modifier.nextCard()) {
                    ArrowPreference(
                        title = stringResource(R.string.theme_title),
                        summary = stringResource(R.string.theme_title_summary),
                        onClick = {
                            context.startActivity(Intent(context, ThemeActivity::class.java))
                        },
                    )
                    var iconHidden by remember { mutableStateOf(LauncherIcon.isHidden(context)) }
                    SwitchPreference(
                        title = stringResource(R.string.hide_launcher_icon),
                        summary = stringResource(R.string.hide_launcher_icon_summary),
                        checked = iconHidden,
                        onCheckedChange = {
                            LauncherIcon.setHidden(context, it)
                            iconHidden = it
                        },
                    )
                }

                SmallTitle(text = stringResource(R.string.settings_language))
                Card(modifier = Modifier.nextCard()) {
                    val languageNames = listOf(
                        stringResource(R.string.language_default),
                        stringResource(R.string.language_zh_cn),
                        stringResource(R.string.language_zh_tw),
                        stringResource(R.string.language_en),
                        stringResource(R.string.language_el),
                        stringResource(R.string.language_fr),
                        stringResource(R.string.language_id),
                        stringResource(R.string.language_it),
                        stringResource(R.string.language_ja),
                        stringResource(R.string.language_ko),
                        stringResource(R.string.language_pt_br),
                        stringResource(R.string.language_ru),
                        stringResource(R.string.language_tr),
                    )
                    val languageValues = listOf(
                        LocaleHelper.Language.SYSTEM,
                        LocaleHelper.Language.ZH_CN,
                        LocaleHelper.Language.ZH_TW,
                        LocaleHelper.Language.EN,
                        LocaleHelper.Language.EL,
                        LocaleHelper.Language.FR,
                        LocaleHelper.Language.ID,
                        LocaleHelper.Language.IT,
                        LocaleHelper.Language.JA,
                        LocaleHelper.Language.KO,
                        LocaleHelper.Language.PT_BR,
                        LocaleHelper.Language.RU,
                        LocaleHelper.Language.TR,
                    )
                    val currentLanguage = LocaleHelper.getSavedLanguage(context)
                    val langIndex = languageValues.indexOf(currentLanguage).takeIf { it >= 0 } ?: 0
                    var langSelected by remember { mutableStateOf(langIndex) }
                    OverlayDropdownPreference(
                        title = stringResource(R.string.settings_language),
                        summary = languageNames[langSelected],
                        items = languageNames,
                        selectedIndex = langSelected,
                        onSelectedIndexChange = { index ->
                            langSelected = index
                            LocaleHelper.setLanguage(context, languageValues[index])
                            activity?.recreate()
                        },
                    )
                }

                SmallTitle(text = stringResource(R.string.settings_data_management))
                Card(modifier = Modifier.nextCard()) {
                    ArrowPreference(
                        title = stringResource(R.string.settings_export),
                        summary = stringResource(R.string.settings_export_summary),
                        onClick = { exportLauncher.launch("corepatch-settings.json") },
                    )
                    ArrowPreference(
                        title = stringResource(R.string.settings_import),
                        summary = stringResource(R.string.settings_import_summary),
                        onClick = { importLauncher.launch("application/json") },
                    )
                }
            }
        }
    }
}
