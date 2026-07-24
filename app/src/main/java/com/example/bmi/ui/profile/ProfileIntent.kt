package com.example.bmi.ui.profile

sealed class ProfileIntent {
    object Init : ProfileIntent()
    object Login : ProfileIntent()
    object Logout : ProfileIntent()
    data class ImportJson(val json: String) : ProfileIntent()
}