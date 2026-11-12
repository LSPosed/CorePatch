package org.lsposed.corepatch.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.lsposed.corepatch.BuildConfig
import org.lsposed.corepatch.R
import org.lsposed.corepatch.ui.effect.BgEffectBackground
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val PROJECT_URL = "https://github.com/zjw2017/CorePatch"
private const val ISSUES_URL = "https://github.com/zjw2017/CorePatch/issues"
private const val LICENSE_URL = "https://github.com/zjw2017/CorePatch/blob/main/LICENSE"

/** About page on the animated gradient background. */
@Composable
fun AboutScreen(
    isCurrent: () -> Boolean,
    extraBottomPadding: Dp = 0.dp,
) {
    val uriHandler = LocalUriHandler.current
    Scaffold { innerPadding ->
        BgEffectBackground(
            // Only while this page is the one on screen: the shader animation loop invalidates
            // draw every frame, and the pager keeps neighbors composed one tab away.
            dynamicBackground = isCurrent(),
            isFullSize = true,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        // HyperMusicCover floats its logo 92dp below the top inset.
                        top = innerPadding.calculateTopPadding() + 92.dp,
                        bottom = innerPadding.calculateBottomPadding() + extraBottomPadding + 12.dp,
                    ),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_app_logo),
                        contentDescription = null,
                        modifier = Modifier.size(96.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MiuixTheme.textStyles.title1,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "v" + BuildConfig.VERSION_NAME,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    // The gap that puts the link cards in the lower half of the page, the
                    // same 126dp HyperMusicCover reserves under its floating logo.
                    Spacer(Modifier.height(126.dp))
                }
                Card(modifier = Modifier.nextCard()) {
                    ArrowPreference(
                        title = stringResource(R.string.about_source_code),
                        summary = PROJECT_URL,
                        onClick = { uriHandler.openUri(PROJECT_URL) },
                    )
                    ArrowPreference(
                        title = stringResource(R.string.about_feedback),
                        summary = ISSUES_URL,
                        onClick = { uriHandler.openUri(ISSUES_URL) },
                    )
                    ArrowPreference(
                        title = stringResource(R.string.about_license),
                        summary = "GPL-2.0",
                        onClick = { uriHandler.openUri(LICENSE_URL) },
                    )
                }
            }
        }
    }
}
