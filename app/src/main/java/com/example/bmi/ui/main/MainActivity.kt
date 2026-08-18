package com.example.bmi.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bmi.BaseActivity
import com.example.bmi.ui.main.components.BottomNavigationBar
import com.example.bmi.ui.display.DisplayScreen
import com.example.bmi.ui.home.HomeScreen
import com.example.bmi.data.enums.Screen
import com.example.bmi.ui.recent.RecentActivity
import com.example.bmi.ui.result.ResultActivity
import com.example.bmi.ui.statistics.StatisticsScreen
import com.example.bmi.ui.theme.BmiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private val viewModel: MainViewModel by viewModels()

    fun goToHome() {
        viewModel.navigateToHome()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            BmiTheme {
                val mainViewModel: MainViewModel = hiltViewModel()
                val currentScreen by mainViewModel.currentScreen.collectAsState()
                val isBottomBarVisible by mainViewModel.isBottomBarVisible.collectAsState()

                // 初始化屏幕（Home 或 Display）
                val hasData = intent.getBooleanExtra("hasData", false)
                LaunchedEffect(Unit) {
                    mainViewModel.setInitialScreen(hasData)
                }

                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        if (isBottomBarVisible) {
                            BottomNavigationBar(
                                currentScreen = currentScreen,
                                onNavigate = { screen -> mainViewModel.navigateTo(screen) }
                            )
                        }
                    }
                    //自动计算出底部导航栏占用的空间innerPadding
                ) { innerPadding ->
                    // 根据当前屏幕显示对应的 Composable
                    when (currentScreen) {
                        Screen.Home -> HomeScreen(
                            modifier = Modifier.padding(innerPadding),
                            onNavigateToResult = { record ->
                                // 跳转到 ResultActivity
                                startActivity(
                                    Intent(this@MainActivity, ResultActivity::class.java).apply {
                                        putExtra("BMI_RECORD", record)
                                    }
                                )
                            }
                        )
                        Screen.Display -> DisplayScreen(
                            modifier = Modifier.padding(innerPadding),
                            onNavigateToRecent = {
                                startActivity(Intent(this@MainActivity, RecentActivity::class.java))
                            }
                        )
                        Screen.Statistics -> StatisticsScreen(
                            modifier = Modifier.padding(innerPadding),
                            onNavigateToHome = {
                                mainViewModel.navigateTo(Screen.Home)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 处理从 Result 页面返回后的跳转逻辑
        viewModel.onResume()
    }
}

// 占位屏幕
@Composable
fun PlaceholderScreen(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 24.sp)
    }
}