package com.example.bmi.ui.profile

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bmi.R
import com.example.bmi.data.database.BmiRecord
import com.example.bmi.data.repository.BmiRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import androidx.core.content.edit

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: BmiRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<ProfileEffect>()
    val effect: SharedFlow<ProfileEffect> = _effect.asSharedFlow()

    fun handleIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.Init -> loadData()
            is ProfileIntent.Login -> performLogin()
            is ProfileIntent.Logout -> performLogout()
            is ProfileIntent.AvatarClicked -> handleAvatarClicked()
            is ProfileIntent.ImportSampleData -> importSampleData()
            is ProfileIntent.ToggleSync -> toggleSync()
        }
    }

    private fun loadData() {
        _state.update {
            it.copy(
                isLoggedIn = false,
                userName = "",
                userEmail = "",
                isSyncEnabled = true
            )
        }
    }

    private fun performLogin() {
        _state.update {
            it.copy(
                isLoggedIn = true,
                userName = "Cassie",
                userEmail = "cassiexiao@gmail.com"
            )
        }
        viewModelScope.launch {
            _effect.emit(ProfileEffect.ShowBanner(
                iconRes = R.drawable.login,
                message = "Logged in successfully."
            ))
        }
    }

    private fun performLogout() {
        _state.update {
            it.copy(
                isLoggedIn = false,
                userName = "",
                userEmail = ""
            )
        }
        viewModelScope.launch {
            _effect.emit(ProfileEffect.ShowBanner(
                iconRes = R.drawable.logout,
                message = "Logged out successfully."
            ))
        }
    }

    private fun handleAvatarClicked() {
        viewModelScope.launch {
            if (_state.value.isLoggedIn) {
                _effect.emit(ProfileEffect.ShowUserInfoDialog)
            } else {
                _effect.emit(ProfileEffect.ShowLoginDialog)
            }
        }
    }

    private fun toggleSync() {
        _state.update {
            it.copy(isSyncEnabled = !it.isSyncEnabled)
        }
    }

    private fun importSampleData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = context.assets
                    .open("sample_records.json")
                    .bufferedReader()
                    .use { it.readText() }

                val gson = Gson()
                val type = object : TypeToken<List<BmiRecord>>() {}.type
                val records: List<BmiRecord> = gson.fromJson(jsonString, type)

                if (records.isNotEmpty()) {
                    repository.insertAll(records)
                    withContext(Dispatchers.Main) {
                        _effect.emit(ProfileEffect.ImportSuccess)
                    }
                } else {
                    Log.d("ProfileViewModel", "No records to import.")
                }
            } catch (e: IOException) {
                Log.e("ProfileViewModel", "Failed to load sample data: ${e.message}", e)
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Import failed: ${e.message}", e)
            }
        }
    }

    fun checkFeedbackContent() {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val feedback = prefs.getString("feedback_content", null)
        if (!feedback.isNullOrEmpty()) {
            // 消费后立即删除，防止重复显示
            prefs.edit { remove("feedback_content") }
            // 构建感谢消息
            val message = context.getString(R.string.feedback_thanks_message, feedback)
            viewModelScope.launch {
                _effect.emit(ProfileEffect.ShowFeedbackBanner(message))
            }
        }
    }
}