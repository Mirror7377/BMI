package com.example.bmi.ui.feedback

sealed class FeedbackIntent {
    data class UpdateFeedbackText(val text: String) : FeedbackIntent()
    data object SubmitFeedback : FeedbackIntent()
    data object NavigateBack : FeedbackIntent()
}