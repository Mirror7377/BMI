package com.example.bmi

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.bmi.utils.DensityUtil
import android.content.Context
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import java.util.Locale

abstract class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    //在 布局解析和资源加载之前修改语言设置
    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("settings", MODE_PRIVATE)
        val langCode = prefs.getString("language", "en") ?: "en"

        val locale = Locale.forLanguageTag(langCode)
        //设为默认语言
        Locale.setDefault(locale)

        //复制一份当前的系统配置（Configuration）
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }
}