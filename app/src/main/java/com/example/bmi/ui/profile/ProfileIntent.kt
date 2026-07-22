package com.example.bmi.ui.profile

sealed class ProfileIntent {
    object Init : ProfileIntent()
    object Refresh : ProfileIntent()
    data class UpdateName(val name: String) : ProfileIntent()
    data class UpdateEmail(val email: String) : ProfileIntent()
    object Login : ProfileIntent()
    object Logout : ProfileIntent()
}