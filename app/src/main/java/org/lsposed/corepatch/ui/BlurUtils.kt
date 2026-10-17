/*
 * Adapted from HyperMusicCover (https://github.com/zyl6932/HyperMusicCover),
 * itself adapted from HyperNavBar (https://github.com/HyperNavBar/HyperNavBar),
 * licensed under the Apache License, Version 2.0.
 *
 * Changes in CorePatch: package renamed and the pieces this project does not use removed.
 */
package org.lsposed.corepatch.ui

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 顶栏渐进模糊（Progressive Blur）统一参数。
 *
 * 所有页面的顶栏模糊都从这里读取，修改后重新编译即可全局生效。
 */
object TopBarBlurConfig {
    /** 模糊半径（dp），模糊最强处的强度 */
    const val BlurRadius: Float = 15f

    /** 渐变曲线指数：1 = 线性；>1 让模糊更快衰减到清晰端（示例为 2.2） */
    const val GradientCurve: Float = 10f

    /** 顶栏 surface 背景混合透明度（0~1），越大栏越实 */
    const val SurfaceAlpha: Float = 0.3f

    /**
     * 滚动渐显距离（dp）：内容下滑该距离内，模糊从透明渐显到完整。
     * 0 = 顶栏常驻完整模糊（推荐，各页面顶部即可见模糊）。
     */
    val ScrollFadeDistance: Dp = 0.dp
}

@Composable
fun rememberBlurBackdrop(): LayerBackdrop? {
    if (!isRuntimeShaderSupported() || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

@Composable
@ReadOnlyComposable
fun isInDarkTheme(): Boolean {
    val surface = MiuixTheme.colorScheme.surface
    // Calculate relative luminance to determine if surface is dark
    val luminance = 0.2126f * surface.red + 0.7152f * surface.green + 0.0722f * surface.blue
    return luminance < 0.5f
}

@Composable
fun BlurredBar(
    backdrop: LayerBackdrop?,
    blurEnabled: Boolean,
    scrollBehavior: ScrollBehavior? = null,
    content: @Composable () -> Unit,
) {
    val blurActive = blurEnabled && backdrop != null
    val scrollFadePx = with(LocalDensity.current) { TopBarBlurConfig.ScrollFadeDistance.toPx() }
    Box {
        if (blurActive) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        alpha = if (scrollFadePx > 0f) {
                            scrollBehavior?.state
                                ?.let { (-it.contentOffset / scrollFadePx).coerceIn(0f, 1f) }
                                ?: 1f
                        } else {
                            1f
                        }
                    }
                    .progressiveTextureBlur(
                        backdrop = backdrop,
                        shape = RectangleShape,
                        gradient = ProgressiveBlur.Top.copy(curve = TopBarBlurConfig.GradientCurve),
                        blurRadius = TopBarBlurConfig.BlurRadius,
                        colors = BlurDefaults.blurColors(
                            blendColors = listOf(
                                BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(TopBarBlurConfig.SurfaceAlpha)),
                            ),
                        ),
                    ),
            )
        }
        content()
    }
}

/**
 * The scroll behaviour every page's list shares. overScrollVertical is miuix's damped overscroll
 * and it REPLACES Compose's own - every caller must also pass `overscrollEffect = null` to the
 * list, or the stretch runs on top of the damping and the two fight at the ends.
 */
fun Modifier.pageScrollModifiers(
    showTopAppBar: Boolean,
    topAppBarScrollBehavior: ScrollBehavior,
): Modifier = this
    .scrollEndHaptic()
    .overScrollVertical()
    .then(if (showTopAppBar) Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection) else Modifier)

@Composable
fun pageContentPadding(
    innerPadding: PaddingValues,
    outerPadding: PaddingValues,
    isWideScreen: Boolean,
    extraTop: Dp = 0.dp,
    extraStart: Dp = 0.dp,
    extraEnd: Dp = 0.dp,
    extraBottom: Dp = 0.dp,
): PaddingValues {
    val topPadding = innerPadding.calculateTopPadding() + extraTop
    val bottomPadding = if (isWideScreen) {
        outerPadding.calculateBottomPadding() + extraBottom +
            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
            WindowInsets.captionBar.asPaddingValues().calculateBottomPadding()
    } else {
        outerPadding.calculateBottomPadding() + extraBottom
    }
    return remember(topPadding, bottomPadding, extraStart, extraEnd) {
        PaddingValues(
            top = topPadding,
            start = extraStart,
            end = extraEnd,
            bottom = bottomPadding,
        )
    }
}

@Composable
fun shouldShowSplitPane(): Boolean {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    return with(density) {
        val widthDp = windowInfo.containerSize.width.toDp()
        val heightDp = windowInfo.containerSize.height.toDp()
        val ratio = heightDp / widthDp
        widthDp >= 840.dp || (widthDp >= 600.dp && ratio < 1.2f)
    }
}

object ColorBlendToken {
    val Pured_Regular_Light = listOf(
        BlendColorEntry(Color(0x340034F9), BlurBlendMode.Overlay),
        BlendColorEntry(Color(0xB3FFFFFF), BlurBlendMode.HardLight),
    )
    val Overlay_Thin_Light = listOf(
        BlendColorEntry(Color(0x4DA9A9A9), BlurBlendMode.Luminosity),
        BlendColorEntry(Color(0x1A9C9C9C), BlurBlendMode.PlusDarker),
    )
}
