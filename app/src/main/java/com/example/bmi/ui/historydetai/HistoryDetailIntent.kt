package com.example.bmi.ui.historydetail

sealed class HistoryDetailIntent {
    data class LoadRecord(val id: Long) : HistoryDetailIntent()
    object DeleteRecord : HistoryDetailIntent()
    object BackPressed : HistoryDetailIntent()
}