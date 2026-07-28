package com.example.bmi.ui.statistics

data class StatisticsUiState(
    val mode: ChartMode = ChartMode.DAY,
    val bmiData: List<DayBmiData> = emptyList(),
    val weightData: List<DayWeightData> = emptyList(),
    val isLoading: Boolean = false,
    val isWeightLoading: Boolean = false,
    val error: String? = null,
    // 各模式缓存（用于快速切换）
    val dayBmiCache: List<DayBmiData> = emptyList(),
    val dayWeightCache: List<DayWeightData> = emptyList(),
    val weekBmiCache: List<DayBmiData> = emptyList(),
    val weekWeightCache: List<DayWeightData> = emptyList(),
    val monthBmiCache: List<DayBmiData> = emptyList(),
    val monthWeightCache: List<DayWeightData> = emptyList()
)

enum class ChartMode { DAY, WEEK, MONTH }