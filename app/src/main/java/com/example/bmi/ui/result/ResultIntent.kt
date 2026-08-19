package com.example.bmi.ui.result

sealed class ResultIntent {
    data class Init(val recordJson: String) : ResultIntent()
    data object SaveRecord : ResultIntent()
}