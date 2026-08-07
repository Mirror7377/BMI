package com.example.bmi.ui.language

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bmi.MainActivity
import com.example.bmi.ui.theme.BmiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LanguageActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BmiTheme {
                LanguageScreen(
                    viewModel = hiltViewModel(),
                    onNavigateToMain = {
                        // 清空任务栈并跳转到 MainActivity（重启 App 应用新语言）
                        val intent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}