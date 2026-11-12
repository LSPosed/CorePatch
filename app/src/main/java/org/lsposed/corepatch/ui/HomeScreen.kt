package org.lsposed.corepatch.ui

import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.lsposed.corepatch.BuildConfig
import org.lsposed.corepatch.R
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Status card and device information. Laid out the way the reference project (HyperMusicCover)
 * lays out its home page, which follows KernelSU's HomeMiuix card.
 */
@Composable
fun HomeScreen(
    serviceAvailable: Boolean,
    isDynamicColor: Boolean,
    isBlurEnabled: Boolean,
    extraBottomPadding: Dp = 0.dp,
) {
    val context = LocalContext.current
    val unknown = stringResource(R.string.home_unknown)
    val deviceModel = remember(unknown) { Build.MODEL.ifEmpty { unknown } }
    val deviceName = remember(context, deviceModel) {
        Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
            ?: deviceModel
    }
    val androidVersion = remember {
        "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
    }
    val loadingText = stringResource(R.string.home_unknown)
    var hyperOSVersion by remember { mutableStateOf(loadingText) }
    LaunchedEffect(Unit) {
        hyperOSVersion = withContext(Dispatchers.IO) { SystemVersion.hyperOs(loadingText) }
    }

    PageScaffold(
        title = stringResource(R.string.tab_home),
        isBlurEnabled = isBlurEnabled,
        extraBottomPadding = extraBottomPadding,
    ) {
        item(key = "status") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .firstCard()
            ) {
                val darkTheme = isInDarkTheme()
                val statusColor = if (serviceAvailable) {
                    when {
                        isDynamicColor -> MiuixTheme.colorScheme.secondaryContainer
                        darkTheme -> Color(0xFF1A3825)
                        else -> Color(0xFFDFFAE4)
                    }
                } else {
                    when {
                        isDynamicColor -> MiuixTheme.colorScheme.errorContainer
                        darkTheme -> Color(0xFF3D1C1C)
                        else -> Color(0xFFFDE8E8)
                    }
                }
                val iconTint = if (serviceAvailable) {
                    if (isDynamicColor) MiuixTheme.colorScheme.primary.copy(alpha = 0.8f)
                    else Color(0xFF36D167)
                } else {
                    if (isDynamicColor) MiuixTheme.colorScheme.error.copy(alpha = 0.8f)
                    else Color(0xFFDC3545)
                }
                val titleText = stringResource(
                    if (serviceAvailable) R.string.home_status_active
                    else R.string.xposed_service_unavailable
                )
                val lineTwo = if (serviceAvailable) {
                    "v" + BuildConfig.VERSION_NAME
                } else {
                    stringResource(R.string.xposed_service_unavailable_summary)
                }

                // Three boxes stacked rather than a column: title and version top-left, the app
                // name pinned bottom-left, and the oversized icon bleeding off the bottom-right.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.defaultColors(color = statusColor),
                    ) {
                        Box {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .offset(27.dp, 31.dp),
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                Icon(
                                    modifier = Modifier.size(110.dp),
                                    imageVector = if (serviceAvailable) Icons.Rounded.CheckCircleOutline
                                    else Icons.Rounded.ErrorOutline,
                                    tint = iconTint,
                                    contentDescription = null
                                )
                            }
                            if (serviceAvailable) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp, 10.dp),
                                    contentAlignment = Alignment.BottomStart,
                                ) {
                                    MiuixText(
                                        text = stringResource(R.string.app_name),
                                        style = MiuixTheme.textStyles.body1,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp, 14.dp),
                                contentAlignment = Alignment.TopStart,
                            ) {
                                Column {
                                    MiuixText(
                                        text = titleText,
                                        style = MiuixTheme.textStyles.title3,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.height(1.dp))
                                    MiuixText(
                                        text = lineTwo,
                                        style = MiuixTheme.textStyles.body2,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item(key = "device") {
            Card(modifier = Modifier.nextCard()) {
                Column {
                    BasicComponent(
                        title = stringResource(R.string.home_device_name),
                        summary = deviceName,
                    )
                    BasicComponent(
                        title = stringResource(R.string.home_device_model),
                        summary = deviceModel,
                    )
                    BasicComponent(
                        title = stringResource(R.string.home_hyperos_version),
                        summary = hyperOSVersion,
                    )
                    BasicComponent(
                        title = stringResource(R.string.home_android_version),
                        summary = androidVersion,
                    )
                }
            }
        }
    }
}

private object SystemVersion {
    /** ro.* properties are the only place the HyperOS version is written down. */
    private fun prop(key: String): String = try {
        @Suppress("PrivateApi")
        val cls = Class.forName("android.os.SystemProperties")
        val get = cls.getMethod("get", String::class.java, String::class.java)
        (get.invoke(null, key, "") as? String).orEmpty()
    } catch (_: Throwable) {
        ""
    }

    fun hyperOs(fallback: String): String {
        val incremental = prop("ro.mi.os.version.incremental").ifEmpty { Build.DISPLAY }
        if (incremental.isNotEmpty()) return incremental
        val name = prop("ro.mi.os.version.name").ifEmpty { prop("ro.miui.ui.version.name") }
        return name.ifEmpty { fallback }
    }
}
