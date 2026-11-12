package org.lsposed.corepatch

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * The desktop icon, which is the `.LauncherAlias` entry in the manifest.
 *
 * The component's enabled state is the only record of whether it is hidden - there is no pref
 * beside it to drift out of step. Hiding it leaves MainActivity reachable through the
 * MODULE_SETTINGS filter LSPosed opens a module through.
 */
object LauncherIcon {

    private fun component(context: Context) =
        ComponentName(context, "${context.packageName}.LauncherAlias")

    fun isHidden(context: Context): Boolean =
        context.packageManager.getComponentEnabledSetting(component(context)) ==
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED

    fun setHidden(context: Context, hidden: Boolean) {
        context.packageManager.setComponentEnabledSetting(
            component(context),
            // ENABLED, not DEFAULT: going back to DEFAULT can leave the alias resolvable but
            // the launcher never puts the icon back.
            if (hidden) {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            },
            // Without this the package is killed on the change, closing the page the switch is on.
            PackageManager.DONT_KILL_APP,
        )
    }
}
