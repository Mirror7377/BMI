package com.example.bmi.ui.historydetail

sealed class HistoryDetailEffect {
    object NavigateBack : HistoryDetailEffect()
    data class ShowError(val message: String) : HistoryDetailEffect()

    object NavigateToHome : HistoryDetailEffect()
}