package com.example.bmi.ui.adapt

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class AgeItemDecoration(
    private val space: Int
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {

        val half = space / 2

        outRect.left = half
        outRect.right = half

        // 第一个Item左边不要留9dp
        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.left = 0
        }

        // 最后一个Item右边不要留9dp
        if (parent.getChildAdapterPosition(view) == state.itemCount - 1) {
            outRect.right = 0
        }
    }
}