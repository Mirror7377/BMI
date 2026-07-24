package com.example.bmi.utils

import android.animation.ArgbEvaluator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.bmi.R
import kotlin.math.abs

object WheelPickerHelper {

    /**
     * 配置一个 RecyclerView 为滚轮选择器
     */
    fun setupWheelPicker(
        recyclerView: RecyclerView,
        context: Context,
        data: List<String>,
        defaultPosition: Int,
        onItemSelected: (Int) -> Unit,
        itemHeightDp: Int = 45,
        pickerHeightDp: Int = 315,
        lineColorRes: Int = R.color.splash_blue,
        lineWidthDp: Float = 0.5f,
        lineWidthDpDate: Int = 40,
        lineWidthDpTime: Int = 100,
        isTimePicker: Boolean = false
    ) {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = WheelAdapter(data) { position ->
                onItemSelected(position)
            }
            val itemHeight = dpToPx(context, itemHeightDp.toFloat())
            val totalHeight = dpToPx(context, pickerHeightDp.toFloat())
            val padding = ((totalHeight - itemHeight) / 2).toInt()
            setPadding(0, padding, 0, padding)
            clipToPadding = false

            val halfWidth = if (isTimePicker) {
                dpToPx(context, lineWidthDpTime.toFloat()) / 2
            } else {
                dpToPx(context, lineWidthDpDate.toFloat()) / 2
            }
            // 避免重复添加装饰器
            if (itemDecorationCount == 0) {
                addItemDecoration(WheelDividerDecoration(context, halfWidth, lineColorRes, lineWidthDp))
            }

            val snapHelper = LinearSnapHelper()
            snapHelper.attachToRecyclerView(this)

            post {
                (layoutManager as LinearLayoutManager).scrollToPosition(defaultPosition)
                snapHelper.findSnapView(layoutManager)?.let { view ->
                    layoutManager?.let { lm ->
                        val distance = snapHelper.calculateDistanceToFinalSnap(lm, view)
                        distance?.let { scrollBy(it[0], it[1]) }
                    }
                }
                updateWheelEffects(this)
            }

            // 移除旧监听器避免重复
            clearOnScrollListeners()
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    updateWheelEffects(recyclerView)
                }
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        snapHelper.findSnapView(layoutManager)?.let { view ->
                            val position = recyclerView.getChildAdapterPosition(view)
                            if (position != RecyclerView.NO_POSITION) {
                                onItemSelected(position)
                            }
                        }
                    }
                }
            })

            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (canScrollVertically(1) || canScrollVertically(-1)) {
                            parent?.requestDisallowInterceptTouchEvent(true)
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        parent?.requestDisallowInterceptTouchEvent(false)
                    }
                }
                false
            }
        }
    }

    // 更新效果
    fun updateWheelEffects(recyclerView: RecyclerView) {
        if (recyclerView.childCount == 0) return
        val firstChild = recyclerView.getChildAt(0)
        var itemHeight = firstChild?.height?.toFloat() ?: 0f
        if (itemHeight == 0f) {
            itemHeight = dpToPx(recyclerView.context, 45f)
        }
        val centerY = recyclerView.height / 2f
        val maxDistance = itemHeight * 2.5f
        val argbEvaluator = ArgbEvaluator()
        val startColor = Color.BLACK
        val endColor = 0xFFBBBBBB.toInt()

        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            val tv = child.findViewById<TextView>(R.id.tvWheelItem) ?: continue
            val childCenterY = child.top + child.height / 2f
            val distance = abs(childCenterY - centerY)
            val ratio = (distance / maxDistance).coerceIn(0f, 1f)
            tv.alpha = 1f - ratio * 0.75f
            val color = argbEvaluator.evaluate(ratio, startColor, endColor) as Int
            tv.setTextColor(color)
        }
    }

    // 适配器
    class WheelAdapter(
        private var data: List<String>,
        private val onItemSelected: (Int) -> Unit
    ) : RecyclerView.Adapter<WheelAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_wheel_picker, parent, false)
            return ViewHolder(view)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.tv.text = data[position]
        }
        override fun getItemCount(): Int = data.size
        fun updateData(newData: List<String>) {
            data = newData
            notifyDataSetChanged()
        }
        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tv: TextView = itemView.findViewById(R.id.tvWheelItem)
        }
    }

    // 装饰器
    class WheelDividerDecoration(
        context: Context,
        private val halfWidth: Float,
        colorRes: Int = R.color.splash_blue,
        lineWidthDp: Float = 0.5f
    ) : RecyclerView.ItemDecoration() {
        private val paint = Paint().apply {
            color = ContextCompat.getColor(context, colorRes)
            strokeWidth = lineWidthDp * context.resources.displayMetrics.density
            isAntiAlias = true
        }
        private val itemHeight = dpToPx(context, 45f)

        override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            val centerX = parent.width / 2f
            val centerY = parent.height / 2f
            c.drawLine(centerX - halfWidth, centerY - itemHeight / 2, centerX + halfWidth, centerY - itemHeight / 2, paint)
            c.drawLine(centerX - halfWidth, centerY + itemHeight / 2, centerX + halfWidth, centerY + itemHeight / 2, paint)
        }
    }

    private fun dpToPx(context: Context, dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics)
    }
}