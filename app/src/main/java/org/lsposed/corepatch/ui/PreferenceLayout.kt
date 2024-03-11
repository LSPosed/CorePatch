package org.lsposed.corepatch.ui

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import org.lsposed.corepatch.Constant

class PreferenceLayout(context: Context, attrs: AttributeSet? = null) :
    CustomViewGroup(context, attrs) {

    val titleView = TextView(context).apply {
        if (isInEditMode) text = Constant.TOOLS_TEXT
        setTextAppearance(android.R.style.TextAppearance_Medium)
        layoutParams = MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            topMargin = 8.dp
            leftMargin = 16.dp
            rightMargin = 16.dp
        }
        this@PreferenceLayout.addView(this)
    }

    val subtitleView = TextView(context).apply {
        if (isInEditMode) text = Constant.TOOLS_TEXT
        layoutParams = MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            leftMargin = 16.dp
            rightMargin = 16.dp
            bottomMargin = 8.dp
        }
        this@PreferenceLayout.addView(this)
    }

    init {
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        setBackgroundResource(outValue.resourceId)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = measuredWidth - titleView.marginStart - titleView.marginEnd
        titleView.let {
            it.measure(width.toExactlyMeasureSpec(), it.defaultHeightMeasureSpec(this))
        }
        subtitleView.let {
            it.measure(width.toExactlyMeasureSpec(), it.defaultHeightMeasureSpec(this))
        }

        setMeasuredDimension(
            measuredWidth,
            titleView.measuredHeightWithMargins + subtitleView.measuredHeightWithMargins
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val titleY = if (subtitleView.text.isNullOrEmpty()) {
            (height / 2) - (titleView.measuredHeight / 2)
        } else {
            titleView.marginTop
        }
        titleView.autoLayout(titleView.marginStart, titleY)
        subtitleView.autoLayout(subtitleView.marginStart, titleView.bottom)
    }
}