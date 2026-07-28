package com.example.bmi.ui.profile

sealed class ProfileEffect {
    object Success : ProfileEffect()
    object ShowUserInfoDialog : ProfileEffect()
    object ShowLoginDialog : ProfileEffect()
}