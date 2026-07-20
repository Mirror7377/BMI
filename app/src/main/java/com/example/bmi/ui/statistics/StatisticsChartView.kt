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
import kotlin.math.roundToInt

class StatisticsChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private companion object {
        const val CARD_WIDTH = 345f
        const val CARD_HEIGHT = 217.5f

        const val Y_PADDING_TOP = 29.5f
        const val Y_PADDING_BOTTOM = 37f
        const val Y_LABEL_COUNT = 6
        const val Y_LABEL_LEFT_MARGIN = 25f

        const val X_PADDING_RIGHT = 20.5f
        const val X_LABEL_COUNT = 8

        const val MONTH_TOP_MARGIN = 14.5f

        // 圆点尺寸（修改）
        const val DOT_RADIUS_NORMAL = 3f        // 普通直径6px
        const val DOT_RADIUS_SELECTED = 5.0f    // 选中总直径9px
        const val DOT_RADIUS_COLOR = 4f         // 选中彩色部分直径8px

        const val VALUE_LABEL_TOP_OFFSET = 8f
    }

    // 画笔（不变）
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

    // 数据
    private var allData: List<DayBmiData> = emptyList()
    private val displayCount = X_LABEL_COUNT
    private var scrollOffset = 0f
    private var minScrollX = 0f
    private var maxScrollX = 0f
    private val visibleStartIndex: Float get() = scrollOffset / xInterval
    private var yMin = 0f
    private var yMax = 1f
    private var yStep = 0.2f

    private var selectedDataIndex: Int? = null
    private var selectedBmi: Float? = null

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

    init {
        isClickable = true
        isFocusable = true
        setWillNotDraw(false)
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

        if (allData.isNotEmpty()) {
            computeFixedYAxis(allData)
        } else {
            yMin = 0f
            yMax = 1f
            yStep = 0.2f
        }

        updateLayoutMetrics()
        updateScrollBounds()
        clampScrollOffset()
        invalidate()
    }

    fun setData(data: List<DayBmiData>) {
        val sorted = data.sortedBy { it.date.timeInMillis }
        val lastWithBmi = sorted.findLast { it.bmi != null }
        if (lastWithBmi == null) {
            allData = emptyList()
            scrollOffset = 0f
            selectedDataIndex = null
            selectedBmi = null
            computeFixedYAxis(emptyList())
            updateLayoutMetrics()
            updateScrollBounds()
            clampScrollOffset()
            invalidate()
            return
        }

        val latestDate = lastWithBmi.date
        val cal = Calendar.getInstance().apply { time = latestDate.time }
        cal.add(Calendar.DAY_OF_YEAR, -58)
        val startDate = cal.clone() as Calendar
        cal.add(Calendar.DAY_OF_YEAR, 59)
        val endDate = cal

        val allDates = mutableListOf<DayBmiData>()
        var current = startDate.clone() as Calendar
        while (current <= endDate) {
            val bmi = sorted.find {
                val d = it.date
                d.get(Calendar.YEAR) == current.get(Calendar.YEAR) &&
                        d.get(Calendar.DAY_OF_YEAR) == current.get(Calendar.DAY_OF_YEAR)
            }?.bmi
            allDates.add(DayBmiData(current.clone() as Calendar, bmi))
            current.add(Calendar.DAY_OF_YEAR, 1)
        }

        this.allData = allDates
        this.selectedDataIndex = null
        this.selectedBmi = null

        computeFixedYAxis(allDates)
        updateLayoutMetrics()

        val targetStart = (allData.size - displayCount).coerceAtLeast(0)
        this.scrollOffset = targetStart.toFloat() * xInterval

        updateScrollBounds()
        clampScrollOffset()
        invalidate()
        notifyRangeChanged()
    }

    private fun computeFixedYAxis(data: List<DayBmiData>) {
        val bmiList = data.mapNotNull { it.bmi }
        if (bmiList.isEmpty()) {
            yMin = 0f
            yMax = 1f
            yStep = 0.2f
            return
        }

        val minVal = bmiList.minOrNull() ?: 0f
        val maxVal = bmiList.maxOrNull() ?: 1f

        val span = if (minVal == maxVal) 2f else (maxVal - minVal)
        val totalSpan = span / 0.75f

        yMin = minVal - totalSpan * 0.10f
        yMax = maxVal + totalSpan * 0.15f

        var step = (yMax - yMin) / 5f
        step = (step * 10f).roundToInt() / 10f
        if (step < 0.1f) step = 0.1f
        yStep = step

        yMax = yMin + step * 5f
    }

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

    private fun notifyRangeChanged() {
        val startIdx = visibleStartIndex.toInt()
        val endIdx = min(startIdx + displayCount - 1, allData.size - 1)
        val startDate = if (startIdx < allData.size) allData[startIdx] else null
        val endDate = if (endIdx < allData.size) allData[endIdx] else null
        onDataRangeChanged?.invoke(
            startDate?.let { formatDate(it.date) } ?: "",
            endDate?.let { formatDate(it.date) } ?: ""
        )
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
        drawMonthLabel(canvas, startIdx, visibleSubList)
        drawDataLine(canvas, startIdx, visibleSubList)
        drawDots(canvas, startIdx, visibleSubList)
        drawSelectedValue(canvas, startIdx, visibleSubList)

        canvas.restore()
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
        for (i in 0 until Y_LABEL_COUNT) {
            val value = yMin + i * yStep
            val y = yPaddingTopPx + (Y_LABEL_COUNT - 1 - i) * yInterval
            val label = String.format("%.1f", value)
            canvas.drawText(label, yLabelLeftPx, y + textPaint.textSize / 3, textPaint)
        }
    }

    private fun drawFillArea(canvas: Canvas, startIdx: Int, visibleData: List<DayBmiData>) {
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
        val bottomY = viewHeight
        fillPath.lineTo(lastX, bottomY)
        fillPath.lineTo(firstX, bottomY)
        fillPath.close()

        val topColor = Color.argb(102, 255, 255, 255)
        val midColor = Color.argb(46, 255, 255, 255)
        val bottomColor = Color.argb(0, 255, 255, 255)

        val shader = LinearGradient(
            0f, 0f,
            0f, viewHeight,
            intArrayOf(topColor, midColor, bottomColor),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        fillPaint.shader = shader
        canvas.drawPath(fillPath, fillPaint)
    }

    private fun drawXLabels(canvas: Canvas, startIdx: Int, visibleData: List<DayBmiData>) {
        val dateY = viewHeight - dpToPx(17.5f)
        for (i in visibleData.indices) {
            val dataIndex = startIdx + i
            if (dataIndex >= allData.size) break
            val x = xStart + dataIndex * xInterval
            val day = allData[dataIndex].dayOfMonth
            canvas.drawText(day.toString(), x, dateY, textPaint)
        }
    }

    private fun drawVerticalGridLines(canvas: Canvas, startIdx: Int, visibleData: List<DayBmiData>) {
        val startY = dpToPx(36f)
        val endY = startY + dpToPx(140f)
        for (i in visibleData.indices) {
            val dataIndex = startIdx + i
            if (dataIndex >= allData.size) break
            val x = xStart + dataIndex * xInterval
            canvas.drawLine(x, startY, x, endY, verticalGridPaint)
        }
    }

    private fun drawMonthLabel(canvas: Canvas, startIdx: Int, visibleData: List<DayBmiData>) {
        var pos = -1
        for (i in visibleData.indices) {
            val dataIndex = startIdx + i
            if (dataIndex < allData.size && allData[dataIndex].dayOfMonth == 1) {
                pos = dataIndex
                break
            }
        }
        if (pos == -1) {
            showMonthLabel = false
            return
        }
        showMonthLabel = true
        val x = xStart + pos * xInterval
        val monthName = getMonthAbbr(allData[pos].month)
        canvas.drawText(monthName, x, monthLabelY, monthPaint)
    }

    private fun drawDataLine(canvas: Canvas, startIdx: Int, visibleData: List<DayBmiData>) {
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

    private fun drawDots(canvas: Canvas, startIdx: Int, visibleData: List<DayBmiData>) {
        val points = getDataPoints(startIdx, visibleData)

        for (point in points) {
            val bmi = allData[point.dataIndex].bmi ?: continue
            val color = getBmiColor(bmi)
            val isSelected = selectedDataIndex == point.dataIndex

            val normalRadius = dpToPx(DOT_RADIUS_NORMAL)
            val selectedRadius = dpToPx(DOT_RADIUS_SELECTED)
            val colorRadius = dpToPx(DOT_RADIUS_COLOR)

            if (isSelected) {
                // 选中：先画白色实心圆（总直径10px）
                dotFillPaint.color = Color.WHITE
                canvas.drawCircle(point.x, point.y, selectedRadius, dotFillPaint)
                // 再画彩色实心圆（直径9px），形成约0.5px白色边缘
                dotFillPaint.color = color
                canvas.drawCircle(point.x, point.y, colorRadius, dotFillPaint)
            } else {
                // 普通：纯白色实心圆（直径6px）
                dotFillPaint.color = Color.WHITE
                canvas.drawCircle(point.x, point.y, normalRadius, dotFillPaint)
            }
        }
    }

    private fun drawSelectedValue(canvas: Canvas, startIdx: Int, visibleData: List<DayBmiData>) {
        if (selectedDataIndex == null || selectedBmi == null) return
        val points = getDataPoints(startIdx, visibleData)
        val point = points.find { it.dataIndex == selectedDataIndex } ?: return
        val label = String.format("%.1f", selectedBmi)
        val labelY = point.y - dpToPx(DOT_RADIUS_SELECTED) - dpToPx(VALUE_LABEL_TOP_OFFSET)
        canvas.drawText(label, point.x, labelY, valuePaint)
    }

    // 数据点计算（保持不变）
    private fun getDataPoints(startIdx: Int, visibleData: List<DayBmiData>): List<ChartPoint> {
        val points = mutableListOf<ChartPoint>()
        for (i in visibleData.indices) {
            val dataIndex = startIdx + i
            if (dataIndex >= allData.size) break
            val data = allData[dataIndex]
            if (data.bmi == null) continue
            val x = xStart + dataIndex * xInterval
            val y = bmiToY(data.bmi)
            points.add(ChartPoint(dataIndex, x, y))
        }
        return points
    }

    // 触摸事件（不变）
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
                selectedBmi = null
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
            val bmi = allData[hitPoint.dataIndex].bmi
            if (bmi != null) {
                selectedDataIndex = hitPoint.dataIndex
                selectedBmi = bmi
                invalidate()
            }
        } else {
            selectedDataIndex = null
            selectedBmi = null
            invalidate()
        }
    }

    // 工具方法
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

    private fun bmiToY(bmi: Float): Float {
        val drawableHeight = viewHeight - yPaddingTopPx - yPaddingBottomPx
        val ratio = (bmi - yMin) / (yMax - yMin)
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
}

private data class ChartPoint(
    val dataIndex: Int,
    val x: Float,
    val y: Float
)