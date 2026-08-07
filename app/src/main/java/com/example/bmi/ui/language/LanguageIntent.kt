package com.example.bmi.ui.language

sealed class LanguageIntent {
    /**
     * 加载已保存的语言（页面初始化时调用）
     */
    data object LoadSavedLanguage : LanguageIntent()

    /**
     * 用户选择了新的语言
     * @param languageCode 语言代码（如 "en"、"zh-CN"）
     */
    data class SelectLanguage(val languageCode: String) : LanguageIntent()
}