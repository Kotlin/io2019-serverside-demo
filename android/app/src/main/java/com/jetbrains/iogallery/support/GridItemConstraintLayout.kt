package com.jetbrains.iogallery.support

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout

class GridItemConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val heightSpec = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY, MeasureSpec.AT_MOST -> MeasureSpec.getSize(heightMeasureSpec)
            else -> Integer.MAX_VALUE
        }
        val measuredHeight = Math.min(measuredHeight, heightSpec)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }
}
