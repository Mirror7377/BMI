package com.example.bmi.ui.display

sealed class DisplayIntent {
    object LoadLatest : DisplayIntent()
    data class NavigateTo(val destination: Destination) : DisplayIntent()

    enum class Destination {
        RECENT
    }
}