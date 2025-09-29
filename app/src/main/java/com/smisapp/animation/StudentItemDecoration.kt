package com.smisapp.animation

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class StudentItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        val position = parent.getChildAdapterPosition(view)

        // Add spacing to all items
        outRect.left = spacing
        outRect.right = spacing

        // Add top spacing only to first item
        if (position == 0) {
            outRect.top = spacing
        }

        // Add bottom spacing to all items
        outRect.bottom = spacing
    }
}