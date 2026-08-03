package com.example.bmi.ui.statistics

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import com.example.bmi.data.enums.ChartMode
import java.util.Calendar

class BmiChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseChartView<DayBmiData>(context, attrs, defStyleAttr) {

    // ===== BMI 特有常量 =====
    private companion object {
        const val DOT_RADIUS_SELECTED_BMI = 5.0f
        const val DOT_RADIUS_COLOR = 4f
    }

    // ===== 私有状态 =====
    private var yMin = 0f
    private var yMax = 1f
    private var yStep = 0.2f

    // ===== 实现抽象方法 =====
    override fun getValue(data: DayBmiData) = data.bmi

    override fun getDate(data: DayBmiData) = data.date

    override fun getMonth(data: DayBmiData) = data.month

    override fun getYear(data: DayBmiData) = data.year

    override fun getDayOfMonth(data: DayBmiData) = data.dayOfMonth

    override fun getTimeInMillis(data: DayBmiData) = data.date.timeInMillis

    override fun getYMin() = yMin

    override fun getYMax() = yMax

    override fun getYStep() = yStep

    //从传入的数据列表中提取所有有效的 BMI 值，计算出一个“合理”的 Y 轴范围
    override fun updateYAxis(data: List<DayBmiData>) {
        val bmiList = data.mapNotNull { it.bmi }
        if (bmiList.isEmpty()) {
            //为空设置默认值
            yMin = 0f
            yMax = 1f
            yStep = 0.2f
            return
        }
        val (min, max, step) = computeAxis(
            bmiList.minOrNull() ?: 0f,
            bmiList.maxOrNull() ?: 1f
        )
        yMin = min
        yMax = max
        yStep = step
    }

    override fun valueToY(value: Float): Float {
        //可以绘图的实际高度
        val drawableHeight = viewHeight - yPaddingTopPx - yPaddingBottomPx
        //这个值在 Y 轴区间内处于 80% 的位置
        val ratio = (value - yMin) / (yMax - yMin)
        //y顶部+反转比例高度，y向下为正
        return yPaddingTopPx + drawableHeight * (1 - ratio)
    }

    override fun getDotColor(value: Float): Int = getBmiColor(value)

    override fun getSelectedDotRadius(): Float = dpToPx(DOT_RADIUS_SELECTED_BMI)

    override fun getSelectedValueLabel(value: Float): String = String.format("%.1f", value)

    override fun getXLabel(data: DayBmiData): String {
        return when (chartMode) {
            ChartMode.DAY, ChartMode.WEEK -> data.dayOfMonth.toString()
            //在 Calendar 类中，月份的索引是从 0 开始的
            ChartMode.MONTH -> (data.month + 1).toString()
        }
    }

    // ===== DAY 模式 =====
    override fun setDataDay(data: List<DayBmiData>) {
        val sorted = data.sortedBy { it.date.timeInMillis }
        val lastWithBmi = sorted.findLast { it.bmi != null }
        //无数据
        if (lastWithBmi == null) {
            this.allData = sorted
            this.selectedDataIndex = null
            this.selectedValue = null
            updateYAxis(allData)
            updateLayoutMetrics()
            val targetStart = (allData.size - displayCount).coerceAtLeast(0)
            this.scrollOffset = targetStart.toFloat() * xInterval
            updateScrollBounds()
            clampScrollOffset()
            invalidate()
            rebuildMonthAnchors()
            updateMonthAnchorPositions()
            return
        }
        //取出刚刚找到有数据的那一天的日期对象
        val latestDate = lastWithBmi.date
        val endDate = Calendar.getInstance().apply { time = latestDate.time }
        //一共60天的数据
        endDate.add(Calendar.DAY_OF_YEAR, -58)
        val startDate = endDate.clone() as Calendar
        endDate.add(Calendar.DAY_OF_YEAR, 59)

        val allDates = mutableListOf<DayBmiData>()
        val current = startDate.clone() as Calendar
        //生成数据列表，无数据的为null
        while (current <= endDate) {
            val bmi = sorted.find {
                val d = it.date
                d.get(Calendar.YEAR) == current.get(Calendar.YEAR) &&
                        d.get(Calendar.DAY_OF_YEAR) == current.get(Calendar.DAY_OF_YEAR)
            }?.bmi//无数据设为null
            allDates.add(DayBmiData(current.clone() as Calendar, bmi))
            current.add(Calendar.DAY_OF_YEAR, 1)
        }

        this.allData = allDates
        this.selectedDataIndex = null
        this.selectedValue = null

        //初始Y轴数据
        updateYAxis(allDates)
        updateLayoutMetrics()

        //计算偏移量显示最后的8个数据点
        val targetStart = (allData.size - displayCount).coerceAtLeast(0)
        this.scrollOffset = targetStart.toFloat() * xInterval

        updateScrollBounds()
        clampScrollOffset()
        invalidate()//重新绘制它
        rebuildMonthAnchors()
        updateMonthAnchorPositions()
    }

    // ===== 重写 drawDots（BMI 特有：选中时内外圈颜色） =====
    override fun drawDots(canvas: Canvas, startIdx: Int, visibleData: List<DayBmiData>) {
        val points = getDataPoints(startIdx, visibleData)

        for (point in points) {
            val bmi = getValue(allData[point.dataIndex]) ?: continue
            val color = getDotColor(bmi)
            val isSelected = selectedDataIndex == point.dataIndex

            val normalRadius = dpToPx(DOT_RADIUS_NORMAL)
            val selectedRadius = dpToPx(DOT_RADIUS_SELECTED_BMI)
            val colorRadius = dpToPx(DOT_RADIUS_COLOR)

            if (isSelected) {
                dotFillPaint.color = Color.WHITE
                canvas.drawCircle(point.x, point.y, selectedRadius, dotFillPaint)
                dotFillPaint.color = color
                canvas.drawCircle(point.x, point.y, colorRadius, dotFillPaint)
            } else {
                dotFillPaint.color = Color.WHITE
                canvas.drawCircle(point.x, point.y, normalRadius, dotFillPaint)
            }
        }
    }

    // ===== BMI 颜色映射 =====
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
}