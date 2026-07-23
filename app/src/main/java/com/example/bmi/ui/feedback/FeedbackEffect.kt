package com.example.bmi.ui.feedback

sealed class FeedbackEffect {
    object NavigateBack : FeedbackEffect()
    data class ShowToast(val message: String) : FeedbackEffect()
}