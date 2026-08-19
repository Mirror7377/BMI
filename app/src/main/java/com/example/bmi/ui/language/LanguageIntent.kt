package com.example.bmi.ui.language

sealed class LanguageIntent {
    /**
     * 加载已保存的语言
     */
    data object LoadSavedLanguage : LanguageIntent()

    data class SelectLanguage(val languageCode: String) : LanguageIntent()
}