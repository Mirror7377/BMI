package com.example.bmi.ui.display


sealed class DisplayEffect {
    object NavigateToHome : DisplayEffect()
    object ShowDiscardDialog : DisplayEffect()
}