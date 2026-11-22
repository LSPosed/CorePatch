package org.lsposed.corepatch.ui

import android.content.Context

/** The app's own display settings, kept in local preferences. */
class UiConfig(context: Context) {
    private val prefs = context.getSharedPreferences("ui", Context.MODE_PRIVATE)

    var colorSchemeMode: Int
        get() = prefs.getInt(PREF_COLOR_SCHEME_MODE, DEFAULT_COLOR_MODE)
        set(value) {
            prefs.edit().putInt(PREF_COLOR_SCHEME_MODE, value).apply()
        }

    var isFloatingNavbar: Boolean
        get() = prefs.getBoolean(PREF_FLOATING_NAVBAR, true)
        set(value) {
            prefs.edit().putBoolean(PREF_FLOATING_NAVBAR, value).apply()
        }

    var isLiquidGlass: Boolean
        get() = prefs.getBoolean(PREF_LIQUID_GLASS, true)
        set(value) {
            prefs.edit().putBoolean(PREF_LIQUID_GLASS, value).apply()
        }

    var isBlurEnabled: Boolean
        get() = prefs.getBoolean(PREF_BLUR_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(PREF_BLUR_ENABLED, value).apply()
        }

    private companion object {
        const val PREF_COLOR_SCHEME_MODE = "color_scheme_mode"
        const val PREF_FLOATING_NAVBAR = "is_floating_navbar"
        const val PREF_LIQUID_GLASS = "is_liquid_glass"
        const val PREF_BLUR_ENABLED = "is_blur_enabled"
    }
}
