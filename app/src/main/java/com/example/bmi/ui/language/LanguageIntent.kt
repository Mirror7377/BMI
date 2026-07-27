package com.example.bmi.ui.language

sealed class LanguageIntent {
    object LoadSavedLanguage : LanguageIntent()
    data class SelectLanguage(val langCode: String) : LanguageIntent()
}