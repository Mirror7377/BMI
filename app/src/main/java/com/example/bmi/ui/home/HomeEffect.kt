package com.example.bmi.ui.home

sealed class HomeEffect {
    data class NavigateToResult(val recordJson: String) : HomeEffect()
}