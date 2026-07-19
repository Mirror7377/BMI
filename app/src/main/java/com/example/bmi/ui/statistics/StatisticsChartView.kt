package com.example.bmi.ui.statistics

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.bmi.R
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 统计图表自定义 View
 * 支持：
 * - 显示 8 天数据，左右滑动切换
 * - 纵坐标 6 个值，5 等分，标签左对齐，距左边 11dp
 * - 圆点默认白色 6px，选中 8px 并变色
 * - 点击圆点显示 BMI 数值
 * - 月份标签随 1 号位置显隐
 */
class StatisticsChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ==================== 尺寸常量 ====================
    private companion object {
        // 卡片尺寸（外部容器已固定，View 撑满）
        const val CARD_WIDTH = 345f
        const val CARD_HEIGHT = 217.5f

        // 纵坐标（Y 轴）
        const val Y_PADDING_TOP = 29.5f          // 第一个数值距顶部
        const val Y_PADDING_BOTTOM = 37f         // 最后一个数值距底部
        const val Y_LABEL_COUNT = 6              // 6 个数值（5 等分）
        const val Y_LABEL_LEFT_MARGIN = 25f      // 纵坐标标签左边缘距卡片左边距 11dp

        // 横坐标（X 轴）
        const val X_PADDING_RIGHT = 20.5f        // 最后一个日期距右侧
        const val X_LABEL_COUNT = 8              // 显示 8 个日期

        // 月份标签
        const val MONTH_TOP_MARGIN = 14.5f       // 距顶部

        // 圆点
        const val DOT_RADIUS_DEFAULT = 3f        // 默认 6px 直径 → 3px 半径
        const val DOT_RADIUS_SELECTED = 4f       // 选中 8px 直径 → 4px 半径

        // 选中数值标签
        const val VALUE_LABEL_TOP_OFFSET = 8f    // 数值在圆点上方偏移
    }

    // ==================== 画笔 ====================
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.LEFT             // 改为左对齐
        typeface = resources.getFont(R.font.montserrat_extrabold)
        textSize = spToPx(12f)
    }

    private val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = resources.getFont(R.font.montserrat_extrabold)
        textSize = spToPx(11f)
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("montserrat", Typeface.BOLD)
        textSize = spToPx(12f)
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = 0x33
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = 0x55
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }

    // ==================== 数据 ====================
    private var allData: List<DayBmiData> = emptyList()
    private var startIndex: Int = 0
    private val displayCount = X_LABEL_COUNT

    private val displayData: List<DayBmiData>
        get() {
            val end = min(startIndex + displayCount, allData.size)
            return if (startIndex < allData.size) {
                allData.subList(startIndex, end)
            } else {
                emptyList()
            }
        }

    private val validBmiValues: List<Float>
        get() = displayData.mapNotNull { it.bmi }

    // 纵坐标范围
    private var yMin: Float = 0f
    private var yMax: Float = 0f
    private var yStep: Float = 0f

    // ==================== 选中状态 ====================
    private var selectedPosition: Int? = null
    private var selectedBmi: Float? = null

    // ==================== 尺寸 ====================
    private var viewWidth: Float = 0f
    private var viewHeight: Float = 0f

    private var yPaddingTopPx: Float = 0f
    private var yPaddingBottomPx: Float = 0f
    private var yLabelLeftPx: Float = 0f          // 纵坐标标签左边缘 X
    private var xPaddingRightPx: Float = 0f
    private var yAvailableHeight: Float = 0f
    private var xAvailableWidth: Float = 0f
    private var yInterval: Float = 0f
    private var xInterval: Float = 0f
    private var xStart: Float = 0f                // 横坐标起始 X

    // 月份标签位置
    private var monthLabelY: Float = 0f
    private var showMonthLabel: Boolean = false

    // ==================== 动画 ====================
    private var animator: ValueAnimator? = null

    // ==================== 监听器 ====================
    var onDataRangeChanged: ((startDate: String, endDate: String) -> Unit)? = null

    // ==================== 初始化 ====================
    init {
        isClickable = true
        isFocusable = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w.toFloat()
        viewHeight = h.toFloat()

        yPaddingTopPx = dpToPx(Y_PADDING_TOP)
        yPaddingBottomPx = dpToPx(Y_PADDING_BOTTOM)
        yLabelLeftPx = dpToPx(Y_LABEL_LEFT_MARGIN)
        xPaddingRightPx = dpToPx(X_PADDING_RIGHT)

        yAvailableHeight = viewHeight - yPaddingTopPx - yPaddingBottomPx
        yInterval = yAvailableHeight / (Y_LABEL_COUNT - 1)

        monthLabelY = dpToPx(MONTH_TOP_MARGIN) + monthPaint.textSize / 2

        recalculateYAxis()
        // 初始更新布局指标
        updateLayoutMetrics()
        invalidate()
    }

    // ==================== 数据设置 ====================
    fun setData(data: List<DayBmiData>) {
        this.allData = data.sortedBy { it.date.timeInMillis }
        this.startIndex = 0
        this.selectedPosition = null
        this.selectedBmi = null
        recalculateYAxis()
        invalidate()
    }

    fun scrollTo(offset: Int) {
        val newStart = (startIndex + offset).coerceIn(0, max(0, allData.size - displayCount))
        if (newStart != startIndex) {
            startIndex = newStart
            selectedPosition = null
            selectedBmi = null
            recalculateYAxis()
            invalidate()
            val startDate = displayData.firstOrNull()?.let { formatDate(it.date) } ?: ""
            val endDate = displayData.lastOrNull()?.let { formatDate(it.date) } ?: ""
            onDataRangeChanged?.invoke(startDate, endDate)
        }
    }

    fun canScrollLeft(): Boolean = startIndex > 0
    fun canScrollRight(): Boolean = startIndex + displayCount < allData.size

    // ==================== 纵坐标计算 ====================
    private fun recalculateYAxis() {
        val values = validBmiValues
        if (values.isEmpty()) {
            yMin = 0f
            yMax = 1f
            yStep = 0.2f
            updateLayoutMetrics()
            return
        }

        val minVal = values.minOrNull() ?: 0f
        val maxVal = values.maxOrNull() ?: 1f

        if (minVal == maxVal) {
            val half = 1f
            yMin = max(0f, minVal - half)
            yMax = minVal + half
        } else {
            val range = maxVal - minVal
            val extendedRange = range / 0.75f
            val mid = (minVal + maxVal) / 2f
            val halfRange = extendedRange / 2f
            yMin = max(0f, mid - halfRange)
            yMax = mid + halfRange
        }

        val rawStep = (yMax - yMin) / (Y_LABEL_COUNT - 1)
        yStep = (rawStep * 10).toInt().toFloat() / 10f
        if (yStep < 0.1f) yStep = 0.1f

        yMin = (yMin / yStep).toInt().toFloat() * yStep
        yMax = yMin + yStep * (Y_LABEL_COUNT - 1)

        updateLayoutMetrics()
    }

    // ==================== 布局指标更新 ====================
    private fun updateLayoutMetrics() {
        // 计算纵坐标标签的最大宽度（左对齐）
        val maxLabelWidth = calculateMaxYLabelWidth()
        // 纵坐标标签的右边缘 = 左边缘 + 最大宽度
        val yLabelRightX = yLabelLeftPx + maxLabelWidth

        // 横坐标起始 = 纵坐标标签右边缘 + 6dp
        xStart = yLabelRightX + dpToPx(6f)

        // 横坐标可用宽度
        xAvailableWidth = viewWidth - xStart - xPaddingRightPx
        xInterval = if (displayCount > 1) {
            xAvailableWidth / (displayCount - 1)
        } else {
            0f
        }
    }

    private fun calculateMaxYLabelWidth(): Float {
        var maxWidth = 0f
        for (i in 0 until Y_LABEL_COUNT) {
            val value = yMin + i * yStep
            val label = String.format("%.1f", value)
            val w = textPaint.measureText(label)
            if (w > maxWidth) maxWidth = w
        }
        return maxWidth
    }

    // ==================== 绘制 ====================
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (allData.isEmpty()) {
            drawEmptyState(canvas)
            return
        }

        val display = displayData
        if (display.isEmpty()) {
            drawEmptyState(canvas)
            return
        }

        drawYLabels(canvas)
        drawXLabels(canvas)
        drawMonthLabel(canvas)
        drawDataLine(canvas)
        drawDots(canvas)
        drawSelectedValue(canvas)
    }

    private fun drawEmptyState(canvas: Canvas) {
        textPaint.textSize = spToPx(14f)
        textPaint.color = Color.WHITE
        textPaint.alpha = 0x80
        canvas.drawText("No Data", viewWidth / 2, viewHeight / 2, textPaint)
        textPaint.alpha = 0xFF
        textPaint.textSize = spToPx(12f)
    }

    private fun drawYLabels(canvas: Canvas) {
        val values = validBmiValues
        if (values.isEmpty()) {
            // 占位刻度
            for (i in 0 until Y_LABEL_COUNT) {
                val y = yPaddingTopPx + i * yInterval
                val label = String.format("%.1f", yMax - i * yStep)
                canvas.drawText(label, yLabelLeftPx, y + textPaint.textSize / 3, textPaint)
            }
            return
        }

        for (i in 0 until Y_LABEL_COUNT) {
            val value = yMin + i * yStep
            val y = yPaddingTopPx + (Y_LABEL_COUNT - 1 - i) * yInterval
            val label = String.format("%.1f", value)
            canvas.drawText(label, yLabelLeftPx, y + textPaint.textSize / 3, textPaint)
        }
    }

    private fun drawXLabels(canvas: Canvas) {
        val display = displayData
        for (i in display.indices) {
            val x = xStart + i * xInterval
            val day = display[i].dayOfMonth
            val y = viewHeight - dpToPx(17.5f)
            canvas.drawText(day.toString(), x, y, textPaint)
        }
    }

    private fun drawMonthLabel(canvas: Canvas) {
        if (!showMonthLabel) return
        val index = displayData.indexOfFirst { it.dayOfMonth == 1 }
        if (index == -1) {
            showMonthLabel = false
            return
        }
        val x = xStart + index * xInterval
        val monthName = getMonthAbbr(displayData[index].month)
        canvas.drawText(monthName, x, monthLabelY, monthPaint)
    }

    private fun drawDataLine(canvas: Canvas) {
        val points = getDataPoints()
        if (points.size < 2) return
        val path = Path()
        path.moveTo(points[0].first, points[0].second)
        for (i in 1 until points.size) {
            path.lineTo(points[i].first, points[i].second)
        }
        canvas.drawPath(path, linePaint)
    }

    private fun drawDots(canvas: Canvas) {
        val points = getDataPoints()
        for (i in points.indices) {
            val (x, y) = points[i]
            val isSelected = selectedPosition == i
            val color = if (isSelected) {
                val bmi = displayData[i].bmi ?: continue
                getBmiColor(bmi)
            } else {
                Color.WHITE
            }
            dotPaint.color = color
            val radius = if (isSelected) {
                dpToPx(DOT_RADIUS_SELECTED)
            } else {
                dpToPx(DOT_RADIUS_DEFAULT)
            }
            canvas.drawCircle(x, y, radius, dotPaint)
        }
    }

    private fun drawSelectedValue(canvas: Canvas) {
        if (selectedPosition == null || selectedBmi == null) return
        val points = getDataPoints()
        if (selectedPosition!! >= points.size) return
        val (x, y) = points[selectedPosition!!]
        val label = String.format("%.1f", selectedBmi)
        val labelY = y - dpToPx(DOT_RADIUS_SELECTED) - dpToPx(VALUE_LABEL_TOP_OFFSET)
        canvas.drawText(label, x, labelY, valuePaint)
    }

    // ==================== 数据点计算 ====================
    private fun getDataPoints(): List<Pair<Float, Float>> {
        val display = displayData
        val points = mutableListOf<Pair<Float, Float>>()
        for (i in display.indices) {
            val data = display[i]
            if (data.bmi == null) continue
            val x = xStart + i * xInterval
            val ratio = (data.bmi - yMin) / (yMax - yMin)
            val y = yPaddingTopPx + (Y_LABEL_COUNT - 1 - ratio) * yInterval
            points.add(x to y)
        }
        return points
    }

    // ==================== 触摸事件 ====================
    private var downX: Float = 0f
    private var downY: Float = 0f
    private var isDragging = false
    private val touchSlop = dpToPx(10f)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - downX
                if (abs(dx) > touchSlop) {
                    isDragging = true
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    val dx = downX - event.x
                    if (abs(dx) > touchSlop) {
                        val direction = if (dx > 0) 1 else -1
                        scrollTo(direction)
                        isDragging = false
                        return true
                    }
                }
                if (!isDragging) {
                    handleClick(event.x, event.y)
                }
                isDragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleClick(x: Float, y: Float) {
        val points = getDataPoints()
        val radius = dpToPx(DOT_RADIUS_SELECTED) + dpToPx(6f)
        var hitIndex: Int? = null
        for (i in points.indices) {
            val (px, py) = points[i]
            val dx = x - px
            val dy = y - py
            if (dx * dx + dy * dy < radius * radius) {
                hitIndex = i
                break
            }
        }
        if (hitIndex != null) {
            val bmi = displayData[hitIndex].bmi
            if (bmi != null) {
                selectedPosition = hitIndex
                selectedBmi = bmi
                invalidate()
            }
        } else {
            selectedPosition = null
            selectedBmi = null
            invalidate()
        }
    }

    // ==================== 工具方法 ====================
    private fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density
    private fun spToPx(sp: Float): Float = sp * resources.displayMetrics.scaledDensity

    private fun getMonthAbbr(month: Int): String {
        return arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")[month]
    }

    private fun formatDate(calendar: Calendar): String {
        return "${getMonthAbbr(calendar.get(Calendar.MONTH))} ${calendar.get(Calendar.DAY_OF_MONTH)}"
    }

    private fun getBmiColor(bmi: Float): Int {
        return when {
            bmi < 16f -> 0xFF4343B8.toInt()
            bmi < 17f -> 0xFF1258E1.toInt()
            bmi < 18.5f -> 0xFF0099F2.toInt()
            bmi < 25f -> 0xFF54A529.toInt()
            bmi < 30f -> 0xFFFECD2E.toInt()
            bmi < 35f -> 0xFFFFA100.toInt()
            bmi < 40f -> 0xFFFF7137.toInt()
            else -> 0xFFD3333B.toInt()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        animator = null
    }

    fun refresh() {
        recalculateYAxis()
        invalidate()
    }

    fun getStartDate(): String? {
        return displayData.firstOrNull()?.let { formatDate(it.date) }
    }

    fun getEndDate(): String? {
        return displayData.lastOrNull()?.let { formatDate(it.date) }
    }
}