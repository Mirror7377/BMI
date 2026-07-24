package com.example.bmi.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bmi.data.database.BmiRecord
import com.example.bmi.data.repository.BmiRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
@HiltViewModel
class ProfileViewModel @Inject constructor(private val repository: BmiRepository) : ViewModel() {
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<ProfileEffect>()
    val effect: SharedFlow<ProfileEffect> = _effect.asSharedFlow()

    fun handleIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.Init -> loadData()
            is ProfileIntent.Login -> performLogin()
            is ProfileIntent.Logout -> performLogout()
            is ProfileIntent.ImportJson -> importJson(intent.json)
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
           // _effect.emit(ProfileEffect.ShowToast("Login successful"))
        }
    }

    private fun performLogout() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoggedIn = false,
                userName = "",
                userEmail = ""
            )
            //_effect.emit(ProfileEffect.ShowToast("Logged out"))
        }
    }

    private fun importJson(json: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val gson = Gson()
                val type = object : TypeToken<List<BmiRecord>>() {}.type
                val records: List<BmiRecord> = gson.fromJson(json, type)
                if (records.isNotEmpty()) {
                    repository.insertAll(records)
                    withContext(Dispatchers.Main) {
                        //todo 导入成功
                        _effect.emit(ProfileEffect.Success)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _effect.emit(ProfileEffect.ShowToast("No records to import."))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _effect.emit(ProfileEffect.ShowToast("Import failed: ${e.message}"))
                }
            }
        }
    }
}