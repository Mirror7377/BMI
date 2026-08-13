package com.example.bmi.ui.result

import com.example.bmi.data.database.RecommendApp
import com.example.bmi.data.database.BmiRecord
import com.example.bmi.ui.bmigauge.BmiLevel

data class ResultState(
    val record: BmiRecord? = null,
    val bmiLevel: BmiLevel = BmiLevel.NORMAL,
    val hasSavedRecord: Boolean = false,
    val recommendedApps: List<RecommendApp> = emptyList(),
    val isLoading: Boolean = false
)