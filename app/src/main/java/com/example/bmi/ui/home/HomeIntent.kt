package com.example.bmi.ui.home

import com.example.bmi.ui.home.enums.Gender
import com.example.bmi.ui.home.enums.HeightUnit
import com.example.bmi.ui.home.enums.TimeOfDay
import com.example.bmi.ui.home.enums.WeightUnit

sealed class HomeIntent {
    // 初始化
    object Init : HomeIntent()

    // 体重相关
    data class WeightChanged(val value: Double) : HomeIntent()           // 用户手动输入（kg）
    data class WeightUnitChanged(val unit: WeightUnit) : HomeIntent()

    // 身高相关
    data class HeightCmChanged(val value: Double) : HomeIntent()           // 用户手动输入（cm）
    data class HeightUnitChanged(val unit: HeightUnit) : HomeIntent()
    data class FeetChanged(val feet: Int) : HomeIntent()                 // ft-in 模式修改英尺
    data class InchesChanged(val inches: Int) : HomeIntent()             // ft-in 模式修改英寸

    // 年龄
    data class AgeChanged(val age: Int) : HomeIntent()

    // 性别
    data class GenderSelected(val gender: Gender) : HomeIntent()

    // 时间
    data class TimeChanged(val timestamp: Long, val timeOfDay: TimeOfDay) : HomeIntent()

    // 计算按钮
    object Calculate : HomeIntent()
}