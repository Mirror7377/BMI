package com.example.bmi.ui.home

import com.example.bmi.ui.home.enums.Gender
import com.example.bmi.ui.home.enums.HeightUnit
import com.example.bmi.ui.home.enums.TimeOfDay
import com.example.bmi.ui.home.enums.WeightUnit

data class HomeState(
    // 用户当前输入的原始值（UI 直接显示）
    val weightInput: Double = 140.0,          // 默认 lb
    val weightUnit: WeightUnit = WeightUnit.LB,

    val heightInput: Double = 170.0,         // cm 模式：cm 值；ft-in 模式：总英寸数
    val heightUnit: HeightUnit = HeightUnit.FT_IN,
    val feetInput: Int = 5,                  // ft-in 模式下的英尺（仅当 heightUnit=FT_IN 时使用）
    val inchesInput: Int = 7,                // ft-in 模式下的英寸


    // 标准换算值（用于计算和存储）
    val weightKg: Double = 65.0,
    val heightCm: Double = 170.0,

    // 其他 UI 状态
    val age: Int = 25,
    val gender: Gender = Gender.MALE,
    val timestamp: Long = System.currentTimeMillis(),//日期
    val timeOfDay: TimeOfDay = TimeOfDay.fromSystemTime(),//根据函数获取时间段
    val isLoading: Boolean = false,
    val weightDisplay: String = "65.00",
    val heightDisplay: String = "170.0",
    val timeDisplay: String = "",
)

