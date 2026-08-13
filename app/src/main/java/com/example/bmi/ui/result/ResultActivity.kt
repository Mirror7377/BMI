package com.example.bmi.ui.result

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bmi.BaseActivity
import com.example.bmi.ui.theme.BmiTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.content.edit

@AndroidEntryPoint
class ResultActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BmiTheme {
                val viewModel = hiltViewModel<ResultViewModel>()

                // 初始化
                LaunchedEffect(Unit) {
                    //intent：在 Activity 中直接写 intent，是 Kotlin 对 getIntent() 的属性委托。
                    // 它代表启动当前 Activity 的那个 Intent 对象。
                    //.extras：这是 Intent 携带的附加数据（Bundle）。
                    // 所有通过 putExtra("key", value) 传过来的数据，都存放在这个 Bundle 里。
                    viewModel.handleIntent(ResultIntent.Init(intent.extras))
                }

                ResultScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() },
                    onNavigateToMain = { isFirstSave ->
                        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                        val target = if (isFirstSave) "display" else "statistics"
                        prefs.edit { putString("post_save_target", target) }
                        finish()
                    }
                )
            }
        }
    }
}