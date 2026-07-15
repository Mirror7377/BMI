package com.example.bmi.ui.display

import com.example.bmi.utils.BmiLevel
import com.example.bmi.ui.home.enums.Gender
import com.example.bmi.ui.home.enums.HeightUnit
import com.example.bmi.ui.home.enums.WeightUnit

data class DisplayState(
    val bmi: Double = 0.0,
    val weightInput: Double = 0.0,
    val weightUnit: String = WeightUnit.KG.name,
    val heightInput: Double = 0.0,
    val heightUnit: String = HeightUnit.CM.name,
    val feet: Int = 0,
    val inches: Int = 0,
    val age: Int = 0,
    val gender: String = Gender.MALE.name,
    val heightCm: Double = 0.0,
    val bmiLevel: BmiLevel = BmiLevel.NORMAL,
    val isLoading: Boolean = false,
    // 可以添加其他UI状态，如错误信息等
)