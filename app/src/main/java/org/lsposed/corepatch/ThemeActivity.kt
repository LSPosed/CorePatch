package org.lsposed.corepatch

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.lsposed.corepatch.ui.AppTheme
import org.lsposed.corepatch.ui.ThemeScreen
import org.lsposed.corepatch.ui.UiConfig

/**
 * The theme settings, as a screen of its own - the same choice HyperMusicCover makes:
 * a whole screen arriving is the platform's own transition and brings its own back handling,
 * and it naturally has no bottom navigation bar.
 */
class ThemeActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val language = LocaleHelper.getSavedLanguage(newBase)
        super.attachBaseContext(LocaleHelper.wrapContext(newBase, language))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val uiConfig = UiConfig(this)
        setContent {
            // The theme screen's subject is the theme it is itself drawn in, so every change is
            // saved and recomposed at once - a change you could only see by going back would be
            // a change made blind.
            var colorMode by remember { mutableIntStateOf(uiConfig.colorSchemeMode) }
            var isFloatingNavbar by remember { mutableStateOf(uiConfig.isFloatingNavbar) }
            var isLiquidGlass by remember { mutableStateOf(uiConfig.isLiquidGlass) }
            var isBlurEnabled by remember { mutableStateOf(uiConfig.isBlurEnabled) }

            AppTheme(colorMode = colorMode) {
                ThemeScreen(
                    colorMode = colorMode,
                    onColorModeChange = {
                        colorMode = it
                        uiConfig.colorSchemeMode = it
                    },
                    isFloatingNavbar = isFloatingNavbar,
                    onFloatingNavbarChange = {
                        isFloatingNavbar = it
                        uiConfig.isFloatingNavbar = it
                    },
                    isLiquidGlass = isLiquidGlass,
                    onLiquidGlassChange = {
                        isLiquidGlass = it
                        uiConfig.isLiquidGlass = it
                    },
                    isBlurEnabled = isBlurEnabled,
                    onBlurEnabledChange = {
                        isBlurEnabled = it
                        uiConfig.isBlurEnabled = it
                    },
                    onBack = { finish() },
                )
            }
        }
    }
}
