package com.example.bmi.ui.result

sealed class ResultEffect {
    data object NavigateToHome : ResultEffect()
}