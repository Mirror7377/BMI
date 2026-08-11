package com.example.bmi.ui.recent

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bmi.BaseActivity
import com.example.bmi.ui.historydetai.HistoryDetailActivity
import com.example.bmi.ui.theme.BmiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecentActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BmiTheme {
                RecentScreen(
                    viewModel = hiltViewModel(),
                    onRecordClick = { recordId ->
                        val intent = Intent(this, HistoryDetailActivity::class.java).apply {
                            putExtra("RECORD_ID", recordId)
                        }
                        startActivity(intent)
                    },
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}