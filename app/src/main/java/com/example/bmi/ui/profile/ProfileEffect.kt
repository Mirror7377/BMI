package com.example.bmi.ui.profile

sealed class ProfileEffect {
    object NavigateBack : ProfileEffect()
    object Success : ProfileEffect()
}