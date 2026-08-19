package com.example.bmi.ui.them

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import java.util.Locale
import androidx.core.content.edit


data class LocalizationState(
    val languageCode: String,
    val locale: Locale
)



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
