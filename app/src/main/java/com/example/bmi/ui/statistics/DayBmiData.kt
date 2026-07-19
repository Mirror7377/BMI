package com.example.bmi.ui.statistics

import java.util.Calendar

/**
 * 单日 BMI 数据
 * @param date 日期（Calendar 对象）
 * @param bmi BMI 值，null 表示当天无数据
 */
data class DayBmiData(
    val date: Calendar,
    val bmi: Float?
) {
    val dayOfMonth: Int
        get() = date.get(Calendar.DAY_OF_MONTH)

    val month: Int
        get() = date.get(Calendar.MONTH)

    val year: Int
        get() = date.get(Calendar.YEAR)
}