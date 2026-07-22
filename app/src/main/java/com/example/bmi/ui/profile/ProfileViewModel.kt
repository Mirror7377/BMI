package com.example.bmi.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class ProfileViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<ProfileEffect>()
    val effect: SharedFlow<ProfileEffect> = _effect.asSharedFlow()

    fun handleIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.Init -> loadData()
            is ProfileIntent.Login -> performLogin()
            is ProfileIntent.Logout -> performLogout()
            else -> {}
        }
    }

    private fun loadData() {
        // 加载本地存储的登录状态（此处为示例，默认未登录）
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoggedIn = false,
                userName = "",
                userEmail = ""
            )
        }
    }

    private fun performLogin() {
        viewModelScope.launch {
            // 模拟登录
            _state.value = _state.value.copy(
                isLoggedIn = true,
                userName = "Cassie",
                userEmail = "cassiexiao@gmail.com"
            )
            _effect.emit(ProfileEffect.ShowToast("Login successful"))
        }
    }

    private fun performLogout() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoggedIn = false,
                userName = "",
                userEmail = ""
            )
            _effect.emit(ProfileEffect.ShowToast("Logged out"))
        }
    }
}