package com.example.bmi.ui.profile

import androidx.annotation.StringRes

data class ProfileState(
    val isLoggedIn: Boolean = false,
    val userName: String = "",
    val userEmail: String = "",
    val isLoading: Boolean = false,
    @StringRes val errorMessage: Int? = null
)