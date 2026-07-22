package com.example.bmi.ui.profile

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.bmi.databinding.ActivityProfileBinding
import com.example.bmi.databinding.DialogLoginBinding
import com.example.bmi.databinding.DialogLogoutBinding
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

        // 同步图标（独立点击，不冲突）
        binding.ivExtraIcon.setOnClickListener {
            showSyncIssueDialog()
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
        // 未登录组（显示 Backup & Restore 等）
        binding.tvBackupRestore.isVisible = !state.isLoggedIn
        binding.ivBackupIcon.isVisible = !state.isLoggedIn
        binding.tvSync.isVisible = !state.isLoggedIn
        binding.ivExtraIcon.isVisible = !state.isLoggedIn

        // 登录组（显示头像、姓名、邮箱）
        binding.loginGroup.isVisible = state.isLoggedIn
        if (state.isLoggedIn) {
            binding.tvNameLogin.text = state.userName
            binding.tvEmailLogin.text = state.userEmail
        }
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
        // 使用 ViewBinding 加载布局
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

        // 通过 binding 设置点击
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
     * 同步问题弹窗（同样改用 ViewBinding，假设布局为 dialog_sync_issue.xml）
     */
    private fun showSyncIssueDialog() {
        // 如果之前已实现，建议也改为 binding
        // 由于用户说“可保留”，此处留空或按需实现
        // 示例（假设已有 DialogSyncIssueBinding）：
        /*
        val dialogBinding = DialogSyncIssueBinding.inflate(layoutInflater)
        val dialog = Dialog(this).apply {
            setContentView(dialogBinding.root)
            setCancelable(true)
            setCanceledOnTouchOutside(true)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            val lp = window?.attributes
            lp?.gravity = Gravity.CENTER
            window?.attributes = lp
        }
        // ... 设置文本和按钮点击
        dialog.show()
        */
    }
}