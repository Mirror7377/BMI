package com.example.bmi.ui.feedback

sealed class FeedbackEffect {
    data object NavigateBack : FeedbackEffect()
}