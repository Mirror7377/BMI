package com.example.bmi.ui.historydetai

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bmi.BaseActivity
import com.example.bmi.ui.main.MainActivity
import com.example.bmi.ui.theme.BmiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HistoryDetailActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 获取并校验 recordId
        val recordId = intent.getLongExtra("RECORD_ID", 0L)
        if (recordId == 0L) {
            finish()
            return
        }

        setContent {
            BmiTheme {
                val viewModel = hiltViewModel<HistoryDetailViewModel>()

                // 加载数据
                LaunchedEffect(Unit) {
                    viewModel.handleIntent(HistoryDetailIntent.LoadRecord(recordId))
                }

                HistoryDetailScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() },
                    onNavigateToHome = {
                        // 删除后无记录，清空任务栈跳转主页
                        val intent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    },
                    showDeleteDialog = {
                        // 不需要额外逻辑，由 Effect 触发
                    }
                )
            }
        }
    }
}