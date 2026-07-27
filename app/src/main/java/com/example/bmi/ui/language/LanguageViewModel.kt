package com.example.bmi.ui.language

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class LanguageViewModel(
    private val prefs: SharedPreferences
) : ViewModel() {

    // UI 状态流
    private val _state = MutableStateFlow(LanguageState())
    val state: StateFlow<LanguageState> = _state.asStateFlow()

    // 副作用流（用于导航、Toast 等一次性事件）
    private val _effect = MutableSharedFlow<Effect>()
    val effect: SharedFlow<Effect> = _effect.asSharedFlow()

    sealed class Effect {
        object NavigateToMain : Effect()
    }

    fun handleIntent(intent: LanguageIntent) {
        when (intent) {
            is LanguageIntent.LoadSavedLanguage -> loadSavedLanguage()
            is LanguageIntent.SelectLanguage -> selectLanguage(intent.langCode)
        }
    }

    private fun loadSavedLanguage() {
        val savedLang = prefs.getString("language", "en") ?: "en"
        _state.update { it.copy(selectedLanguage = savedLang) }
    }

    private fun selectLanguage(langCode: String) {
        // 更新存储
        prefs.edit().putString("language", langCode).apply()

        // 更新状态（UI 立即响应）
        _state.update { it.copy(selectedLanguage = langCode) }

        // 发送导航副作用（跳转到 MainActivity）
        viewModelScope.launch {
            _effect.emit(Effect.NavigateToMain)
        }
    }
}