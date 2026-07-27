package com.example.bmi.ui.language

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.bmi.BaseActivity
import com.example.bmi.MainActivity
import com.example.bmi.R
import com.example.bmi.databinding.ActivityLanguageBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class LanguageActivity : BaseActivity() {

    private lateinit var binding: ActivityLanguageBinding

    // 注入 ViewModel（这里简单通过委托获取，需确保 Application 中提供 SharedPreferences）
    private val viewModel: LanguageViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val prefs = applicationContext.getSharedPreferences("settings", MODE_PRIVATE)
                return LanguageViewModel(prefs) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 返回按钮
        binding.ivBack.setOnClickListener { finish() }

        // 观察 UI 状态
        viewModel.state
            .onEach { state -> renderState(state) }
            .launchIn(lifecycleScope)

        // 观察副作用（导航）
        viewModel.effect
            .onEach { effect ->
                when (effect) {
                    LanguageViewModel.Effect.NavigateToMain -> navigateToMain()
                }
            }
            .launchIn(lifecycleScope)

        // 加载已保存的语言
        viewModel.handleIntent(LanguageIntent.LoadSavedLanguage)

        // 设置所有语言点击监听
        setupLanguageClicks()
    }

    // ------- UI 渲染 -------
    private fun renderState(state: LanguageState) {
        // 根据选中的语言代码，显示对应的勾选标记
        val langIdMap = mapOf(
            "en" to R.id.lang1,
            "pt" to R.id.lang2,
            "ru" to R.id.lang3,
            // 注意：lang4 也映射到 "pt"，这里保留但可能重复，视布局而定
            "de" to R.id.lang5,
            "zh-TW" to R.id.lang6,
            "zh-CN" to R.id.lang7,
            "fr" to R.id.lang8,
            "es" to R.id.lang9,
            "it" to R.id.lang10,
            "ko" to R.id.lang11
        )

        // 先隐藏所有勾选
        listOf(
            binding.check1, binding.check2, binding.check3, binding.check4,
            binding.check5, binding.check6, binding.check7, binding.check8,
            binding.check9, binding.check10, binding.check11
        ).forEach { it.visibility = View.GONE }

        // 显示选中的勾选
        val targetViewId = langIdMap[state.selectedLanguage]
        when (targetViewId) {
            R.id.lang1 -> binding.check1.visibility = View.VISIBLE
            R.id.lang2 -> binding.check2.visibility = View.VISIBLE
            R.id.lang3 -> binding.check3.visibility = View.VISIBLE
            R.id.lang5 -> binding.check5.visibility = View.VISIBLE
            R.id.lang6 -> binding.check6.visibility = View.VISIBLE
            R.id.lang7 -> binding.check7.visibility = View.VISIBLE
            R.id.lang8 -> binding.check8.visibility = View.VISIBLE
            R.id.lang9 -> binding.check9.visibility = View.VISIBLE
            R.id.lang10 -> binding.check10.visibility = View.VISIBLE
            R.id.lang11 -> binding.check11.visibility = View.VISIBLE
            // lang4 没有对应的 check？如果需要，可以单独处理
            else -> { /* 默认不显示 */ }
        }
    }

    // ------- 点击事件 -------
    private fun setupLanguageClicks() {
        val langClickMap = mapOf(
            binding.lang1 to "en",
            binding.lang2 to "pt",
            binding.lang3 to "ru",
            binding.lang4 to "pt",      // 注意：lang4 也指向 "pt"
            binding.lang5 to "de",
            binding.lang6 to "zh-TW",
            binding.lang7 to "zh-CN",
            binding.lang8 to "fr",
            binding.lang9 to "es",
            binding.lang10 to "it",
            binding.lang11 to "ko"
        )

        langClickMap.forEach { (view, langCode) ->
            view.setOnClickListener {
                viewModel.handleIntent(LanguageIntent.SelectLanguage(langCode))
            }
        }
    }

    // ------- 导航 -------
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}