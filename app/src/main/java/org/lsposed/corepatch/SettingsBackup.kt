package org.lsposed.corepatch

import android.content.Context
import org.json.JSONObject
import org.lsposed.corepatch.ui.UiConfig

/**
 * Export and import of everything the user has set, which lives in two places: the app's own
 * [org.lsposed.corepatch.ui.UiConfig] preferences and the module's parameters in the remote
 * preferences group ([Config]). A file written when the module is not answering still imports -
 * the module's keys are simply absent and nothing is pushed.
 */
object SettingsBackup {

    private val MODULE_KEYS = arrayOf(
        Config.BYPASS_DOWNGRADE,
        Config.BYPASS_MIN_SDK,
        Config.BYPASS_VERIFICATION,
        Config.BYPASS_RESOURCE_ARSC_RESTRICTIONS,
        Config.BYPASS_DIGEST,
        Config.BYPASS_EXACT_SIGNATURE_MATCH,
        Config.USE_PREVIOUS_SIGNATURES,
        Config.ALLOW_HIDDEN_APIS_FOR_SYSTEM_APPS,
        Config.BYPASS_SHARED_USER,
        Config.DISABLE_VERIFICATION_AGENT,
        Config.BYPASS_BLOCK,
    )

    fun export(context: Context): String {
        val json = JSONObject()
        with(UiConfig(context)) {
            json.put("colorSchemeMode", colorSchemeMode)
            json.put("floatingNavbar", isFloatingNavbar)
            json.put("liquidGlass", isLiquidGlass)
            json.put("blurEnabled", isBlurEnabled)
        }
        json.put("language", LocaleHelper.getSavedLanguage(context).code)
        MODULE_KEYS.forEach { key ->
            json.put(key, Config.getConfig(key))
        }
        return json.toString(2)
    }

    fun import(context: Context, json: String) {
        val obj = JSONObject(json)
        with(UiConfig(context)) {
            if (obj.has("colorSchemeMode")) colorSchemeMode = obj.getInt("colorSchemeMode")
            if (obj.has("floatingNavbar")) isFloatingNavbar = obj.getBoolean("floatingNavbar")
            if (obj.has("liquidGlass")) isLiquidGlass = obj.getBoolean("liquidGlass")
            if (obj.has("blurEnabled")) isBlurEnabled = obj.getBoolean("blurEnabled")
        }
        if (obj.has("language")) {
            val code = obj.getString("language")
            val language = LocaleHelper.Language.entries.find { it.code == code }
                ?: LocaleHelper.Language.SYSTEM
            LocaleHelper.setLanguage(context, language)
        }
        MODULE_KEYS.forEach { key ->
            if (obj.has(key)) Config.setConfig(key, obj.getBoolean(key))
        }
    }
}
