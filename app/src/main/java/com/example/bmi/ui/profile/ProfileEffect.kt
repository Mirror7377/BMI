package com.example.bmi.ui.profile

sealed class ProfileEffect {
    object NavigateBack : ProfileEffect()
    data class ShowToast(val message: String) : ProfileEffect()
    object Success : ProfileEffect()
}