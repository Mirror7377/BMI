package com.example.bmi.ui.statistics.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.example.bmi.data.enums.ChartMode
import com.example.bmi.ui.statistics.DayBmiData
import java.util.Calendar

/**
 * BMI 图表专属配置
 * 对应原 BmiChartView
 */
class BmiChartConfig : ChartConfig<DayBmiData> {

    override fun getValue(data: DayBmiData): Float? = data.bmi
    override fun getDate(data: DayBmiData): Calendar = data.date
    override fun getMonth(data: DayBmiData): Int = data.month
    override fun getYear(data: DayBmiData): Int = data.year
    override fun getDayOfMonth(data: DayBmiData): Int = data.dayOfMonth
    override fun getTimeInMillis(data: DayBmiData): Long = data.date.timeInMillis

    override fun computeAxis(data: List<DayBmiData>): Triple<Float, Float, Float> {
        val bmiList = data.mapNotNull { it.bmi }
        if (bmiList.isEmpty()) {
            return Triple(0f, 1f, 0.2f)
        }
        return computeAxisInternal(bmiList.minOrNull() ?: 0f, bmiList.maxOrNull() ?: 1f)
    }

    override fun valueToY(value: Float, yMin: Float, yMax: Float, drawableHeight: Float, yPaddingTop: Float): Float {
        val ratio = (value - yMin) / (yMax - yMin)
        return yPaddingTop + drawableHeight * (1 - ratio)
    }

    // 对应原 BmiChartView.getBmiColor()
    override fun getDotColor(value: Float): Color {
        return when {
            value < 16f -> Color(0xFF4343B8)
            value < 17f -> Color(0xFF1258E1)
            value < 18.5f -> Color(0xFF0099F2)
            value < 25f -> Color(0xFF54A529)
            value < 30f -> Color(0xFFFECD2E)
            value < 35f -> Color(0xFFFFA100)
            value < 40f -> Color(0xFFFF7137)
            else -> Color(0xFFD3333B)
        }
    }

    override val selectedDotRadiusDp: Float = 5f
    override val normalDotRadiusDp: Float = 3f

    override fun getSelectedValueLabel(value: Float): String = String.format("%.1f", value)

    override fun getXLabel(data: DayBmiData, mode: ChartMode): String {
        return when (mode) {
            ChartMode.DAY, ChartMode.WEEK -> data.dayOfMonth.toString()
            ChartMode.MONTH -> (data.month + 1).toString()
        }
    }

    // 对应原 BmiChartView.drawDots() 选中状态：外圈白 5dp，内圈彩色 4dp
    override fun DrawScope.drawSelectedDot(point: Offset, value: Float) {
        drawCircle(Color.Companion.White, radius = selectedDotRadiusDp.dp.toPx(), center = point)
        drawCircle(getDotColor(value), radius = 4f.dp.toPx(), center = point)
    }

    // 对应原 BaseChartView.drawDots() 普通状态（白色实心圆）
    override fun DrawScope.drawNormalDot(point: Offset) {
        drawCircle(Color.Companion.White, radius = normalDotRadiusDp.dp.toPx(), center = point)
    }

    override fun createPlaceholder(date: Calendar): DayBmiData = DayBmiData(date, null)
}