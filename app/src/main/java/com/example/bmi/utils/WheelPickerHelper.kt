package com.example.bmi.utils

import android.animation.ArgbEvaluator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bmi.R
import com.example.bmi.databinding.ItemWheelPickerBinding
import kotlin.math.abs
import java.util.WeakHashMap
import androidx.core.view.isEmpty

/**
 * 滚轮选择器辅助工具
 * 基于 RecyclerView + LinearSnapHelper 实现，提供类似 iOS Picker Wheel 的交互体验
 */
object WheelPickerHelper {

    // ---------- 常量 ----------
    private const val DEFAULT_ITEM_HEIGHT_DP = 45
    private const val DEFAULT_PICKER_HEIGHT_DP = 315

    private var startColor: Int = 0
    private var endColor: Int = 0
    private val argbEvaluator = ArgbEvaluator()

    // 记录每个 RecyclerView 是否已完成配置（装饰、监听、SnapHelper），避免重复配置
    private val configuredMap = WeakHashMap<RecyclerView, Boolean>()

    /**
     * 配置一个 RecyclerView 为滚轮选择器
     * @param recyclerView     目标 RecyclerView
     * @param context          上下文
     * @param data             数据列表
     * @param defaultPosition  默认选中位置
     * @param onItemSelected   选中回调
     * @param itemHeightDp     每个选项的高度（dp）
     * @param pickerHeightDp   选择器总高度（dp）
     * @param lineColorRes     中间横线颜色资源
     * @param lineWidthDp      横线宽度（dp）
     * @param lineWidthDpDate  日期模式下横线宽度（dp）（用于半宽计算）
     * @param lineWidthDpTime  时间模式下横线宽度（dp）
     * @param isTimePicker     是否为时间选择器（影响横线宽度）
     */
    @SuppressLint("ClickableViewAccessibility")
    fun setupWheelPicker(
        recyclerView: RecyclerView,
        context: Context,
        data: List<String>,
        defaultPosition: Int,
        onItemSelected: (Int) -> Unit,
        itemHeightDp: Int = DEFAULT_ITEM_HEIGHT_DP,
        pickerHeightDp: Int = DEFAULT_PICKER_HEIGHT_DP,
        lineColorRes: Int = R.color.splash_blue,
        lineWidthDp: Float = 0.5f,
        lineWidthDpDate: Int = 40,
        lineWidthDpTime: Int = 100,
        isTimePicker: Boolean = false
    ) {
        // 初始化颜色缓存（仅一次）
        if (startColor == 0) {
            startColor = ContextCompat.getColor(context, R.color.bg_start)
            endColor = ContextCompat.getColor(context, R.color.bg_end)
        }

        // 停止所有滚动，避免冲突
        recyclerView.stopScroll()

        // 计算尺寸
        val itemHeightPx = dpToPx(context, itemHeightDp.toFloat())//每个选项的高度
        val pickerHeightPx = dpToPx(context, pickerHeightDp.toFloat())//滚轮整体的可见高度
        val padding = ((pickerHeightPx - itemHeightPx) / 2).toInt()//滚轮在屏幕上上下内边距


        val lm = (recyclerView.layoutManager as? LinearLayoutManager)
            ?: LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false).also {
                recyclerView.layoutManager = it
            }

        val adapter = if (recyclerView.adapter is WheelAdapter) {
            recyclerView.adapter as WheelAdapter
        } else {
            WheelAdapter().also { recyclerView.adapter = it }
        }

        // ---------- 判断是否已完成配置（只配置一次） ----------
        val isConfigured = configuredMap[recyclerView] == true

        if (!isConfigured) {
            // 设置内边距
            recyclerView.setPadding(0, padding, 0, padding)
            recyclerView.clipToPadding = false//取消裁剪

            // 添加装饰器（横线）
            val halfWidth = if (isTimePicker) {
                dpToPx(context, lineWidthDpTime.toFloat()) / 2
            } else {
                dpToPx(context, lineWidthDpDate.toFloat()) / 2
            }
            //装饰器
            recyclerView.addItemDecoration(
                WheelDividerDecoration(context, halfWidth, itemHeightDp, lineColorRes, lineWidthDp)
            )

            // ---------- 清除旧监听 ----------
            recyclerView.clearOnScrollListeners()

            // ---------- 添加滚动监听 ----------
            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    //设置渐变效果
                    updateWheelEffects(recyclerView)
                }


                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    // 滚动完全停止时
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        // 使用已附加的 SnapHelper 找到当前居中 View
                        val snapHelper = recyclerView.onFlingListener as? LinearSnapHelper
                        if (snapHelper != null) {
                            val snapView = snapHelper.findSnapView(lm) ?: return
                            //计算出距离屏幕中心最近的那个子 View
                            val position = recyclerView.getChildAdapterPosition(snapView)
                            if (position != RecyclerView.NO_POSITION) {
                                //位置有效
                                onItemSelected(position)
                            }
                        }
                        // 确保效果更新
                        updateWheelEffects(recyclerView)
                    }
                }
            })

            //确保只调用一次 从而保证滚轮的吸附行为稳定
            if (recyclerView.onFlingListener == null) {
                LinearSnapHelper().attachToRecyclerView(recyclerView)
            }

            recyclerView.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        //检查能否上下滑动
                        if (recyclerView.canScrollVertically(1) || recyclerView.canScrollVertically(-1)) {
                            recyclerView.parent?.requestDisallowInterceptTouchEvent(true)
                        }
                    }
                    //恢复父布局的拦截权限
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        recyclerView.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                }
                false
            }

            // 标记为已配置
            configuredMap[recyclerView] = true
        }

        // ---------- 数据提交与初始化滚动 提交新数据列表时 ----------
        adapter.submitList(data) {
            recyclerView.post {
                val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return@post

                //让 RecyclerView 滚动到目标位置
                lm.scrollToPosition(defaultPosition)

                recyclerView.post {
                    val targetView = lm.findViewByPosition(defaultPosition)
                    // 精准微调偏移量
                    if (targetView != null) {
                        val centerY = recyclerView.height / 2f
                        val childCenterY = targetView.top + targetView.height / 2f
                        val dy = (childCenterY - centerY).toInt()
                        recyclerView.scrollBy(0, dy)
                    }

                    recyclerView.post {
                        updateWheelEffects(recyclerView)
                        onItemSelected(defaultPosition)
                    }
                }
            }
        }
    }

    // ---------- 更新渐变效果（中间加深，两端淡化） ----------
    fun updateWheelEffects(recyclerView: RecyclerView) {
        if (recyclerView.isEmpty()) return

        val firstChild = recyclerView.getChildAt(0)
        //获取列表顶部显示的那一项item的实际的高度
        var itemHeight = firstChild?.height?.toFloat() ?: 0f
        if (itemHeight == 0f) {
            itemHeight = dpToPx(recyclerView.context, DEFAULT_ITEM_HEIGHT_DP.toFloat())
        }

        val centerY = recyclerView.height / 2f
        val maxDistance = itemHeight * 2.5f

        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            val holder = recyclerView.getChildViewHolder(child) as? WheelAdapter.ViewHolder
            val tv = holder?.binding?.tvWheelItem
            //当前 item 的垂直中心坐标
            val childCenterY = child.top + child.height / 2f
            val distance = abs(childCenterY - centerY)
            val ratio = (distance / maxDistance).coerceIn(0f, 1f)

            tv?.let { it.alpha = 1f - ratio * 0.75f }
            val color = argbEvaluator.evaluate(ratio, startColor, endColor) as Int
            tv?.setTextColor(color)
        }
    }

    // ---------- 工具方法 ----------
    private fun dpToPx(context: Context, dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics)
    }

    // ---------- Adapter ----------
    class WheelAdapter : ListAdapter<String, WheelAdapter.ViewHolder>(DiffCallback()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemWheelPickerBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.binding.tvWheelItem.text = getItem(position)
        }

        class ViewHolder(
            val binding: ItemWheelPickerBinding
        ) : RecyclerView.ViewHolder(binding.root)

        class DiffCallback : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
        }
    }

    // ---------- 装饰器（中间两条横线） ----------
    class WheelDividerDecoration(
        context: Context,
        private val halfWidth: Float,
        itemHeightDp: Int,
        colorRes: Int = R.color.splash_blue,
        lineWidthDp: Float = 0.5f
    ) : RecyclerView.ItemDecoration() {

        private val paint = Paint().apply {
            color = ContextCompat.getColor(context, colorRes)
            strokeWidth = dpToPx(context, lineWidthDp)
            isAntiAlias = true//抗锯齿
        }

        private val itemHeight = dpToPx(context, itemHeightDp.toFloat())

        override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            val centerX = parent.width / 2f// 屏幕水平中心点
            val centerY = parent.height / 2f// 屏幕垂直中心点

            // 上横线
            c.drawLine(
                centerX - halfWidth, centerY - itemHeight / 2,
                centerX + halfWidth, centerY - itemHeight / 2,
                paint
            )
            // 下横线
            c.drawLine(
                centerX - halfWidth, centerY + itemHeight / 2,
                centerX + halfWidth, centerY + itemHeight / 2,
                paint
            )
        }
    }
}