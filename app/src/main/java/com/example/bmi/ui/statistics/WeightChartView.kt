package com.example.bmi.ui.statistics

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.widget.OverScroller
import com.example.bmi.R
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class WeightChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ===== 枚举（新增 MONTH） =====
    enum class ChartMode { DAY, WEEK, MONTH }

    // ===== 常量 =====
    private companion object {
        const val Y_PADDING_TOP = 29.5f
        const val Y_PADDING_BOTTOM = 37f
        const val Y_LABEL_COUNT = 6
        const val Y_LABEL_LEFT_MARGIN = 25f

        const val X_PADDING_RIGHT = 20.5f
        const val X_LABEL_COUNT = 8

        const val MONTH_TOP_MARGIN = 14.5f

        const val DOT_RADIUS_NORMAL = 3f
        const val DOT_RADIUS_SELECTED = 4.5f

        const val VALUE_LABEL_TOP_OFFSET = 8f
    }

    // ===== 月份锚点（DAY / WEEK 使用） =====
    private val monthAnchors = mutableListOf<MonthAnchor>()

    private data class MonthAnchor(
        val month: Int,       // 0‑11
        val dataIndex: Int,   // 在 allData 中的索引
        var x: Float = 0f     // 像素坐标，由 updateMonthAnchorPositions() 计算
    )

    // ===== 画笔 =====
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.LEFT
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
        typeface = resources.getFont(R.font.montserrat_extrabold)
        textSize = spToPx(12f)
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val dotFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = 0x33
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val verticalGridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EEEEEE")
        style = Paint.Style.STROKE
        strokeWidth = 0.5f
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = 0x99
        strokeWidth = 4.0f
        style = Paint.Style.STROKE
    }

    // ===== 数据 =====
    private var allData: List<DayWeightData> = emptyList()
    private val displayCount = X_LABEL_COUNT
    private var scrollOffset = 0f
    private var minScrollX = 0f
    private var maxScrollX = 0f
    private val visibleStartIndex: Float get() = scrollOffset / xInterval

    private var weightMin = 0f
    private var weightMax = 1f
    private var weightStep = 0.2f

    private var selectedDataIndex: Int? = null
    private var selectedWeight: Float? = null

    private var viewWidth = 0f
    private var viewHeight = 0f
    private var yPaddingTopPx = 0f
    private var yPaddingBottomPx = 0f
    private var yLabelLeftPx = 0f
    private var xPaddingRightPx = 0f
    private var yAvailableHeight = 0f
    private var xAvailableWidth = 0f
    private var yInterval = 0f
    private var xInterval = 0f
    private var xStart = 0f
    private var monthLabelY = 0f
    private var showMonthLabel = false

    private val scroller = OverScroller(context)
    private var velocityTracker: VelocityTracker? = null
    private var isDragging = false
    private var lastTouchX = 0f
    private val touchSlop = dpToPx(10f)

    private var animator: ValueAnimator? = null
    var onDataRangeChanged: ((startDate: String, endDate: String) -> Unit)? = null

    // ===== 模式支持 =====
    private var chartMode = ChartMode.DAY
    fun setMode(mode: ChartMode) {
        if (chartMode != mode) {
            chartMode = mode
            invalidate()
        }
    }

    init {
        isClickable = true
        isFocusable = true
        setWillNotDraw(false)
    }

    // ===== 尺寸与布局 =====
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

        if (allData.isNotEmpty()) {
            computeFixedYAxis(allData)
        } else {
            weightMin = 0f
            weightMax = 1f
            weightStep = 0.2f
        }

        updateLayoutMetrics()
        updateScrollBounds()
        clampScrollOffset()
        invalidate()
    }

    // ===== 数据设置（路由到对应模式） =====
    fun setData(data: List<DayWeightData>) {
        when (chartMode) {
            ChartMode.DAY -> setDataDay(data)
            ChartMode.WEEK -> setDataWeek(data)
            ChartMode.MONTH -> setDataMonth(data)
        }
    }

    // ---------- DAY 模式 ----------
    private fun setDataDay(data: List<DayWeightData>) {
        val sorted = data.sortedBy { it.date.timeInMillis }
        val lastWithWeight = sorted.findLast { it.weight != null }

        if (lastWithWeight == null) {
            allData = emptyList()
            scrollOffset = 0f
            selectedDataIndex = null
            selectedWeight = null
            computeFixedYAxis(emptyList())
            updateLayoutMetrics()
            updateScrollBounds()
            clampScrollOffset()
            invalidate()
            return
        }

        val latestDate = lastWithWeight.date
        val cal = Calendar.getInstance().apply { time = latestDate.time }
        cal.add(Calendar.DAY_OF_YEAR, -58)
        val startDate = cal.clone() as Calendar
        cal.add(Calendar.DAY_OF_YEAR, 59)
        val endDate = cal

        val allDates = mutableListOf<DayWeightData>()
        var current = startDate.clone() as Calendar
        while (current <= endDate) {
            val weight = sorted.find {
                val d = it.date
                d.get(Calendar.YEAR) == current.get(Calendar.YEAR) &&
                        d.get(Calendar.DAY_OF_YEAR) == current.get(Calendar.DAY_OF_YEAR)
            }?.weight
            allDates.add(DayWeightData(current.clone() as Calendar, weight))
            current.add(Calendar.DAY_OF_YEAR, 1)
        }

        this.allData = allDates
        this.selectedDataIndex = null
        this.selectedWeight = null

        computeFixedYAxis(allDates)
        updateLayoutMetrics()

        val targetStart = (allData.size - displayCount).coerceAtLeast(0)
        this.scrollOffset = targetStart.toFloat() * xInterval

        updateScrollBounds()
        clampScrollOffset()
        invalidate()

        rebuildMonthAnchors()
        updateMonthAnchorPositions()
        notifyRangeChanged()
    }

    // ---------- WEEK 模式 ----------
    private fun setDataWeek(data: List<DayWeightData>) {
        allData = data.sortedBy { it.date.timeInMillis }
        selectedDataIndex = null
        selectedWeight = null

        computeFixedYAxis(allData)
        updateLayoutMetrics()

        val targetStart = (allData.size - displayCount).coerceAtLeast(0)
        scrollOffset = targetStart.toFloat() * xInterval
        updateScrollBounds()
        clampScrollOffset()

        invalidate()
        notifyRangeChanged()
        rebuildMonthAnchors()
        updateMonthAnchorPositions()
    }

    // ---------- MONTH 模式（新增） ----------
    private fun setDataMonth(data: List<DayWeightData>) {
        allData = data.sortedBy { it.date.timeInMillis }
        selectedDataIndex = null
        selectedWeight = null

        computeFixedYAxis(allData)
        updateLayoutMetrics()

        // 默认滚动到最后 8 个月
        val targetStart = (allData.size - displayCount).coerceAtLeast(0)
        scrollOffset = targetStart.toFloat() * xInterval
        updateScrollBounds()
        clampScrollOffset()

        invalidate()
        notifyRangeChanged()
        // MONTH 模式不使用月份锚点，清空即可
        monthAnchors.clear()
        updateMonthAnchorPositions()
    }

    // ===== Y轴计算 =====
    private fun computeFixedYAxis(data: List<DayWeightData>) {
        val weightList = data.mapNotNull { it.weight }

        if (weightList.isEmpty()) {
            weightMin = 0f
            weightMax = 1f
            weightStep = 0.2f
            return
        }

        val (min, max, step) = computeAxis(
            weightList.minOrNull() ?: 0f,
            weightList.maxOrNull() ?: 1f
        )

        weightMin = min
        weightMax = max
        weightStep = step
    }

    private fun computeAxis(
        minVal: Float,
        maxVal: Float
    ): Triple<Float, Float, Float> {
        if (minVal == maxVal) {
            val value = minVal
            val absVal = abs(value)
            val step = if (absVal < 4f) {
                max(absVal / 4f, 0.1f)
            } else {
                1f
            }
            val axisMin = value - step
            val axisMax = axisMin + step * (Y_LABEL_COUNT - 1)
            return Triple(axisMin, axisMax, step)
        }

        val dataSpan = maxVal - minVal
        val totalSpan = dataSpan / 0.75f
        val bottomBlank = totalSpan * 0.05f
        val topBlank = totalSpan * 0.20f

        var axisMin = minVal - bottomBlank
        var axisMax = maxVal + topBlank

        var step = (axisMax - axisMin) / (Y_LABEL_COUNT - 1)
        step = (step * 10).toInt().toFloat() / 10f
        if (step < 0.1f) step = 0.1f

        axisMin = kotlin.math.floor(axisMin / step).toFloat() * step
        axisMax = axisMin + step * (Y_LABEL_COUNT - 1)

        return Triple(axisMin, axisMax, step)
    }

    // ===== 滚动辅助 =====
    private fun updateScrollBounds() {
        if (allData.isEmpty()) {
            minScrollX = 0f
            maxScrollX = 0f
            return
        }
        val totalWidth = (allData.size - 1) * xInterval
        val visibleWidth = (displayCount - 1) * xInterval
        maxScrollX = max(0f, totalWidth - visibleWidth)
        minScrollX = 0f
    }

    private fun clampScrollOffset() {
        scrollOffset = scrollOffset.coerceIn(minScrollX, maxScrollX)
    }

    // ===== 日期范围回调（MONTH 模式特殊格式） =====
    private fun notifyRangeChanged() {
        if (allData.isEmpty()) return

        val startIdx = visibleStartIndex.toInt()
        if (startIdx < 0 || startIdx >= allData.size) return

        val endIdx = min(startIdx + displayCount - 1, allData.size - 1)
        val startDate = allData[startIdx].date
        val endDate = allData[endIdx].date

        val rangeStr = when (chartMode) {
            ChartMode.DAY, ChartMode.WEEK -> {
                "${formatDate(startDate)} ~ ${formatDate(endDate)}"
            }
            ChartMode.MONTH -> {
                "${getMonthAbbr(startDate.get(Calendar.MONTH))} ${startDate.get(Calendar.YEAR)} ~ " +
                        "${getMonthAbbr(endDate.get(Calendar.MONTH))} ${endDate.get(Calendar.YEAR)}"
            }
        }
        onDataRangeChanged?.invoke(rangeStr, rangeStr)
    }

    private fun updateLayoutMetrics() {
        val maxLabelWidth = calculateMaxYLabelWidth()
        val yLabelRightX = yLabelLeftPx + maxLabelWidth
        xStart = yLabelRightX + dpToPx(6f)
        xAvailableWidth = viewWidth - xStart - xPaddingRightPx
        xInterval = if (displayCount > 1) {
            xAvailableWidth / (displayCount - 1)
        } else {
            0f
        }
        updateScrollBounds()
        updateMonthAnchorPositions()
    }

    private fun calculateMaxYLabelWidth(): Float {
        var maxWidth = 0f
        for (i in 0 until Y_LABEL_COUNT) {
            val value = weightMin + i * weightStep
            val label = String.format("%.1f", value)
            val w = textPaint.measureText(label)
            if (w > maxWidth) maxWidth = w
        }
        return maxWidth
    }

    // ===== 绘制主流程 =====
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (allData.isEmpty()) {
            drawEmptyState(canvas)
            return
        }
        drawYLabels(canvas)

        val clipLeft = xStart
        val clipTop = 0f
        val clipRight = viewWidth
        val clipBottom = viewHeight
        canvas.save()
        canvas.clipRect(clipLeft, clipTop, clipRight, clipBottom)

        val startIdx = visibleStartIndex.toInt()
        val endIdx = min(startIdx + displayCount + 1, allData.size)
        val visibleSubList = allData.subList(startIdx, endIdx)
        canvas.translate(-scrollOffset, 0f)

        drawVerticalGridLines(canvas, startIdx, visibleSubList)
        drawFillArea(canvas, startIdx, visibleSubList)
        drawXLabels(canvas, startIdx, visibleSubList)
        drawMonthLabel(canvas, startIdx, endIdx, visibleSubList)
        drawDataLine(canvas, startIdx, visibleSubList)
        drawDots(canvas, startIdx, visibleSubList)
        drawSelectedValue(canvas, startIdx, visibleSubList)

        canvas.restore()
    }

    // ===== 各元素绘制 =====
    private fun drawEmptyState(canvas: Canvas) {
        textPaint.textSize = spToPx(14f)
        textPaint.color = Color.WHITE
        textPaint.alpha = 0x80
        canvas.drawText("No Data", viewWidth / 2, viewHeight / 2, textPaint)
        textPaint.alpha = 0xFF
        textPaint.textSize = spToPx(12f)
    }

    private fun drawYLabels(canvas: Canvas) {
        for (i in 0 until Y_LABEL_COUNT) {
            val value = weightMin + i * weightStep
            val y = yPaddingTopPx + (Y_LABEL_COUNT - 1 - i) * yInterval
            val label = String.format("%.1f", value)
            canvas.drawText(label, yLabelLeftPx, y + textPaint.textSize / 3, textPaint)
        }
    }

    private fun drawFillArea(canvas: Canvas, startIdx: Int, visibleData: List<DayWeightData>) {
        val points = getDataPoints(startIdx, visibleData)
        if (points.size < 2) return

        val fillPath = Path()
        fillPath.moveTo(points[0].x, points[0].y)

        for (i in 1 until points.size) {
            val p1 = points[i - 1]
            val p2 = points[i]
            val dy = kotlin.math.abs(p2.y - p1.y)

            if (dy <= 1f) {
                fillPath.lineTo(p2.x, p2.y)
            } else {
                val midX = p1.x + (p2.x - p1.x) / 2f
                fillPath.cubicTo(midX, p1.y, midX, p2.y, p2.x, p2.y)
            }
        }

        val lastX = points.last().x
        val firstX = points.first().x
        val bottomY = viewHeight - yPaddingBottomPx
        fillPath.lineTo(lastX, bottomY)
        fillPath.lineTo(firstX, bottomY)
        fillPath.close()

        val topColor = Color.argb(102, 255, 255, 255)
        val midColor = Color.argb(46, 255, 255, 255)
        val bottomColor = Color.argb(0, 255, 255, 255)

        val shader = LinearGradient(
            0f, yPaddingTopPx,
            0f, viewHeight - yPaddingBottomPx,
            intArrayOf(topColor, midColor, bottomColor),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        fillPaint.shader = shader
        canvas.drawPath(fillPath, fillPaint)
    }

    // ========== 横坐标绘制（DAY/WEEK 显示日，MONTH 显示月数字） ==========
    private fun drawXLabels(canvas: Canvas, startIdx: Int, visibleData: List<DayWeightData>) {
        val dateY = viewHeight - dpToPx(17.5f)
        for (i in visibleData.indices) {
            val dataIndex = startIdx + i
            if (dataIndex >= allData.size) break
            val data = allData[dataIndex]

            val x = xStart + dataIndex * xInterval
            val label = when (chartMode) {
                ChartMode.DAY, ChartMode.WEEK -> data.dayOfMonth.toString()
                ChartMode.MONTH -> (data.month + 1).toString()  // 1~12
            }
            canvas.drawText(label, x, dateY, textPaint)
        }
    }

    // ========== 月份/年份标签绘制 ==========
    private fun drawMonthLabel(canvas: Canvas, startIdx: Int, endIdx: Int, visibleData: List<*>) {
        when (chartMode) {
            ChartMode.DAY -> {
                // Day 模式：每月 1 号显示月份英文缩写
                var pos = -1
                for (i in visibleData.indices) {
                    val dataIndex = startIdx + i
                    if (dataIndex < allData.size && allData[dataIndex].dayOfMonth == 1) {
                        pos = dataIndex
                        break
                    }
                }
                if (pos != -1) {
                    val x = xStart + pos * xInterval
                    val text = getMonthAbbr(allData[pos].month)
                    val textWidth = monthPaint.measureText(text)
                    val left = x - textWidth / 2
                    val right = x + textWidth / 2
                    val visibleLeft = scrollOffset + xStart
                    val visibleRight = scrollOffset + viewWidth
                    // 仅当标签完全可见时才绘制
                    if (left >= visibleLeft && right <= visibleRight) {
                        canvas.drawText(text, x, monthLabelY, monthPaint)
                    }
                }
            }

            ChartMode.WEEK -> {
                // Week 模式：每月第一个周一显示月份英文缩写
                for (anchor in monthAnchors) {
                    val x = anchor.x
                    val text = getMonthAbbr(anchor.month)
                    val textWidth = monthPaint.measureText(text)
                    val left = x - textWidth / 2
                    val right = x + textWidth / 2
                    val visibleLeft = scrollOffset + xStart
                    val visibleRight = scrollOffset + viewWidth
                    // 仅当标签完全可见时才绘制
                    if (left >= visibleLeft && right <= visibleRight) {
                        canvas.drawText(text, x, monthLabelY, monthPaint)
                    }
                }
            }

            ChartMode.MONTH -> {
                // Month 模式：仅在 1 月（month == 0）的位置绘制年份
                for (i in visibleData.indices) {
                    val dataIndex = startIdx + i
                    if (dataIndex >= allData.size) break
                    val data = allData[dataIndex]
                    if (data.month == 0) {  // 1月
                        val x = xStart + dataIndex * xInterval
                        val text = data.year.toString()
                        val textWidth = monthPaint.measureText(text)
                        val left = x - textWidth / 2
                        val right = x + textWidth / 2
                        val visibleLeft = scrollOffset + xStart
                        val visibleRight = scrollOffset + viewWidth
                        // 仅当标签完全可见时才绘制
                        if (left >= visibleLeft && right <= visibleRight) {
                            canvas.drawText(text, x, monthLabelY, monthPaint)
                        }
                    }
                }
            }
        }
    }

    private fun drawVerticalGridLines(canvas: Canvas, startIdx: Int, visibleData: List<DayWeightData>) {
        val startY = dpToPx(36f)
        val endY = startY + dpToPx(160f)
        for (i in visibleData.indices) {
            val dataIndex = startIdx + i
            if (dataIndex >= allData.size) break
            val x = xStart + dataIndex * xInterval
            canvas.drawLine(x, startY, x, endY, verticalGridPaint)
        }
    }

    private fun drawDataLine(canvas: Canvas, startIdx: Int, visibleData: List<DayWeightData>) {
        val points = getDataPoints(startIdx, visibleData)
        if (points.size < 2) return

        val path = Path()
        path.moveTo(points[0].x, points[0].y)

        for (i in 1 until points.size) {
            val p1 = points[i - 1]
            val p2 = points[i]
            val dy = kotlin.math.abs(p2.y - p1.y)

            if (dy <= 1f) {
                path.lineTo(p2.x, p2.y)
            } else {
                val midX = p1.x + (p2.x - p1.x) / 2f
                path.cubicTo(midX, p1.y, midX, p2.y, p2.x, p2.y)
            }
        }

        canvas.drawPath(path, linePaint)
    }

    private fun drawDots(canvas: Canvas, startIdx: Int, visibleData: List<DayWeightData>) {
        val points = getDataPoints(startIdx, visibleData)

        for (point in points) {
            val isSelected = selectedDataIndex == point.dataIndex
            val normalRadius = dpToPx(DOT_RADIUS_NORMAL)
            val selectedRadius = dpToPx(DOT_RADIUS_SELECTED)

            if (isSelected) {
                dotFillPaint.color = Color.WHITE
                canvas.drawCircle(point.x, point.y, selectedRadius, dotFillPaint)
            } else {
                dotFillPaint.color = Color.WHITE
                canvas.drawCircle(point.x, point.y, normalRadius, dotFillPaint)
            }
        }
    }

    private fun drawSelectedValue(canvas: Canvas, startIdx: Int, visibleData: List<DayWeightData>) {
        if (selectedDataIndex == null || selectedWeight == null) return
        val points = getDataPoints(startIdx, visibleData)
        val point = points.find { it.dataIndex == selectedDataIndex } ?: return
        val label = String.format("%.1f", selectedWeight)
        val labelY = point.y - dpToPx(DOT_RADIUS_SELECTED) - dpToPx(VALUE_LABEL_TOP_OFFSET)
        canvas.drawText(label, point.x, labelY, valuePaint)
    }

    // ===== 数据点提取（自动过滤 null） =====
    private fun getDataPoints(startIdx: Int, visibleData: List<DayWeightData>): List<ChartPoint> {
        val points = mutableListOf<ChartPoint>()
        for (i in visibleData.indices) {
            val dataIndex = startIdx + i
            if (dataIndex >= allData.size) break
            val data = allData[dataIndex]
            if (data.weight == null) continue
            val x = xStart + dataIndex * xInterval
            val y = weightToY(data.weight)
            points.add(ChartPoint(dataIndex, x, y))
        }
        return points
    }

    // ===== 触摸事件（完全复用） =====
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (allData.isEmpty()) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!scroller.isFinished) {
                    scroller.abortAnimation()
                }
                lastTouchX = event.x
                isDragging = false
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain()
                } else {
                    velocityTracker?.clear()
                }
                velocityTracker?.addMovement(event)
                selectedDataIndex = null
                selectedWeight = null
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = lastTouchX - event.x
                velocityTracker?.addMovement(event)
                if (!isDragging && abs(dx) > touchSlop) {
                    isDragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                if (isDragging) {
                    var newOffset = scrollOffset + dx
                    newOffset = newOffset.coerceIn(minScrollX, maxScrollX)
                    scrollOffset = newOffset
                    lastTouchX = event.x
                    invalidate()
                    notifyRangeChanged()
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                velocityTracker?.addMovement(event)
                if (isDragging) {
                    velocityTracker?.computeCurrentVelocity(1000)
                    val velocityX = velocityTracker?.xVelocity ?: 0f
                    if (abs(velocityX) > 500f) {
                        scroller.fling(
                            scrollOffset.toInt(), 0,
                            -velocityX.toInt(), 0,
                            minScrollX.toInt(), maxScrollX.toInt(),
                            0, 0,
                            (xInterval * 0.5f).toInt(), 0
                        )
                        postInvalidateOnAnimation()
                    }
                    parent?.requestDisallowInterceptTouchEvent(false)
                } else {
                    handleClick(event.x, event.y)
                }
                isDragging = false
                velocityTracker?.recycle()
                velocityTracker = null
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            val newX = scroller.currX.toFloat()
            val clampedX = newX.coerceIn(minScrollX, maxScrollX)
            if (scrollOffset != clampedX) {
                scrollOffset = clampedX
                invalidate()
                notifyRangeChanged()
            }
            if (!scroller.computeScrollOffset()) {
                val finalX = scroller.currX.toFloat().coerceIn(minScrollX, maxScrollX)
                if (scrollOffset != finalX) {
                    scrollOffset = finalX
                    invalidate()
                    notifyRangeChanged()
                }
            }
        }
    }

    private fun handleClick(x: Float, y: Float) {
        val dataX = x + scrollOffset
        val radius = dpToPx(DOT_RADIUS_SELECTED) + dpToPx(6f)

        val startIdx = visibleStartIndex.toInt()
        val endIdx = min(startIdx + displayCount + 1, allData.size)
        val visibleSubList = allData.subList(startIdx, endIdx)
        val points = getDataPoints(startIdx, visibleSubList)

        var hitPoint: ChartPoint? = null
        for (point in points) {
            val dx = dataX - point.x
            val dy = y - point.y
            if (dx * dx + dy * dy < radius * radius) {
                hitPoint = point
                break
            }
        }

        if (hitPoint != null) {
            val weight = allData[hitPoint.dataIndex].weight
            if (weight != null) {
                selectedDataIndex = hitPoint.dataIndex
                selectedWeight = weight
                invalidate()
            }
        } else {
            selectedDataIndex = null
            selectedWeight = null
            invalidate()
        }
    }

    // ===== 工具方法 =====
    private fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density
    private fun spToPx(sp: Float): Float = sp * resources.displayMetrics.scaledDensity

    private fun getMonthAbbr(month: Int): String {
        return arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")[month]
    }

    private fun formatDate(calendar: Calendar): String {
        return "${getMonthAbbr(calendar.get(Calendar.MONTH))} ${calendar.get(Calendar.DAY_OF_MONTH)}"
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        animator = null
    }

    private fun weightToY(weight: Float): Float {
        val drawableHeight = viewHeight - yPaddingTopPx - yPaddingBottomPx
        val ratio = (weight - weightMin) / (weightMax - weightMin)
        return yPaddingTopPx + drawableHeight * (1 - ratio)
    }

    fun refresh() {
        invalidate()
    }

    fun getStartDate(): String? {
        val startIdx = visibleStartIndex.toInt()
        return if (startIdx < allData.size) formatDate(allData[startIdx].date) else null
    }

    fun getEndDate(): String? {
        val endIdx = min(visibleStartIndex.toInt() + displayCount - 1, allData.size - 1)
        return if (endIdx >= 0 && endIdx < allData.size) formatDate(allData[endIdx].date) else null
    }

    private data class ChartPoint(
        val dataIndex: Int,
        val x: Float,
        val y: Float
    )

    // ========== 月份锚点管理 ==========

    /**
     * 重建所有月份锚点（DAY 模式：每月1号；WEEK 模式：该月份第一个周一，
     * 若该月无周一则用该月第一个数据点，绝不跨月）
     */
    private fun rebuildMonthAnchors() {
        monthAnchors.clear()
        if (allData.isEmpty()) return

        when (chartMode) {
            ChartMode.DAY -> {
                for (i in allData.indices) {
                    if (allData[i].dayOfMonth == 1) {
                        monthAnchors.add(MonthAnchor(allData[i].month, i))
                    }
                }
            }

            ChartMode.WEEK -> {
                var currentMonth = -1
                var firstMondayIndexInMonth = -1
                var firstIndexInMonth = -1

                for (i in allData.indices) {
                    val data = allData[i]
                    val m = data.month

                    if (m != currentMonth) {
                        if (currentMonth != -1) {
                            val anchorIndex = if (firstMondayIndexInMonth != -1)
                                firstMondayIndexInMonth else firstIndexInMonth
                            if (anchorIndex != -1) {
                                monthAnchors.add(MonthAnchor(currentMonth, anchorIndex))
                            }
                        }
                        currentMonth = m
                        firstMondayIndexInMonth = -1
                        firstIndexInMonth = i
                    }

                    if (data.date.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY &&
                        firstMondayIndexInMonth == -1) {
                        firstMondayIndexInMonth = i
                    }
                }

                if (currentMonth != -1) {
                    val anchorIndex = if (firstMondayIndexInMonth != -1)
                        firstMondayIndexInMonth else firstIndexInMonth
                    if (anchorIndex != -1) {
                        monthAnchors.add(MonthAnchor(currentMonth, anchorIndex))
                    }
                }
            }

            ChartMode.MONTH -> {
                // MONTH 模式不使用锚点，清空即可
                monthAnchors.clear()
            }
        }
    }

    /**
     * 根据当前布局参数更新所有锚点的像素坐标
     */
    private fun updateMonthAnchorPositions() {
        for (anchor in monthAnchors) {
            anchor.x = xStart + anchor.dataIndex * xInterval
        }
    }
}