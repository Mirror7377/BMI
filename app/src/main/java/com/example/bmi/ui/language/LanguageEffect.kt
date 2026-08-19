package com.example.bmi.ui.language

sealed class LanguageEffect {
    /**
     * 导航到主页面
     */
    data object NavigateToMain : LanguageEffect()
}