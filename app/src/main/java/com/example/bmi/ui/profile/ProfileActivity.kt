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
import com.example.bmi.ui.language.LanguageActivity
import com.example.bmi.utils.CommonBanner
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
        binding.ivBack.setOnClickListener { finish() }

        binding.profileContainer.setOnClickListener {
            if (viewModel.state.value.isLoggedIn) {
                showUserInfoDialog()
            } else {
                showLoginDialog()
            }
        }

        binding.tvLanguage.setOnClickListener {
            startActivity(Intent(this, LanguageActivity::class.java))
        }

        binding.ivExtraIcon.setOnClickListener {
            showSyncIssueDialog()
        }

        binding.tvFeedback.setOnClickListener {
            startActivity(Intent(this, FeedbackActivity::class.java))
        }

        binding.tvRateUs.setOnClickListener {
            val url = "https://play.google.com/store/apps/details?id=bmicalculator.bmi.calculator.weightlosstracker"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
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

        binding.tvBackupRestore.isVisible = !isLoggedIn
        binding.tvSync.isVisible = !isLoggedIn

        binding.loginGroup.isVisible = isLoggedIn
        if (isLoggedIn) {
            binding.tvNameLogin.text = state.userName
            binding.tvEmailLogin.text = state.userEmail
        }

        binding.ivBackupIcon.isVisible = true
        binding.ivExtraIcon.isVisible = true

        updateBackupIconConstraints(isLoggedIn)
    }

    private fun updateBackupIconConstraints(isLoggedIn: Boolean) {
        val params = binding.ivBackupIcon.layoutParams as ConstraintLayout.LayoutParams
        if (isLoggedIn) {
            params.startToEnd = binding.tvNameLogin.id
            params.topToTop = binding.tvNameLogin.id
            params.bottomToBottom = binding.tvNameLogin.id
            params.startToStart = ConstraintLayout.LayoutParams.UNSET
            params.endToEnd = ConstraintLayout.LayoutParams.UNSET
        } else {
            params.startToEnd = binding.tvBackupRestore.id
            params.topToTop = binding.tvBackupRestore.id
            params.bottomToBottom = binding.tvBackupRestore.id
            params.startToStart = ConstraintLayout.LayoutParams.UNSET
            params.endToEnd = ConstraintLayout.LayoutParams.UNSET
        }
        binding.ivBackupIcon.layoutParams = params
        binding.ivBackupIcon.requestLayout()
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

    // ---------- 弹窗方法 ----------

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
            dialog.dismiss()
            // 直接显示登录成功 Banner
            CommonBanner.show(
                this,
                R.drawable.login,
                "Logged in successfully."
            )
            viewModel.handleIntent(ProfileIntent.Login)
        }
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

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
            dialog.dismiss()
            // 直接显示登出成功 Banner
            CommonBanner.show(
                this,
                R.drawable.logout,
                "Logged out successfully."
            )
            viewModel.handleIntent(ProfileIntent.Logout)
        }
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showSyncIssueDialog() {
        val dialogBinding = DialogSyncIssueBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this).apply {
            setView(dialogBinding.root)
            setCancelable(true)
        }.create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

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

        dialogBinding.btnDialogDone.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

}