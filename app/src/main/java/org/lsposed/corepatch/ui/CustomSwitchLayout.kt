package org.lsposed.corepatch.ui

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Switch
import android.widget.TextView
import org.lsposed.corepatch.Constant

class CustomSwitchLayout(context: Context, attrs: AttributeSet? = null) :
    CustomViewGroup(context, attrs) {

    val titleView = TextView(context).apply {
        if (this.isInEditMode) text = Constant.TOOLS_TEXT
        setTextAppearance(android.R.style.TextAppearance_Medium)
        layoutParams = MarginLayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            topMargin = 8.dp
            leftMargin = 16.dp
            rightMargin = 16.dp
        }
        this@CustomSwitchLayout.addView(this)
    }
    val subtitleView = TextView(context).apply {
        if (this.isInEditMode) text = Constant.TOOLS_TEXT
        layoutParams = MarginLayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            leftMargin = 16.dp
            bottomMargin = 8.dp
            rightMargin = 16.dp
        }
        this@CustomSwitchLayout.addView(this)
    }
    val switchView = Switch(context).apply {
        layoutParams = MarginLayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            leftMargin = 8.dp
            rightMargin = 8.dp
        }
        this@CustomSwitchLayout.addView(this)
    }

    init {
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        setBackgroundResource(outValue.resourceId)
        setOnClickListener { switchView.toggle() }
    }

    fun setOnCheckListener(listener: (Boolean) -> Unit) {
        switchView.setOnCheckedChangeListener { _, isChecked ->
            listener(isChecked)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        switchView.measure(
            measuredWidth.toAtMostMeasureSpec(), measuredHeight.toAtMostMeasureSpec()
        )
        val titleWidth =
            measuredWidth - switchView.measuredWidth - switchView.marginStart - switchView.marginEnd - titleView.marginStart - titleView.marginEnd

        titleView.measure(
            titleWidth.toExactlyMeasureSpec(), titleView.defaultHeightMeasureSpec(this)
        )
        subtitleView.measure(
            titleWidth.toExactlyMeasureSpec(), subtitleView.defaultHeightMeasureSpec(this)
        )

        val totalHeight =
            (titleView.marginTop + titleView.measuredHeight + subtitleView.measuredHeight + subtitleView.marginBottom).coerceAtLeast(
                switchView.measuredHeight
            )

        setMeasuredDimension(measuredWidth, totalHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val titleY = if (subtitleView.text.isNullOrEmpty()) {
            (height / 2) - (titleView.measuredHeight / 2)
        } else {
            titleView.marginTop
        }
        titleView.autoLayout(titleView.marginStart, titleY)
        subtitleView.autoLayout(titleView.marginStart, titleView.bottom)
        switchView.autoLayout(
            this@CustomSwitchLayout.measuredWidth - switchView.marginStart - switchView.measuredWidth,
            (height / 2) - (switchView.measuredHeight / 2)
        )
    }

}