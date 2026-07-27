package com.example.bmi.ui.display

sealed class DisplayEffect {
    data class NavigateTo(val destination: DisplayIntent.Destination) : DisplayEffect()
    data class ShowError(val message: String) : DisplayEffect()
}