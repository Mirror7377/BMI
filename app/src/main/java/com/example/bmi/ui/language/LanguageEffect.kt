package com.example.bmi.ui.language

sealed class LanguageEffect {
    /**
     * 导航到主页面（语言切换后重启 App）
     */
    data object NavigateToMain : LanguageEffect()
}