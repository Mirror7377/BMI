package com.example.bmi.ui.result


sealed class ResultEffect {
    data class NavigateToHome(val isFirstSave: Boolean) : ResultEffect()
    object ShowDiscardDialog : ResultEffect()
}