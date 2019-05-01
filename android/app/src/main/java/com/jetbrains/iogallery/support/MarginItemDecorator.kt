package com.jetbrains.iogallery.support

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class MarginItemDecorator(private val itemSpacing: Int, private val columnsCount: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        outRect.apply {
            left = itemSpacing
            top = if (position < columnsCount) itemSpacing else 0
            right = if (position % columnsCount != 0) itemSpacing else 0
            bottom = itemSpacing
        }
    }
}
