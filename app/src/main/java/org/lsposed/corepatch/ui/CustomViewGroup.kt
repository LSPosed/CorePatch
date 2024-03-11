package org.lsposed.corepatch.ui

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT

// thanks Drakeet
val Int.dp
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), Resources.getSystem().displayMetrics
    ).toInt()

val Float.sp
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, this, Resources.getSystem().displayMetrics
    )

// https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:core/core-ktx/src/main/java/androidx/core/view/View.kt
inline val View.marginStart: Int
    get() {
        val lp = layoutParams
        return if (lp is ViewGroup.MarginLayoutParams) lp.marginStart else 0
    }
inline val View.marginEnd: Int
    get() {
        val lp = layoutParams
        return if (lp is ViewGroup.MarginLayoutParams) lp.marginEnd else 0
    }
inline val View.marginTop: Int
    get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin ?: 0
inline val View.marginBottom: Int
    get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0


abstract class CustomViewGroup(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    protected val View.measuredWidthWithMargins
        get() = measuredWidth + marginStart + marginEnd

    protected val View.measuredHeightWithMargins
        get() = measuredHeight + marginTop + marginBottom

    protected fun Int.toExactlyMeasureSpec() =
        MeasureSpec.makeMeasureSpec(this, MeasureSpec.EXACTLY)

    protected fun Int.toAtMostMeasureSpec() = MeasureSpec.makeMeasureSpec(this, MeasureSpec.AT_MOST)

    protected fun View.defaultWidthMeasureSpec(parent: ViewGroup): Int {
        return when (layoutParams.width) {
            MATCH_PARENT -> parent.measuredWidth.toExactlyMeasureSpec()
            WRAP_CONTENT -> WRAP_CONTENT.toAtMostMeasureSpec()
            0 -> throw IllegalAccessException("我不考虑这种情况 $this")
            else -> layoutParams.width.toExactlyMeasureSpec()
        }
    }

    protected fun View.defaultHeightMeasureSpec(parent: ViewGroup): Int {
        return when (layoutParams.height) {
            MATCH_PARENT -> parent.measuredHeight.toExactlyMeasureSpec()
            WRAP_CONTENT -> WRAP_CONTENT.toAtMostMeasureSpec()
            0 -> throw IllegalAccessException("我不考虑这种情况 $this")
            else -> layoutParams.height.toExactlyMeasureSpec()
        }
    }

    protected fun View.autoMeasure() {
        measure(
            this.defaultWidthMeasureSpec(this@CustomViewGroup),
            this.defaultHeightMeasureSpec(this@CustomViewGroup)
        )
    }

    protected fun View.autoLayout(x: Int = 0, y: Int = 0) {
        if (!isRTL) {
            layout(x, y, x + measuredWidth, y + measuredHeight)
        } else {
            val newX = this@CustomViewGroup.measuredWidth - x - measuredWidth
            layout(newX, y, newX + measuredWidth, y + measuredHeight)
        }
    }

    private inline val isRTL: Boolean
        get() = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
}