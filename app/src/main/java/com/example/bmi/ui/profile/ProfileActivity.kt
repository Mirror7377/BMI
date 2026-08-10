package com.example.bmi.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bmi.BaseActivity
import com.example.bmi.ui.feedback.FeedbackActivity
import com.example.bmi.ui.language.LanguageActivity
import com.example.bmi.ui.profile.components.LoginDialog
import com.example.bmi.ui.profile.components.LogoutDialog
import com.example.bmi.ui.theme.BmiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileActivity : ComponentActivity() {

    private val viewModel: ProfileViewModel by viewModels()
    private var showLoginDialog by mutableStateOf(false)
    private var showUserInfoDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel = hiltViewModel<ProfileViewModel>()

            BmiTheme {
                ProfileScreen(
                    viewModel = viewModel,
                    onNavigateToLanguage = {
                        startActivity(Intent(this, LanguageActivity::class.java))
                    },
                    onNavigateToFeedback = {
                        startActivity(Intent(this, FeedbackActivity::class.java))
                    },
                    onShowLoginDialog = { showLoginDialog = true },
                    onShowUserInfoDialog = { showUserInfoDialog = true },
                    onNavigateBack = { finish() },
                    onRateUs = {
                        val url = "https://play.google.com/store/apps/details?id=bmicalculator.bmi.calculator.weightlosstracker"
                        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                        if (intent.resolveActivity(packageManager) != null) {
                            startActivity(intent)
                        }
                    }
                )

                // 登录弹窗
                if (showLoginDialog) {
                    LoginDialog(
                        onDismiss = { showLoginDialog = false },
                        onLogin = {
                            showLoginDialog = false
                            viewModel.handleIntent(ProfileIntent.Login)
                        }
                    )
                }

                // 登出弹窗
                if (showUserInfoDialog) {
                    LogoutDialog(
                        onDismiss = { showUserInfoDialog = false },
                        onLogout = {
                            showUserInfoDialog = false
                            viewModel.handleIntent(ProfileIntent.Logout)
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkFeedbackContent()
    }
}