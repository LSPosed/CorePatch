package org.lsposed.corepatch.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.lsposed.corepatch.R
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

/** Color scheme, navigation bar style and background blur. */
@Composable
fun ThemeScreen(
    colorMode: Int,
    onColorModeChange: (Int) -> Unit,
    isFloatingNavbar: Boolean,
    onFloatingNavbarChange: (Boolean) -> Unit,
    isLiquidGlass: Boolean,
    onLiquidGlassChange: (Boolean) -> Unit,
    isBlurEnabled: Boolean,
    onBlurEnabledChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    extraBottomPadding: Dp = 0.dp,
) {
    val colorModeLabels = listOf(
        stringResource(R.string.theme_mode_system),
        stringResource(R.string.theme_mode_light),
        stringResource(R.string.theme_mode_dark),
        stringResource(R.string.theme_mode_monet_system),
        stringResource(R.string.theme_mode_monet_light),
        stringResource(R.string.theme_mode_monet_dark),
    )
    PageScaffold(
        title = stringResource(R.string.theme_title),
        isBlurEnabled = isBlurEnabled,
        extraBottomPadding = extraBottomPadding,
        onBack = onBack,
    ) {
        item {
            Column {
                Card(modifier = Modifier.firstCard()) {
                    OverlayDropdownPreference(
                        title = stringResource(R.string.theme_color_scheme),
                        items = colorModeLabels,
                        selectedIndex = colorMode,
                        onSelectedIndexChange = onColorModeChange,
                    )
                    SwitchPreference(
                        title = stringResource(R.string.nav_floating),
                        summary = stringResource(R.string.nav_floating_summary),
                        checked = isFloatingNavbar,
                        onCheckedChange = onFloatingNavbarChange,
                    )
                    SwitchPreference(
                        title = stringResource(R.string.nav_liquid_glass),
                        summary = stringResource(R.string.nav_liquid_glass_summary),
                        checked = isLiquidGlass,
                        onCheckedChange = onLiquidGlassChange,
                    )
                    SwitchPreference(
                        title = stringResource(R.string.blur_background),
                        summary = stringResource(R.string.blur_background_summary),
                        checked = isBlurEnabled,
                        onCheckedChange = onBlurEnabledChange,
                    )
                }
            }
        }
    }
}
