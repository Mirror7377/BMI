package com.example.bmi.ui.feedback

sealed class FeedbackIntent {
    object Init : FeedbackIntent()
    data class SubmitFeedback(val text: String) : FeedbackIntent()
}