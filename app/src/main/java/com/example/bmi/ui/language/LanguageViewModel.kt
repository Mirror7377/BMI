package com.example.bmi.ui.language

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bmi.ui.them.getSavedLanguageCode
import com.example.bmi.ui.them.saveLanguageCode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    // ===== MVI State =====
    private val _state = MutableStateFlow(LanguageState())
    val state: StateFlow<LanguageState> = _state.asStateFlow()

    // ===== MVI Effect =====
    private val _effect = MutableSharedFlow<LanguageEffect>()
    val effect: SharedFlow<LanguageEffect> = _effect.asSharedFlow()

    // ===== 处理用户意图 =====
    fun handleIntent(intent: LanguageIntent) {
        when (intent) {
            is LanguageIntent.LoadSavedLanguage -> loadSavedLanguage()
            is LanguageIntent.SelectLanguage -> selectLanguage(intent.languageCode)
        }
    }

    // ===== 业务逻辑 =====

    /**
     * 从 SharedPreferences 加载已保存的语言
     */
    private fun loadSavedLanguage() {
        val savedLanguage = getSavedLanguageCode(context)
        _state.update { it.copy(selectedLanguage = savedLanguage) }
    }

    /**
     * 保存新语言并触发导航效果
     */
    private fun selectLanguage(languageCode: String) {
        // 1. 保存到 SharedPreferences
        saveLanguageCode(context, languageCode)

        // 2. 更新状态
        _state.update { it.copy(selectedLanguage = languageCode) }

        // 3. 触发导航效果（重启 App）
        viewModelScope.launch {
            _effect.emit(LanguageEffect.NavigateToMain)
        }
    }
}