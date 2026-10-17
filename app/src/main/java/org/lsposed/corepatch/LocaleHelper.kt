/*
 * Adapted from HyperMusicCover (https://github.com/zyl6932/HyperMusicCover),
 * itself adapted from HyperNavBar (https://github.com/HyperNavBar/HyperNavBar),
 * licensed under the Apache License, Version 2.0.
 *
 * Changes in CorePatch: package renamed and the pieces this project does not use removed.
 */
package org.lsposed.corepatch
import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {

    private const val PREFS_NAME = "app_prefs"
    private const val KEY_LANGUAGE = "language_code"

    enum class Language(val code: String) {
        SYSTEM(""),
        ZH_CN("zh-CN"),
        ZH_TW("zh-TW"),
        EN("en"),
        EL("el"),
        FR("fr"),
        ID("id"),
        IT("it"),
        JA("ja"),
        KO("ko"),
        PT_BR("pt-BR"),
        RU("ru"),
        TR("tr"),
    }

    fun getSavedLanguage(context: Context): Language {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val code = prefs.getString(KEY_LANGUAGE, "") ?: ""
        return Language.entries.find { it.code == code } ?: Language.SYSTEM
    }

    fun setLanguage(context: Context, language: Language) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language.code)
            .apply()
    }

    fun wrapContext(context: Context, language: Language): Context {
        if (language == Language.SYSTEM) return context
        val locale = Locale.forLanguageTag(language.code)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }
}
