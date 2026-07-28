package com.example.bmi.ui.language

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.bmi.BaseActivity
import com.example.bmi.MainActivity
import com.example.bmi.R
import com.example.bmi.databinding.ActivityLanguageBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LanguageActivity : BaseActivity() {

    private lateinit var binding: ActivityLanguageBinding

    private val viewModel: LanguageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeState()
        observeEffect()

        // 加载已保存的语言
        viewModel.handleIntent(LanguageIntent.LoadSavedLanguage)
    }

    // ========== 设置监听器 ==========
    private fun setupListeners() {
        binding.ivBack.setOnClickListener { finish() }
        setupLanguageClicks()
    }

    // ========== 语言点击映射 ==========
    private fun setupLanguageClicks() {
        val langClickMap = mapOf(
            binding.lang1 to "en",
            binding.lang2 to "pt",
            binding.lang3 to "ru",
            binding.lang4 to "pt",
            binding.lang5 to "de",
            binding.lang6 to "zh-TW",
            binding.lang7 to "zh-CN",
            binding.lang8 to "fr",
            binding.lang9 to "es",
            binding.lang10 to "it",
            binding.lang11 to "ko"
        )

        //遍历列表
        langClickMap.forEach { (view, langCode) ->
            //为当前遍历到的控件设置点击监听
            view.setOnClickListener {
                viewModel.handleIntent(LanguageIntent.SelectLanguage(langCode))
            }
        }
    }

    // ========== 观察状态 ==========
    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    // ========== 观察副作用 ==========
    private fun observeEffect() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        is LanguageEffect.NavigateToMain -> navigateToMain()
                    }
                }
            }
        }
    }

    // ========== UI 渲染 ==========
    private fun renderState(state: LanguageState) {
        val checkViews = mapOf(
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

        //隐藏所有勾选标记
        checkViews.values.forEach { it.visibility = View.GONE }

        val langIdMap = mapOf(
            "en" to R.id.lang1,
            "pt" to R.id.lang2,
            "ru" to R.id.lang3,
            "de" to R.id.lang5,
            "zh-TW" to R.id.lang6,
            "zh-CN" to R.id.lang7,
            "fr" to R.id.lang8,
            "es" to R.id.lang9,
            "it" to R.id.lang10,
            "ko" to R.id.lang11
        )
        //获取当前选中的语言，然后显示对应的选中图标
        val targetViewId = langIdMap[state.selectedLanguage]
        targetViewId?.let { checkViews[it]?.visibility = View.VISIBLE }
    }

    // ========== 导航 ==========
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        //新的任务栈中启动该 Activity    在启动 MainActivity 之前，彻底销毁（清空）当前任务栈中的所有 Activity。
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}