package com.example.bmi.ui.home

import com.example.bmi.ui.home.enums.Gender
import com.example.bmi.ui.home.enums.HeightUnit
import com.example.bmi.ui.home.enums.TimeOfDay
import com.example.bmi.ui.home.enums.WeightUnit

data class HomeState(
    // 用户当前输入的原始值（UI 直接显示）
    val weightInput: Double = 140.0,          // 默认 lb
    val weightUnit: WeightUnit = WeightUnit.LB,

    val heightUnit: HeightUnit = HeightUnit.FT_IN,
    val feetInput: Int = 5,                  // ft-in 模式下的英尺（仅当 heightUnit=FT_IN 时使用）
    val inchesInput: Int = 7,                // ft-in 模式下的英寸


    // 用于显示统计图数据
    val weightKg: Double = 65.0,

    val heightCm: Double = 170.0,

    // 其他 UI 状态
    val age: Int = 25,
    val gender: Gender = Gender.MALE,
    val timestamp: Long = System.currentTimeMillis(),//日期
    val timeOfDay: TimeOfDay = TimeOfDay.fromSystemTime(),//根据函数获取时间段
    val isLoading: Boolean = false,
    val weightDisplay: String = "65.00",
)

