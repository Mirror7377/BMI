package com.example.bmi.ui.historydetai

sealed class HistoryDetailEffect {
    data object NavigateBack : HistoryDetailEffect()
    data object NavigateToHome : HistoryDetailEffect()  // 清空任务栈跳转主页
    data object ShowDeleteDialog : HistoryDetailEffect()
    data object ShowBmiLegend : HistoryDetailEffect()
}