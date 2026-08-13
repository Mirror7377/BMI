package com.example.bmi.ui.historydetai

import com.example.bmi.data.database.BmiRecord
import com.example.bmi.data.database.RecommendApp
import com.example.bmi.ui.bmigauge.BmiLevel

data class HistoryDetailState(
    val record: BmiRecord? = null,
    val bmiLevel: BmiLevel = BmiLevel.NORMAL,
    val recommendedApps: List<RecommendApp> = emptyList(),
    val isLoading: Boolean = false
)