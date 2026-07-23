package com.example.bmi.ui.profile

import android.app.Dialog
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.bmi.R
import com.example.bmi.databinding.ActivityProfileBinding
import com.example.bmi.databinding.DialogLoginBinding
import com.example.bmi.databinding.DialogLogoutBinding
import com.example.bmi.databinding.DialogSyncIssueBinding
import com.example.bmi.ui.feedback.FeedbackActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeState()
        observeEffect()

        viewModel.handleIntent(ProfileIntent.Init)
    }

    private fun setupListeners() {
        // 返回按钮
        binding.ivBack.setOnClickListener { finish() }

        // 点击第一个容器 -> 根据登录状态弹出不同弹窗
        binding.profileContainer.setOnClickListener {
            if (viewModel.state.value.isLoggedIn) {
                showUserInfoDialog()
            } else {
                showLoginDialog()
            }
        }

        // 同步数据图标
        binding.ivExtraIcon.setOnClickListener {
            showSyncIssueDialog()
        }

        binding.tvFeedback.setOnClickListener {
            startActivity(Intent(this, FeedbackActivity::class.java))
        }

        binding.tvRateUs.setOnClickListener {
            val url = "https://play.google.com/store/apps/details?id=bmicalculator.bmi.calculator.weightlosstracker"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            // 检查是否有应用能处理该 Intent
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "No browser available", Toast.LENGTH_SHORT).show()
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

    private fun renderState(state: ProfileState) {
        val isLoggedIn = state.isLoggedIn

        // 未登录组（显示 Backup & Restore 等）
        binding.tvBackupRestore.isVisible = !isLoggedIn
        binding.tvSync.isVisible = !isLoggedIn

        // 登录组（显示头像、姓名、邮箱）
        binding.loginGroup.isVisible = isLoggedIn
        if (isLoggedIn) {
            binding.tvNameLogin.text = state.userName
            binding.tvEmailLogin.text = state.userEmail
        }

        // ====== 重要：两个图标始终显示 ======
        binding.ivBackupIcon.isVisible = true   // 始终可见
        binding.ivExtraIcon.isVisible = true    // 始终可见

        // 动态切换 ivBackupIcon 的约束
        updateBackupIconConstraints(isLoggedIn)
    }

    /**
     * 动态修改 ivBackupIcon 的约束关系：
     * - 未登录：对齐 tvBackupRestore
     * - 登录后：对齐 tvNameLogin
     */
    private fun updateBackupIconConstraints(isLoggedIn: Boolean) {
        val params = binding.ivBackupIcon.layoutParams as ConstraintLayout.LayoutParams
        if (isLoggedIn) {
            // 登录状态：约束到 tvNameLogin
            params.startToEnd = binding.tvNameLogin.id
            params.topToTop = binding.tvNameLogin.id
            params.bottomToBottom = binding.tvNameLogin.id
            // 清除可能冲突的旧约束
            params.startToStart = ConstraintLayout.LayoutParams.UNSET
            params.endToEnd = ConstraintLayout.LayoutParams.UNSET
        } else {
            // 未登录状态：约束到 tvBackupRestore
            params.startToEnd = binding.tvBackupRestore.id
            params.topToTop = binding.tvBackupRestore.id
            params.bottomToBottom = binding.tvBackupRestore.id
            params.startToStart = ConstraintLayout.LayoutParams.UNSET
            params.endToEnd = ConstraintLayout.LayoutParams.UNSET
        }
        binding.ivBackupIcon.layoutParams = params
        binding.ivBackupIcon.requestLayout() // 刷新布局
    }

    private fun observeEffect() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        is ProfileEffect.ShowToast -> {
                            Toast.makeText(this@ProfileActivity, effect.message, Toast.LENGTH_SHORT).show()
                        }
                        is ProfileEffect.NavigateBack -> finish()
                    }
                }
            }
        }
    }

    // ---------- 弹窗方法（使用 ViewBinding） ----------

    /**
     * 登录弹窗（含 Log in 和 Cancel）
     */
    private fun showLoginDialog() {
        val dialogBinding = DialogLoginBinding.inflate(layoutInflater)
        val dialog = Dialog(this).apply {
            setContentView(dialogBinding.root)
            setCancelable(true)
            setCanceledOnTouchOutside(true)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            val lp = window?.attributes
            lp?.gravity = Gravity.BOTTOM
            lp?.width = ViewGroup.LayoutParams.MATCH_PARENT
            lp?.height = ViewGroup.LayoutParams.WRAP_CONTENT
            window?.attributes = lp
        }

        dialogBinding.btnLogin.setOnClickListener {
            viewModel.handleIntent(ProfileIntent.Login)
            dialog.dismiss()
        }
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * 用户信息弹窗（含 Log out 和 Cancel）
     */
    private fun showUserInfoDialog() {
        val dialogBinding = DialogLogoutBinding.inflate(layoutInflater)
        val dialog = Dialog(this).apply {
            setContentView(dialogBinding.root)
            setCancelable(true)
            setCanceledOnTouchOutside(true)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            val lp = window?.attributes
            lp?.gravity = Gravity.BOTTOM
            lp?.width = ViewGroup.LayoutParams.MATCH_PARENT
            lp?.height = ViewGroup.LayoutParams.WRAP_CONTENT
            window?.attributes = lp
        }

        dialogBinding.btnLogout.setOnClickListener {
            viewModel.handleIntent(ProfileIntent.Logout)
            dialog.dismiss()
        }
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * 同步问题弹窗
     */
    private fun showSyncIssueDialog() {
        val dialogBinding = DialogSyncIssueBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this).apply {
            setView(dialogBinding.root)
            setCancelable(true)
        }.create()

        // 设置背景透明（让 CardView 圆角显示）
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // 组合文本：前半部分蓝色加粗，后半部分默认（黑色常规）
        val fullText = "The function will be back once the issue is solved. We’d greatly appreciate your patience."
        val spannable = SpannableString(fullText)
        val firstPart = "The function will be back once the issue is solved."
        val start = 0
        val end = firstPart.length

        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.splash_blue)),
            start, end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            start, end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        dialogBinding.tvDialogCombined.text = spannable

        // DONE 按钮点击关闭弹窗
        dialogBinding.btnDialogDone.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}