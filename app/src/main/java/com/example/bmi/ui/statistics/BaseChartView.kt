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
import com.example.bmi.data.enums.ChartMode
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 图表基类，提取 BmiChartView 和 WeightChartView 的公共逻辑
 * @param T 数据类型（DayBmiData 或 DayWeightData）
 */
abstract class BaseChartView<T>(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ===== 枚举 =====

    // ===== 常量 =====
    protected companion object {
        const val Y_PADDING_TOP = 29.5f
        const val Y_PADDING_BOTTOM = 37f
        const val Y_LABEL_COUNT = 6
        const val Y_LABEL_LEFT_MARGIN = 25f

        const val X_PADDING_RIGHT = 20.5f
        const val X_LABEL_COUNT = 8

        const val MONTH_TOP_MARGIN = 14.5f

        const val DOT_RADIUS_NORMAL = 3f
    }

    // ===== 月份锚点 =====
    protected val monthAnchors = mutableListOf<MonthAnchor>()

    protected data class MonthAnchor(
        val month: Int,
        val dataIndex: Int,
        var x: Float = 0f
    )

    // ===== 画笔（所有子类共用） =====
    protected val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.LEFT
        typeface = resources.getFont(R.font.montserrat_extrabold)
        textSize = spToPx(12f)
    }

    protected val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = resources.getFont(R.font.montserrat_extrabold)
        textSize = spToPx(11f)
    }

    protected val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = resources.getFont(R.font.montserrat_extrabold)
        textSize = spToPx(12f)
    }

    protected val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    protected val dotFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    protected val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = 0x33
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    protected val verticalGridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EEEEEE")
        style = Paint.Style.STROKE
        strokeWidth = 0.5f
    }

    protected val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = 0x99
        strokeWidth = 4.0f
        style = Paint.Style.STROKE
    }

    protected val valueBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2C2C2E")
        style = Paint.Style.FILL
    }

    // ===== 数据与状态 =====
    protected var allData: List<T> = emptyList()
    protected val displayCount = X_LABEL_COUNT
    protected var scrollOffset = 0f
    protected var minScrollX = 0f
    protected var maxScrollX = 0f
    protected val visibleStartIndex: Float get() = scrollOffset / xInterval

    protected var selectedDataIndex: Int? = null
    protected var selectedValue: Float? = null

    protected var viewWidth = 0f
    protected var viewHeight = 0f
    protected var yPaddingTopPx = 0f
    protected var yPaddingBottomPx = 0f
    protected var yLabelLeftPx = 0f
    protected var xPaddingRightPx = 0f
    protected var yAvailableHeight = 0f
    protected var xAvailableWidth = 0f
    protected var yInterval = 0f
    protected var xInterval = 0f
    protected var xStart = 0f
    protected var monthLabelY = 0f

    protected val scroller = OverScroller(context)
    protected var velocityTracker: VelocityTracker? = null
    protected var isDragging = false
    protected var lastTouchX = 0f
    protected val touchSlop = dpToPx(10f)

    protected var animator: ValueAnimator? = null

    var onDataRangeChanged: ((startDate: String, endDate: String) -> Unit)? = null

    // ===== 模式支持 =====
    protected var chartMode = ChartMode.DAY
    fun setMode(mode: ChartMode) {
        if (chartMode != mode) {
            chartMode = mode
            invalidate()
        }
    }

    // ===== 子类必须实现的抽象方法 =====
    /** 获取数据中的数值（BMI 或 Weight） */
    protected abstract fun getValue(data: T): Float?

    /** 获取数据中的日期 */
    protected abstract fun getDate(data: T): Calendar

    /** 获取数据中的月份 */
    protected abstract fun getMonth(data: T): Int

    /** 获取数据中的年份 */
    protected abstract fun getYear(data: T): Int

    /** 获取数据中的日 */
    protected abstract fun getDayOfMonth(data: T): Int

    /** 获取数据中的时间戳（用于比较） */
    protected abstract fun getTimeInMillis(data: T): Long

    /** 获取 Y 轴最小值 */
    protected abstract fun getYMin(): Float

    /** 获取 Y 轴最大值 */
    protected abstract fun getYMax(): Float

    /** 获取 Y 轴步长 */
    protected abstract fun getYStep(): Float

    /** 更新 Y 轴数据 */
    protected abstract fun updateYAxis(data: List<T>)

    /** 将数值转换为 Y 坐标 */
    protected abstract fun valueToY(value: Float): Float

    /** 获取圆点颜色 */
    protected abstract fun getDotColor(value: Float): Int

    /** 获取选中的圆点半径 */
    protected abstract fun getSelectedDotRadius(): Float

    /** 获取选中值的显示标签 */
    protected abstract fun getSelectedValueLabel(value: Float): String

    /** 获取 X 轴标签（日/月数字） */
    protected abstract fun getXLabel(data: T): String

    /** 获取日期范围的格式化字符串 */
    protected abstract fun getRangeString(startDate: Calendar, endDate: Calendar): String

    /** 判断两个数据是否相等（用于比较） */
    protected abstract fun areDataEqual(d1: T, d2: T): Boolean

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
            updateYAxis(allData)
        }

        updateLayoutMetrics()
        updateScrollBounds()
        clampScrollOffset()
        invalidate()
    }

    // ===== 数据设置 =====
    fun setData(data: List<T>) {
        when (chartMode) {
            ChartMode.DAY -> setDataDay(data)
            ChartMode.WEEK -> setDataWeek(data)
            ChartMode.MONTH -> setDataMonth(data)
        }
    }

    // ---------- 模板方法：DAY 模式 ----------
    protected open fun setDataDay(data: List<T>) {
        // 由子类实现具体逻辑，因为涉及数据裁剪
    }

    // ---------- 模板方法：WEEK 模式 ----------
    protected open fun setDataWeek(data: List<T>) {
        allData = data.sortedBy { getTimeInMillis(it) }
        selectedDataIndex = null
        selectedValue = null
        updateYAxis(allData)
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

    // ---------- 模板方法：MONTH 模式 ----------
    protected open fun setDataMonth(data: List<T>) {
        allData = data.sortedBy { getTimeInMillis(it) }
        selectedDataIndex = null
        selectedValue = null
        updateYAxis(allData)
        updateLayoutMetrics()
        val targetStart = (allData.size - displayCount).coerceAtLeast(0)
        scrollOffset = targetStart * xInterval + xInterval / 2f
        updateScrollBounds()
        clampScrollOffset()
        invalidate()
        notifyRangeChanged()
        monthAnchors.clear()
        updateMonthAnchorPositions()
    }

    // ===== Y 轴计算工具 =====
    protected fun computeAxis(
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

        axisMin = kotlin.math.floor(axisMin / step) * step
        axisMax = axisMin + step * (Y_LABEL_COUNT - 1)

        return Triple(axisMin, axisMax, step)
    }

    // ===== 滚动辅助 =====
    protected fun updateScrollBounds() {
        if (allData.isEmpty()) {
            minScrollX = 0f
            maxScrollX = 0f
            return
        }
        val totalWidth = (allData.size - 1) * xInterval
        val visibleWidth = (displayCount - 1) * xInterval
        minScrollX = -xInterval / 2f
        maxScrollX = max(0f, totalWidth - visibleWidth)
    }

    protected fun clampScrollOffset() {
        scrollOffset = scrollOffset.coerceIn(minScrollX, maxScrollX)
    }

    // ===== 日期范围回调 =====
    protected fun notifyRangeChanged() {
        if (allData.isEmpty()) return

        val startIdx = visibleStartIndex.toInt()
        if (startIdx < 0 || startIdx >= allData.size) return

        val endIdx = min(startIdx + displayCount - 1, allData.size - 1)
        val startDate = getDate(allData[startIdx])
        val endDate = getDate(allData[endIdx])

        val rangeStr = getRangeString(startDate, endDate)
        onDataRangeChanged?.invoke(rangeStr, rangeStr)
    }

    // ===== 布局度量 =====
    protected fun updateLayoutMetrics() {
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

    protected open fun calculateMaxYLabelWidth(): Float {
        var maxWidth = 0f
        for (i in 0 until Y_LABEL_COUNT) {
            val value = getYMin() + i * getYStep()
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
        drawXLabels(canvas, startIdx, visibleSubList)
        drawMonthLabel(canvas, startIdx, endIdx, visibleSubList)
        drawDots(canvas, startIdx, visibleSubList)
        drawSelectedValue(canvas, startIdx, visibleSubList)

        drawFillArea(canvas)
        drawDataLine(canvas)

        canvas.restore()
    }

    // ===== 绘制方法 =====
    protected fun drawEmptyState(canvas: Canvas) {
        textPaint.textSize = spToPx(14f)
        textPaint.color = Color.WHITE
        textPaint.alpha = 0x80
        canvas.drawText("No Data", viewWidth / 2, viewHeight / 2, textPaint)
        textPaint.alpha = 0xFF
        textPaint.textSize = spToPx(12f)
    }

    protected fun drawYLabels(canvas: Canvas) {
        for (i in 0 until Y_LABEL_COUNT) {
            val value = getYMin() + i * getYStep()
            val y = yPaddingTopPx + (Y_LABEL_COUNT - 1 - i) * yInterval
            val label = String.format("%.1f", value)
            canvas.drawText(label, yLabelLeftPx, y + textPaint.textSize / 3, textPaint)
        }
    }

    protected fun drawFillArea(canvas: Canvas) {
        val points = getFullDataPoints()
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

    protected open fun drawXLabels(canvas: Canvas, startIdx: Int, visibleData: List<T>) {
        val dateY = viewHeight - dpToPx(17.5f)
        textPaint.textAlign = Paint.Align.CENTER
        val visibleLeft = scrollOffset + xStart
        val visibleRight = scrollOffset + viewWidth
        for (i in visibleData.indices) {
            val dataIndex = startIdx + i
            if (dataIndex >= allData.size) break
            val data = allData[dataIndex]
            val x = xStart + dataIndex * xInterval
            val label = getXLabel(data)
            val labelWidth = textPaint.measureText(label)
            val left = x - labelWidth / 2
            val right = x + labelWidth / 2
            if (left >= visibleLeft && right <= visibleRight) {
                canvas.drawText(label, x, dateY, textPaint)
            }
        }
        textPaint.textAlign = Paint.Align.LEFT
    }

    protected open fun drawMonthLabel(canvas: Canvas, startIdx: Int, endIdx: Int, visibleData: List<T>) {
        when (chartMode) {
            ChartMode.DAY -> {
                var pos = -1
                for (i in visibleData.indices) {
                    val dataIndex = startIdx + i
                    if (dataIndex < allData.size && getDayOfMonth(allData[dataIndex]) == 1) {
                        pos = dataIndex
                        break
                    }
                }
                if (pos != -1) {
                    val x = xStart + pos * xInterval
                    val text = getMonthAbbr(getMonth(allData[pos]))
                    val textWidth = monthPaint.measureText(text)
                    val left = x - textWidth / 2
                    val right = x + textWidth / 2
                    val visibleLeft = scrollOffset + xStart
                    val visibleRight = scrollOffset + viewWidth
                    if (left >= visibleLeft && right <= visibleRight) {
                        canvas.drawText(text, x, monthLabelY, monthPaint)
                    }
                }
            }

            ChartMode.WEEK -> {
                for (anchor in monthAnchors) {
                    val x = anchor.x
                    val text = getMonthAbbr(anchor.month)
                    val textWidth = monthPaint.measureText(text)
                    val left = x - textWidth / 2
                    val right = x + textWidth / 2
                    val visibleLeft = scrollOffset + xStart
                    val visibleRight = scrollOffset + viewWidth
                    if (left >= visibleLeft && right <= visibleRight) {
                        canvas.drawText(text, x, monthLabelY, monthPaint)
                    }
                }
            }

            ChartMode.MONTH -> {
                for (i in visibleData.indices) {
                    val dataIndex = startIdx + i
                    if (dataIndex >= allData.size) break
                    val data = allData[dataIndex]
                    if (getMonth(data) == 0) {
                        val x = xStart + dataIndex * xInterval
                        val text = getYear(data).toString()
                        val textWidth = monthPaint.measureText(text)
                        val left = x - textWidth / 2
                        val right = x + textWidth / 2
                        val visibleLeft = scrollOffset + xStart
                        val visibleRight = scrollOffset + viewWidth
                        if (left >= visibleLeft && right <= visibleRight) {
                            canvas.drawText(text, x, monthLabelY, monthPaint)
                        }
                    }
                }
            }
        }
    }

    protected fun drawVerticalGridLines(canvas: Canvas, startIdx: Int, visibleData: List<T>) {
        val startY = dpToPx(36f)
        val endY = startY + dpToPx(160f)
        for (i in visibleData.indices) {
            val dataIndex = startIdx + i
            if (dataIndex >= allData.size) break
            val x = xStart + dataIndex * xInterval
            canvas.drawLine(x, startY, x, endY, verticalGridPaint)
        }
    }

    protected fun drawDataLine(canvas: Canvas) {
        val points = getFullDataPoints()
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

    protected open fun drawDots(canvas: Canvas, startIdx: Int, visibleData: List<T>) {
        val points = getDataPoints(startIdx, visibleData)
        val normalRadius = dpToPx(DOT_RADIUS_NORMAL)

        for (point in points) {
            val value = getValue(allData[point.dataIndex])
            if (value == null) continue
            val color = getDotColor(value)
            val isSelected = selectedDataIndex == point.dataIndex

            if (isSelected) {
                val selectedRadius = getSelectedDotRadius()
                dotFillPaint.color = Color.WHITE
                canvas.drawCircle(point.x, point.y, selectedRadius, dotFillPaint)
                dotFillPaint.color = color
                canvas.drawCircle(point.x, point.y, selectedRadius * 0.8f, dotFillPaint)
            } else {
                dotFillPaint.color = Color.WHITE
                canvas.drawCircle(point.x, point.y, normalRadius, dotFillPaint)
            }
        }
    }

    protected open fun drawSelectedValue(canvas: Canvas, startIdx: Int, visibleData: List<T>) {
        if (selectedDataIndex == null || selectedValue == null) return

        val points = getDataPoints(startIdx, visibleData)
        val point = points.find { it.dataIndex == selectedDataIndex } ?: return

        val label = getSelectedValueLabel(selectedValue!!)

        val paddingHorizontal = dpToPx(8f)
        val paddingTop = dpToPx(6f)
        val paddingBottom = dpToPx(6f)

        val textWidth = valuePaint.measureText(label)
        val textHeight = valuePaint.fontMetrics.descent - valuePaint.fontMetrics.ascent

        val bgWidth = textWidth + paddingHorizontal * 2
        val bgHeight = textHeight + paddingTop + paddingBottom

        val gap = dpToPx(9f)

        val bgLeft = point.x - bgWidth / 2
        val bgBottom = point.y - getSelectedDotRadius() - gap
        val bgTop = bgBottom - bgHeight
        val bgRight = bgLeft + bgWidth

        canvas.drawRoundRect(
            bgLeft,
            bgTop,
            bgRight,
            bgBottom,
            dpToPx(5f),
            dpToPx(5f),
            valueBgPaint
        )

        val fm = valuePaint.fontMetrics
        val textX = point.x
        val textY = bgTop + paddingTop - fm.ascent
        canvas.drawText(label, textX, textY, valuePaint)

        val triangleHeight = dpToPx(3f)
        val triangleHalfWidth = dpToPx(5f)
        val triangleTop = bgBottom
        val triangleBottom = bgBottom + triangleHeight
        val triangleCenterX = point.x

        val trianglePath = Path().apply {
            moveTo(triangleCenterX - triangleHalfWidth, triangleTop)
            lineTo(triangleCenterX + triangleHalfWidth, triangleTop)
            lineTo(triangleCenterX, triangleBottom)
            close()
        }
        canvas.drawPath(trianglePath, valueBgPaint)
    }

    // ===== 数据点提取 =====
    protected fun getDataPoints(startIdx: Int, visibleData: List<T>): List<ChartPoint> {
        val points = mutableListOf<ChartPoint>()
        for (i in visibleData.indices) {
            val dataIndex = startIdx + i
            if (dataIndex >= allData.size) break
            val data = allData[dataIndex]
            val value = getValue(data)
            if (value == null) continue
            val x = xStart + dataIndex * xInterval
            val y = valueToY(value)
            points.add(ChartPoint(dataIndex, x, y))
        }
        return points
    }

    protected fun getFullDataPoints(): List<ChartPoint> {
        val points = mutableListOf<ChartPoint>()
        for (i in allData.indices) {
            val data = allData[i]
            val value = getValue(data) ?: continue
            val x = xStart + i * xInterval
            val y = valueToY(value)
            points.add(ChartPoint(i, x, y))
        }
        return points
    }

    protected data class ChartPoint(
        val dataIndex: Int,
        val x: Float,
        val y: Float
    )

    // ===== 触摸事件 =====
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
                selectedValue = null
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

    protected fun handleClick(x: Float, y: Float) {
        val dataX = x + scrollOffset
        val radius = getSelectedDotRadius() + dpToPx(6f)

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
            val value = getValue(allData[hitPoint.dataIndex])
            if (value != null) {
                selectedDataIndex = hitPoint.dataIndex
                selectedValue = value
                invalidate()
            }
        } else {
            selectedDataIndex = null
            selectedValue = null
            invalidate()
        }
    }

    // ===== 月份锚点管理 =====
    protected fun rebuildMonthAnchors() {
        monthAnchors.clear()
        if (allData.isEmpty()) return

        when (chartMode) {
            ChartMode.DAY -> {
                for (i in allData.indices) {
                    if (getDayOfMonth(allData[i]) == 1) {
                        monthAnchors.add(MonthAnchor(getMonth(allData[i]), i))
                    }
                }
            }

            ChartMode.WEEK -> {
                var currentMonth = -1
                var firstMondayIndexInMonth = -1
                var firstIndexInMonth = -1

                for (i in allData.indices) {
                    val data = allData[i]
                    val m = getMonth(data)

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

                    val cal = getDate(data)
                    if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY &&
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
                monthAnchors.clear()
            }
        }
    }

    protected fun updateMonthAnchorPositions() {
        for (anchor in monthAnchors) {
            anchor.x = xStart + anchor.dataIndex * xInterval
        }
    }

    // ===== 工具方法 =====
    protected fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density
    protected fun spToPx(sp: Float): Float = sp * resources.displayMetrics.scaledDensity

    protected fun getMonthAbbr(month: Int): String {
        return arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")[month]
    }

    protected fun formatDate(calendar: Calendar): String {
        return "${getMonthAbbr(calendar.get(Calendar.MONTH))} ${calendar.get(Calendar.DAY_OF_MONTH)}"
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        animator = null
    }

    fun refresh() {
        invalidate()
    }

    fun getStartDate(): String? {
        val startIdx = visibleStartIndex.toInt()
        return if (startIdx < allData.size) formatDate(getDate(allData[startIdx])) else null
    }

    fun getEndDate(): String? {
        val endIdx = min(visibleStartIndex.toInt() + displayCount - 1, allData.size - 1)
        return if (endIdx >= 0 && endIdx < allData.size) formatDate(getDate(allData[endIdx])) else null
    }
}