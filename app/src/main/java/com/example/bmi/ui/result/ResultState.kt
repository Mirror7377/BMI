package com.example.bmi.ui.result

import com.example.bmi.data.database.RecommendApp
import com.example.bmi.data.enums.Gender
import com.example.bmi.data.enums.HeightUnit
import com.example.bmi.data.enums.WeightUnit
import com.example.bmi.ui.bmigauge.BmiLevel

data class ResultState(
    val bmi: Double = 0.0,
    val weightInput: Double = 0.0,
    val weightUnit: String = WeightUnit.KG.name,
    val heightUnit: String = HeightUnit.CM.name,
    val feet: Int = 0,
    val inches: Int = 0,
    val age: Int = 0,
    val gender: String = Gender.MALE.name,
    val heightCm: Double = 0.0,
    val bmiLevel: BmiLevel = BmiLevel.NORMAL,
    val timestamp: Long = 0L,  //测量时间
    val timeOfDay: String = "Morning", //测量时间段
    // 数据库中是否已有历史记录
    val hasSavedRecord: Boolean = false,
    val recommendedApps: List<RecommendApp> = emptyList()
)