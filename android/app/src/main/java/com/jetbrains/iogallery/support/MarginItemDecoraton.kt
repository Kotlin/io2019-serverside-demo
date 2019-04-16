package com.jetbrains.iogallery.support

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class MarginItemDecoraton(private val itemSpacing: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        outRect.apply {
            top = itemSpacing
            left = itemSpacing
            right = 0
            bottom = if (position >= parent.adapter!!.itemCount - 2) itemSpacing else 0
        }
    }
}
