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
        //查找选中的点，没找到直接返回
        val point = points.find { it.dataIndex == selectedDataIndex } ?: return

        //生成标签文本
        val label = String.format("%.1f kg", selectedValue)

        //设置自定义弹出的内边距
        val paddingHorizontal = dpToPx(8f)
        val paddingTop = dpToPx(6f)
        val paddingBottom = dpToPx(6f)

        val textWidth = valuePaint.measureText(label)
        //出当前字体下文字“从最高处到最低处”的精确像素高度  ascent（上升高度）  descent（下降高度）
        val textHeight = valuePaint.fontMetrics.descent - valuePaint.fontMetrics.ascent

        val bgWidth = textWidth + paddingHorizontal * 2
        val bgHeight = textHeight + paddingTop + paddingBottom

        //定义了 “气泡弹窗（Tooltip）底部边缘” 与 “数据点（圆点）顶部边缘” 之间的固定垂直间距（空隙）。
        val gap = dpToPx(9f)

        //水平居中
        val bgLeft = point.x - bgWidth / 2
        //垂直
        val bgBottom = point.y - dpToPx(DOT_RADIUS_SELECTED_WEIGHT) - gap
        val bgTop = bgBottom - bgHeight
        val bgRight = bgLeft + bgWidth

        canvas.drawRoundRect(
            bgLeft,
            bgTop,
            bgRight,
            bgBottom,
            dpToPx(5f),//设置圆角矩形
            dpToPx(5f),
            valueBgPaint
        )

        val fm = valuePaint.fontMetrics
        val textX = point.x
        //                              基线为负
        val textY = bgTop + paddingTop - fm.ascent
        canvas.drawText(label, textX, textY, valuePaint)

        val triangleHeight = dpToPx(3f)    // 三角形的高度（3dp）
        val triangleHalfWidth = dpToPx(5f)  // 三角形底边的一半宽度（5dp）
        val triangleTop = bgBottom                 // 三角形的上边（底边）与背景框的下边对齐
        val triangleBottom = bgBottom + triangleHeight  // 三角形的下边（尖角）位于背景下方 3dp 处
        val triangleCenterX = point.x              // 三角形的水平中心与数据点 X 坐标对齐

        val trianglePath = Path().apply {
            moveTo(triangleCenterX - triangleHalfWidth, triangleTop)          // 顶点1：左上
            lineTo(triangleCenterX + triangleHalfWidth, triangleTop)          // 顶点2：右上
            lineTo(triangleCenterX, triangleBottom)                          // 顶点3：下尖
            close()                                                          // 闭合路径
        }
        //绘制三角形
        canvas.drawPath(trianglePath, valueBgPaint)
    }
}