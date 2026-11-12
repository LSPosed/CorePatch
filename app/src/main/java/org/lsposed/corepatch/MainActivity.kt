package org.lsposed.corepatch

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.lsposed.corepatch.ui.AppTheme
import org.lsposed.corepatch.ui.MainScreen
import org.lsposed.corepatch.ui.UiConfig
import org.lsposed.corepatch.ui.isDarkColorMode

class MainActivity : ComponentActivity() {

    /**
     * Bumped every time this activity comes back to the front and read as a `remember` key in
     * [setContent]: the theme screen is an activity of its own, so it saves what it changes while
     * this one is stopped, and the settings read in [onCreate] would otherwise leave the app drawn
     * in the theme it had on the way out until the next cold start.
     */
    private var settingsEpoch by mutableStateOf(0)

    override fun attachBaseContext(newBase: Context) {
        val language = LocaleHelper.getSavedLanguage(newBase)
        super.attachBaseContext(LocaleHelper.wrapContext(newBase, language))
    }

    override fun onResume() {
        super.onResume()
        settingsEpoch++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Re-read on the way back from the theme screen and after a language change.
            val uiConfig = remember(settingsEpoch) { UiConfig(this@MainActivity) }
            var colorMode by remember(settingsEpoch) { mutableIntStateOf(uiConfig.colorSchemeMode) }
            var isFloatingNavbar by remember(settingsEpoch) { mutableStateOf(uiConfig.isFloatingNavbar) }
            var isLiquidGlass by remember(settingsEpoch) { mutableStateOf(uiConfig.isLiquidGlass) }
            var isBlurEnabled by remember(settingsEpoch) { mutableStateOf(uiConfig.isBlurEnabled) }

            val darkMode = isDarkColorMode(colorMode, isSystemInDarkTheme())
            DisposableEffect(darkMode) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
                    ) { darkMode },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
                    ) { darkMode },
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false // required on MIUI/HyperOS
                }
                onDispose {}
            }
            AppTheme(colorMode = colorMode) {
                MainScreen(
                    colorMode = colorMode,
                    isFloatingNavbar = isFloatingNavbar,
                    isLiquidGlass = isLiquidGlass,
                    isBlurEnabled = isBlurEnabled,
                )
            }
        }
    }
}
