package com.example.bmi.ui.result

import com.example.bmi.data.database.RecommendApp
import com.example.bmi.utils.BmiLevel
import com.example.bmi.ui.home.enums.Gender
import com.example.bmi.ui.home.enums.HeightUnit
import com.example.bmi.ui.home.enums.WeightUnit

data class ResultState(
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
    // 数据库中是否已有历史记录
    val hasSavedRecord: Boolean = false,
    val recommendedApps: List<RecommendApp> = emptyList()
)