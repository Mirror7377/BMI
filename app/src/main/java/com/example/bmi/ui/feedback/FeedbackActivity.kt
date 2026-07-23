package com.example.bmi.ui.feedback

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.bmi.R
import com.example.bmi.databinding.ActivityFeedbackBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.example.bmi.ui.profile.ProfileActivity

@AndroidEntryPoint
class FeedbackActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedbackBinding
    private val viewModel: FeedbackViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 使按钮能动态调整位置
        val originalBottomMargin =
            resources.getDimensionPixelSize(R.dimen.feedback_bottom_margin)

        ViewCompat.setOnApplyWindowInsetsListener(binding.feedbackContainer) { view, insets ->

            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom

            view.updateLayoutParams<ConstraintLayout.LayoutParams> {
                bottomMargin = originalBottomMargin + imeBottom
            }

            insets
        }

        setupListeners()
        observeState()
        observeEffect()

        // 监听输入框变化
        binding.etFeedback.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val hasText = !s.isNullOrBlank()
                updateSaveButtonState(hasText)
            }
        })
        // 初始状态（无内容）
        updateSaveButtonState(false)

        viewModel.handleIntent(FeedbackIntent.Init)


    }

    private fun updateSaveButtonState(enabled: Boolean) {
        //todo 按钮置灰，动态显示如何实现
        val bgColor = if (enabled) {
            ContextCompat.getColor(this, R.color.splash_blue) // #3659CF
        } else {
            ContextCompat.getColor(this, R.color.bg_gray) // #EAEAEE
        }
        binding.btnSave.setCardBackgroundColor(bgColor)
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            val text = binding.etFeedback.text.toString().trim()
            if (text.isEmpty()) {
            } else {
                // 然后关闭当前 Activity 返回上一页
                finish()
            }
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: FeedbackState) {
        // 可处理加载状态、错误等
        if (state.isLoading) {
            // 显示进度
        }
        state.errorMessage?.let {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeEffect() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        is FeedbackEffect.ShowToast -> {
                            Toast.makeText(this@FeedbackActivity, effect.message, Toast.LENGTH_SHORT).show()
                        }
                        is FeedbackEffect.NavigateBack -> finish()
                    }
                }
            }
        }
    }
}