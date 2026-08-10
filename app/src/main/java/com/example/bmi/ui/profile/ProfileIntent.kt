package com.example.bmi.ui.profile

sealed class ProfileIntent {
    data object Init : ProfileIntent()
    data object Login : ProfileIntent()
    data object Logout : ProfileIntent()
    data object AvatarClicked : ProfileIntent()
    data object ImportSampleData : ProfileIntent()
    data object ToggleSync : ProfileIntent()
}