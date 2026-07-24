package com.example.bmi.ui.language

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.example.bmi.BaseActivity
import com.example.bmi.MainActivity
import com.example.bmi.R
import com.example.bmi.databinding.ActivityLanguageBinding

class LanguageActivity : BaseActivity() {

    private lateinit var binding: ActivityLanguageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivBack.setOnClickListener {
            finish()
        }

        applySavedLanguageSelection()
        setupLanguageSelection()
    }

    private fun applySavedLanguageSelection() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val savedLang = prefs.getString("language", "en") ?: "en"

        val langMap = mapOf(
            R.id.lang1 to binding.check1,
            R.id.lang2 to binding.check2,
            R.id.lang3 to binding.check3,
            R.id.lang4 to binding.check4,
            R.id.lang5 to binding.check5,
            R.id.lang6 to binding.check6,
            R.id.lang7 to binding.check7,
            R.id.lang8 to binding.check8,
            R.id.lang9 to binding.check9,
            R.id.lang10 to binding.check10,
            R.id.lang11 to binding.check11
        )

        // 匹配标准语言标签
        val targetId = when (savedLang) {
            "en" -> R.id.lang1
            "pt" -> R.id.lang2
            "ru" -> R.id.lang3
            "de" -> R.id.lang5
            "zh-TW" -> R.id.lang6      // 改为标准格式
            "zh-CN" -> R.id.lang7      // 改为标准格式
            "fr" -> R.id.lang8
            "es" -> R.id.lang9
            "it" -> R.id.lang10
            "ko" -> R.id.lang11
            else -> R.id.lang1
        }

        langMap.values.forEach { it.visibility = View.GONE }
        langMap[targetId]?.visibility = View.VISIBLE
    }

    private fun setupLanguageSelection() {
        val langItems = listOf(
            binding.lang1 to binding.check1,
            binding.lang2 to binding.check2,
            binding.lang3 to binding.check3,
            binding.lang4 to binding.check4,
            binding.lang5 to binding.check5,
            binding.lang6 to binding.check6,
            binding.lang7 to binding.check7,
            binding.lang8 to binding.check8,
            binding.lang9 to binding.check9,
            binding.lang10 to binding.check10,
            binding.lang11 to binding.check11
        )

        langItems.forEach { (langView, checkView) ->
            langView.setOnClickListener {
                langItems.forEach { (_, check) -> check.visibility = View.GONE }
                checkView.visibility = View.VISIBLE

                val langCode = when (langView.id) {
                    R.id.lang1 -> "en"
                    R.id.lang2 -> "pt"
                    R.id.lang3 -> "ru"
                    R.id.lang4 -> "pt"
                    R.id.lang5 -> "de"
                    R.id.lang6 -> "zh-TW"   // 标准格式
                    R.id.lang7 -> "zh-CN"   // 标准格式
                    R.id.lang8 -> "fr"
                    R.id.lang9 -> "es"
                    R.id.lang10 -> "it"
                    R.id.lang11 -> "ko"
                    else -> "en"
                }

                getSharedPreferences("settings", MODE_PRIVATE)
                    .edit()
                    .putString("language", langCode)
                    .apply()

                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }
}