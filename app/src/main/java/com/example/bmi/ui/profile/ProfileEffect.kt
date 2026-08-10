package com.example.bmi.ui.profile

sealed class ProfileEffect {
    data object ShowLoginDialog : ProfileEffect()
    data object ShowUserInfoDialog : ProfileEffect()
    data object ImportSuccess : ProfileEffect()
    data class ShowFeedbackBanner(val message: String) : ProfileEffect()
    data class ShowBanner(val iconRes: Int, val message: String) : ProfileEffect()
}