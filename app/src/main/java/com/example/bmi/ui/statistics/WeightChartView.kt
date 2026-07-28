package com.example.bmi.ui.statistics

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import com.example.bmi.data.enums.ChartMode
import java.util.Calendar

class WeightChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseChartView<DayWeightData>(context, attrs, defStyleAttr) {

    // ===== Weight 特有常量 =====
    private companion object {
        const val DOT_RADIUS_SELECTED_WEIGHT = 4.5f
    }

    // ===== 私有状态 =====
    private var weightMin = 0f
    private var weightMax = 1f
    private var weightStep = 0.2f

    // ===== 实现抽象方法 =====
    override fun getValue(data: DayWeightData) = data.weight

    override fun getDate(data: DayWeightData) = data.date

    override fun getMonth(data: DayWeightData) = data.month

    override fun getYear(data: DayWeightData) = data.year

    override fun getDayOfMonth(data: DayWeightData) = data.dayOfMonth

    override fun getTimeInMillis(data: DayWeightData) = data.date.timeInMillis

    override fun getYMin() = weightMin

    override fun getYMax() = weightMax

    override fun getYStep() = weightStep

    override fun updateYAxis(data: List<DayWeightData>) {
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

    override fun valueToY(value: Float): Float {
        val drawableHeight = viewHeight - yPaddingTopPx - yPaddingBottomPx
        val ratio = (value - weightMin) / (weightMax - weightMin)
        return yPaddingTopPx + drawableHeight * (1 - ratio)
    }

    override fun getDotColor(value: Float): Int = Color.WHITE

    override fun getSelectedDotRadius(): Float = dpToPx(DOT_RADIUS_SELECTED_WEIGHT)

    override fun getSelectedValueLabel(value: Float): String = String.format("%.1f kg", value)

    override fun getXLabel(data: DayWeightData): String {
        return when (chartMode) {
            ChartMode.DAY, ChartMode.WEEK -> data.dayOfMonth.toString()
            ChartMode.MONTH -> (data.month + 1).toString()
        }
    }

    override fun getRangeString(startDate: Calendar, endDate: Calendar): String {
        return when (chartMode) {
            ChartMode.DAY, ChartMode.WEEK -> {
                "${formatDate(startDate)} ~ ${formatDate(endDate)}"
            }
            ChartMode.MONTH -> {
                "${getMonthAbbr(startDate.get(Calendar.MONTH))} ${startDate.get(Calendar.YEAR)} ~ " +
                        "${getMonthAbbr(endDate.get(Calendar.MONTH))} ${endDate.get(Calendar.YEAR)}"
            }
        }
    }

    override fun areDataEqual(d1: DayWeightData, d2: DayWeightData): Boolean = d1 == d2

    // ===== DAY 模式（Weight 特有逻辑） =====
    override fun setDataDay(data: List<DayWeightData>) {
        val sorted = data.sortedBy { it.date.timeInMillis }
        val lastWithWeight = sorted.findLast { it.weight != null }

        if (lastWithWeight == null) {
            allData = emptyList()
            scrollOffset = 0f
            selectedDataIndex = null
            selectedValue = null
            updateYAxis(emptyList())
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
        this.selectedValue = null

        updateYAxis(allDates)
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

    // ===== 重写 drawDots（Weight 普通白色圆点，选中时更大） =====
    override fun drawDots(canvas: Canvas, startIdx: Int, visibleData: List<DayWeightData>) {
        val points = getDataPoints(startIdx, visibleData)

        for (point in points) {
            val isSelected = selectedDataIndex == point.dataIndex
            val normalRadius = dpToPx(DOT_RADIUS_NORMAL)
            val selectedRadius = dpToPx(DOT_RADIUS_SELECTED_WEIGHT)

            if (isSelected) {
                dotFillPaint.color = Color.WHITE
                canvas.drawCircle(point.x, point.y, selectedRadius, dotFillPaint)
            } else {
                dotFillPaint.color = Color.WHITE
                canvas.drawCircle(point.x, point.y, normalRadius, dotFillPaint)
            }
        }
    }

    // ===== 重写 drawSelectedValue（Weight 添加 kg 单位） =====
    override fun drawSelectedValue(canvas: Canvas, startIdx: Int, visibleData: List<DayWeightData>) {
        if (selectedDataIndex == null || selectedValue == null) return

        val points = getDataPoints(startIdx, visibleData)
        val point = points.find { it.dataIndex == selectedDataIndex } ?: return

        val label = String.format("%.1f kg", selectedValue)

        val paddingHorizontal = dpToPx(8f)
        val paddingTop = dpToPx(6f)
        val paddingBottom = dpToPx(6f)

        val textWidth = valuePaint.measureText(label)
        val textHeight = valuePaint.fontMetrics.descent - valuePaint.fontMetrics.ascent

        val bgWidth = textWidth + paddingHorizontal * 2
        val bgHeight = textHeight + paddingTop + paddingBottom

        val gap = dpToPx(9f)

        val bgLeft = point.x - bgWidth / 2
        val bgBottom = point.y - dpToPx(DOT_RADIUS_SELECTED_WEIGHT) - gap
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
}