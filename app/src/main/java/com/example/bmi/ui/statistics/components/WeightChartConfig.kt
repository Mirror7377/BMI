package com.example.bmi.ui.statistics.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.example.bmi.data.enums.ChartMode
import com.example.bmi.ui.statistics.DayWeightData
import java.util.Calendar

/**
 * 体重图表专属配置
 * 对应原 WeightChartView
 */
class WeightChartConfig : ChartConfig<DayWeightData> {

    override fun getValue(data: DayWeightData): Float? = data.weight
    override fun getDate(data: DayWeightData): Calendar = data.date
    override fun getMonth(data: DayWeightData): Int = data.month
    override fun getYear(data: DayWeightData): Int = data.year
    override fun getDayOfMonth(data: DayWeightData): Int = data.dayOfMonth
    override fun getTimeInMillis(data: DayWeightData): Long = data.date.timeInMillis

    override fun computeAxis(data: List<DayWeightData>): Triple<Float, Float, Float> {
        val weightList = data.mapNotNull { it.weight }
        if (weightList.isEmpty()) {
            return Triple(0f, 1f, 0.2f)
        }
        return computeAxisInternal(weightList.minOrNull() ?: 0f, weightList.maxOrNull() ?: 1f)
    }

    override fun valueToY(value: Float, yMin: Float, yMax: Float, drawableHeight: Float, yPaddingTop: Float): Float {
        val ratio = (value - yMin) / (yMax - yMin)
        return yPaddingTop + drawableHeight * (1 - ratio)
    }

    override fun getDotColor(value: Float): Color = Color.Companion.White

    override val selectedDotRadiusDp: Float = 4.5f
    override val normalDotRadiusDp: Float = 3f

    override fun getSelectedValueLabel(value: Float): String = String.format("%.1f kg", value)

    override fun getXLabel(data: DayWeightData, mode: ChartMode): String {
        return when (mode) {
            ChartMode.DAY, ChartMode.WEEK -> data.dayOfMonth.toString()
            ChartMode.MONTH -> (data.month + 1).toString()
        }
    }

    override fun DrawScope.drawSelectedDot(point: Offset, value: Float) {
        drawCircle(Color.Companion.White, radius = selectedDotRadiusDp.dp.toPx(), center = point)
    }

    override fun DrawScope.drawNormalDot(point: Offset) {
        drawCircle(Color.Companion.White, radius = normalDotRadiusDp.dp.toPx(), center = point)
    }

    override fun createPlaceholder(date: Calendar): DayWeightData = DayWeightData(date, null)
}