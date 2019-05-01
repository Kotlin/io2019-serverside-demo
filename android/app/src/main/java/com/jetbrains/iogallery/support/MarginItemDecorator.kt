package com.jetbrains.iogallery.support

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class MarginItemDecorator(private val itemSpacing: Int, private val columnsCount: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        outRect.apply {
            top = itemSpacing
            left = itemSpacing
            right = 0
            bottom = if (position >= parent.adapter!!.itemCount - columnsCount) itemSpacing else 0
        }
    }
}
