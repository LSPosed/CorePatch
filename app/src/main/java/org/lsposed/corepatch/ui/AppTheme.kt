package org.lsposed.corepatch.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

// Indices match the theme_color_scheme dropdown entries and SharedPreferences storage.
const val COLOR_MODE_SYSTEM = 0
const val COLOR_MODE_LIGHT = 1
const val COLOR_MODE_DARK = 2
const val COLOR_MODE_MONET_SYSTEM = 3
const val COLOR_MODE_MONET_LIGHT = 4
const val COLOR_MODE_MONET_DARK = 5

val COLOR_MODE_COUNT = 6

/** Default to Monet dynamic colors, falling back gracefully on older Android versions. */
const val DEFAULT_COLOR_MODE = COLOR_MODE_MONET_SYSTEM

fun colorSchemeModeOf(colorMode: Int): ColorSchemeMode = when (colorMode) {
    COLOR_MODE_LIGHT -> ColorSchemeMode.Light
    COLOR_MODE_DARK -> ColorSchemeMode.Dark
    COLOR_MODE_MONET_SYSTEM -> ColorSchemeMode.MonetSystem
    COLOR_MODE_MONET_LIGHT -> ColorSchemeMode.MonetLight
    COLOR_MODE_MONET_DARK -> ColorSchemeMode.MonetDark
    else -> ColorSchemeMode.System
}

/** Whether the given [colorMode] renders as dark, used for system bar styling. */
fun isDarkColorMode(colorMode: Int, systemDark: Boolean): Boolean = when (colorMode) {
    COLOR_MODE_LIGHT, COLOR_MODE_MONET_LIGHT -> false
    COLOR_MODE_DARK, COLOR_MODE_MONET_DARK -> true
    else -> systemDark
}

@Composable
fun AppTheme(
    colorMode: Int,
    content: @Composable () -> Unit,
) {
    // ThemeController's colorSchemeMode is read-only, so rebuild when the mode changes.
    val controller = remember(colorMode) { ThemeController(colorSchemeModeOf(colorMode)) }
    MiuixTheme(
        controller = controller,
        content = content,
    )
}
