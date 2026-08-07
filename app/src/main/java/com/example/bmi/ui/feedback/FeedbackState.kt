package com.example.bmi.ui.feedback

data class FeedbackState(
    val feedbackText: String = "",
    val isSubmitEnabled: Boolean = false,
    val isLoading: Boolean = false
)