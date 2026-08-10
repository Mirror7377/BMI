package com.example.bmi.ui.profile


data class ProfileState(
    val isLoggedIn: Boolean = false,
    val userName: String = "",
    val userEmail: String = "",
    val isSyncEnabled: Boolean = true,
    val isLoading: Boolean = false
)