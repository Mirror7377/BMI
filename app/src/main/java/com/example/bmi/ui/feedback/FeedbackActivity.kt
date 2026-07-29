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
import com.example.bmi.BaseActivity
import com.example.bmi.ui.profile.ProfileActivity

@AndroidEntryPoint
class FeedbackActivity : BaseActivity() {

    private lateinit var binding: ActivityFeedbackBinding
    private val viewModel: FeedbackViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 读取按钮的底部外边距 使按钮能动态调整位置
        val originalBottomMargin = resources.getDimensionPixelSize(R.dimen.feedback_bottom_margin)

        //设置一个 窗口插入监听器
        ViewCompat.setOnApplyWindowInsetsListener(binding.feedbackContainer) { view, insets ->
            //从 insets 中提取 键盘输入法（IME）的底部插入高度
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            //动态更新 feedbackContainer 的 bottomMargin 属性。
            view.updateLayoutParams<ConstraintLayout.LayoutParams> {
                bottomMargin = originalBottomMargin + imeBottom
            }
            insets
        }

        setupListeners()
        // 初始按钮置灰
        updateSaveButtonState(false)

    }

    private fun setupListeners() {
        // 返回按钮
        binding.ivBack.setOnClickListener {
            finish()
        }

        // 提交按钮
        binding.btnSave.setOnClickListener {
            val feedbackText = binding.etFeedback.text.toString().trim()
            if (feedbackText.isNotEmpty()) {
                val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                prefs.edit().putString("feedback_content", feedbackText).apply()
                finish()
            }
        }

        // 输入框监听（控制按钮启用/禁用）
        binding.etFeedback.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val hasText = !s.isNullOrBlank()
                updateSaveButtonState(hasText)
            }
        })
    }

    private fun updateSaveButtonState(enabled: Boolean) {
        val bgColor = if (enabled) {
            ContextCompat.getColor(this, R.color.splash_blue)
        } else {
            ContextCompat.getColor(this, R.color.bg_gray)
        }
        binding.btnSave.setCardBackgroundColor(bgColor)
    }
}