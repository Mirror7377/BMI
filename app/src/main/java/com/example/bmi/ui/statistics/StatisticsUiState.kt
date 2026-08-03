package com.example.bmi.ui.statistics

import com.example.bmi.data.enums.ChartMode

data class StatisticsUiState(
    val mode: ChartMode = ChartMode.DAY,
    val bmiData: List<DayBmiData> = emptyList(),
    val weightData: List<DayWeightData> = emptyList()
)
