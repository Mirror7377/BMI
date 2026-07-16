package com.example.bmi.ui.result


sealed class ResultEffect {
    object NavigateToHome : ResultEffect()
    object ShowDiscardDialog : ResultEffect()
}