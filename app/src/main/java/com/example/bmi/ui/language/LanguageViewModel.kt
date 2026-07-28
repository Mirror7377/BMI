// LanguageViewModel.kt
package com.example.bmi.ui.language

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    //@param:把这个注解只贴在构造函数的参数上
    //ApplicationContext: Hilt知道要给你全局唯一的 Application 实例（生命周期等于整个 App 进程）
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    //它打开/创建了一个名为“settings”的私有配置文件，让 ViewModel 可以读写键值对数据（比如用户当前选择的语言）。
    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(LanguageState())
    val state: StateFlow<LanguageState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<LanguageEffect>()
    val effect: SharedFlow<LanguageEffect> = _effect.asSharedFlow()

    fun handleIntent(intent: LanguageIntent) {
        when (intent) {
            is LanguageIntent.LoadSavedLanguage -> loadSavedLanguage()
            is LanguageIntent.SelectLanguage -> selectLanguage(intent.langCode)
        }
    }

    private fun loadSavedLanguage() {
        //                                      要读取的键名      默认值   默认值
        val savedLang = prefs.getString("language", "en") ?: "en"
        _state.update { it.copy(selectedLanguage = savedLang) }
    }

    private fun selectLanguage(langCode: String) {
        prefs.edit().putString("language", langCode).apply()
        _state.update { it.copy(selectedLanguage = langCode) }
        viewModelScope.launch {
            _effect.emit(LanguageEffect.NavigateToMain)
        }
    }
}