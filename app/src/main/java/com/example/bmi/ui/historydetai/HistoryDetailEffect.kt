package com.example.bmi.ui.historydetai

sealed class HistoryDetailEffect {
    object NavigateBack : HistoryDetailEffect()

    object NavigateToHome : HistoryDetailEffect()
}