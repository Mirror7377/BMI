package com.example.bmi.ui.them

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import java.util.Locale
import androidx.core.content.edit

/**
 * CompositionLocal 用于在 Compose 树中全局传递当前语言
 * 替代 BaseActivity 中的 attachBaseContext 逻辑
 */
data class LocalizationState(
    val languageCode: String,
    val locale: Locale
)

// 可被 CompositionLocalProvider 覆盖的只读 Local
val LocalLocalization = compositionLocalOf<LocalizationState> {
    error("No LocalizationState provided! Did you forget to wrap your app in BmiTheme?")
}

/**
 * 获取当前系统语言（作为默认值）
 */
fun getSystemLocale(): Locale {
    return Locale.getDefault()
}

/**
 * 获取 SharedPreferences 中保存的语言代码，若无则返回系统语言
 */
fun getSavedLanguageCode(context: Context): String {
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    return prefs.getString("language", null) ?: Locale.getDefault().language
}

/**
 * 保存语言代码到 SharedPreferences
 */
fun saveLanguageCode(context: Context, languageCode: String) {
    context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        .edit {
            putString("language", languageCode)
        }
}

/**
 * 根据语言代码获取 Locale
 */
fun getLocaleFromCode(languageCode: String): Locale {
    return Locale.forLanguageTag(languageCode)
}