package com.example.bmi.ui.statistics

sealed class StatisticsIntent {
    object LoadDay : StatisticsIntent()
    object LoadWeek : StatisticsIntent()
    object LoadMonth : StatisticsIntent()
}