package com.example.bmi.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bmi.ui.main.MainActivity
import com.example.bmi.ui.splash.SplashScreen
import com.example.bmi.ui.splash.SplashViewModel
import com.example.bmi.ui.theme.BmiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BmiTheme {
                SplashScreen(
                    viewModel = hiltViewModel(),
                    onNavigate = { hasData ->
                        val intent = Intent(this@SplashActivity, MainActivity::class.java).apply {
                            putExtra("hasData", hasData)
                        }
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}