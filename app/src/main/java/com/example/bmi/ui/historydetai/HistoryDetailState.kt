package com.example.bmi.ui.historydetail

import com.example.bmi.data.database.RecommendApp
import com.example.bmi.ui.home.enums.Gender
import com.example.bmi.ui.home.enums.HeightUnit
import com.example.bmi.ui.home.enums.WeightUnit
import com.example.bmi.ui.bmigauge.BmiLevel

data class HistoryDetailState(
    val recordId: Long = 0L,
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
    val recommendedApps: List<RecommendApp> = emptyList(),
    val timestamp: Long = 0L,
    val timeOfDay: String = "",
    val isLoading: Boolean = false
)