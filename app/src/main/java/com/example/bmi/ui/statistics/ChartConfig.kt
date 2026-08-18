package com.example.bmi.ui.statistics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.bmi.data.enums.ChartMode
import java.util.Calendar
import kotlin.math.abs

/**
 * 图表配置接口
 *
 * Y轴统一采用6个刻度点，5个等间距。
 * 最高数据点位于第1、2个刻度之间的正中间，
 * 最低数据点位于第5、6个刻度之间的正中间。
 */
interface ChartConfig<T> {

    fun getValue(data: T): Float?

    fun getDate(data: T): Calendar

    fun getMonth(data: T): Int

    fun getYear(data: T): Int

    fun getDayOfMonth(data: T): Int

    fun getTimeInMillis(data: T): Long

    fun createPlaceholder(date: Calendar): T

    /**
     * 返回 (axisMin, axisMax, step)
     * Y轴总共6个刻度，因此有5个step。
     */
    fun computeAxis(data: List<T>): Triple<Float, Float, Float>

    fun valueToY(
        value: Float,
        yMin: Float,
        yMax: Float,
        drawableHeight: Float,
        yPaddingTop: Float
    ): Float

    fun getDotColor(value: Float): Color

    val selectedDotRadiusDp: Float

    val normalDotRadiusDp: Float

    fun getSelectedValueLabel(value: Float): String

    fun getXLabel(
        data: T,
        mode: ChartMode
    ): String

    fun DrawScope.drawSelectedDot(
        point: Offset,
        value: Float
    )

    fun DrawScope.drawNormalDot(
        point: Offset
    )
}

/**
 * 通用Y轴计算逻辑
 *
 * Y轴固定6个刻度（5个间隔）。
 * 最高数据点位于第1和第2刻度中间，最低数据点位于第5和第6刻度中间。
 * 因此：上方留白0.5 step，数据区域4 step，下方留白0.5 step。
 */
internal fun computeAxisInternal(
    minVal: Float,
    maxVal: Float
): Triple<Float, Float, Float> {

    // 当无数据变化时（max == min），构造一个合理的6刻度范围
    if (minVal == maxVal) {
        val value = minVal
        val absValue = abs(value)

        val step = when {
            absValue < 1f -> 0.1f
            absValue < 10f -> 1f
            absValue < 100f -> 5f
            else -> 10f
        }

        // 让当前值处于整个Y轴中间
        val axisMin = value - step * 2.5f
        val axisMax = value + step * 2.5f

        return Triple(axisMin, axisMax, step)
    }

    // 正常情况：最高点和最低点之间跨越4个step
    val dataSpan = maxVal - minVal
    val step = dataSpan / 4f

    // 最低数据点位于 axisMin + 0.5 step
    val axisMin = minVal - step * 0.5f

    // 最高数据点位于 axisMax - 0.5 step
    val axisMax = maxVal + step * 0.5f

    return Triple(axisMin, axisMax, step)
}

/**
 * 获取月份缩写
 */
internal fun getMonthAbbr(month: Int): String {
    return arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )[month]
}