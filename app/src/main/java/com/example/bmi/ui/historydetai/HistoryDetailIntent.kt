package com.example.bmi.ui.historydetai

sealed class HistoryDetailIntent {
    data class LoadRecord(val id: Long) : HistoryDetailIntent()
    data object DeleteRecord : HistoryDetailIntent()
    data object ShowBmiLegend : HistoryDetailIntent()
}