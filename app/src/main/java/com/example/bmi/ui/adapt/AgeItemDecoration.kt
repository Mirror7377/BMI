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

    }
}