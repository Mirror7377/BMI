package com.example.bmi.ui.main

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.bmi.ui.display.DisplayScreen
import com.example.bmi.ui.feedback.FeedbackScreen
import com.example.bmi.ui.historydetai.HistoryDetailScreen
import com.example.bmi.ui.home.HomeScreen
import com.example.bmi.ui.language.LanguageScreen
import com.example.bmi.ui.main.components.BottomNavigationBar
import com.example.bmi.ui.navigation.DisplayRoute
import com.example.bmi.ui.navigation.FeedbackRoute
import com.example.bmi.ui.navigation.HistoryDetailRoute
import com.example.bmi.ui.navigation.HomeRoute
import com.example.bmi.ui.navigation.LanguageRoute
import com.example.bmi.ui.navigation.ProfileRoute
import com.example.bmi.ui.navigation.RecentRoute
import com.example.bmi.ui.navigation.ResultRoute
import com.example.bmi.ui.navigation.SplashRoute
import com.example.bmi.ui.navigation.StatisticsRoute
import com.example.bmi.ui.profile.ProfileScreen
import com.example.bmi.ui.recent.RecentScreen
import com.example.bmi.ui.result.ResultScreen
import com.example.bmi.ui.splash.SplashScreen
import com.example.bmi.ui.statistics.StatisticsScreen
import com.example.bmi.ui.theme.BmiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val context = applyLanguage(newBase)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            BmiTheme {
                val navController = rememberNavController()
                BmiApp(navController = navController)
            }
        }
    }

    companion object {
        fun applyLanguage(context: Context): Context {
            val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            val langCode = prefs.getString("language", "en") ?: "en"
            val locale = java.util.Locale.forLanguageTag(langCode)
            java.util.Locale.setDefault(locale)

            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            config.setLayoutDirection(locale)

            return context.createConfigurationContext(config)
        }
    }
}

@Composable
fun BmiApp(
    navController: NavHostController,
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val isBottomBarVisible by mainViewModel.isBottomBarVisible.collectAsStateWithLifecycle()

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val isBottomNavRoute = currentRoute in listOf(
        HomeRoute::class.qualifiedName,
        DisplayRoute::class.qualifiedName,
        StatisticsRoute::class.qualifiedName
    )

    val showBottomBar = isBottomBarVisible && isBottomNavRoute

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        BmiNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun BmiNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    NavHost(
        navController = navController,
        startDestination = SplashRoute,
        modifier = modifier
    ) {
        composable<SplashRoute> {
            SplashScreen(
                viewModel = hiltViewModel(),
                onNavigate = { hasData ->
                    val destination = if (hasData) DisplayRoute else HomeRoute
                    navController.navigate(destination) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                }
            )
        }

        composable<HomeRoute> { backStackEntry ->
            // 读取 ResultScreen 返回时设置的标记
            val showDeleteSuccess by backStackEntry.savedStateHandle
                .getStateFlow("show_delete_success", false)
                .collectAsStateWithLifecycle()

            HomeScreen(
                showDeleteSuccess = showDeleteSuccess,
                onConsumeDeleteSuccess = {
                    // 消费掉，防止重组时重复显示
                    backStackEntry.savedStateHandle.remove<Boolean>("show_delete_success")
                },
                onNavigateToResult = { recordJson ->
                    navController.navigate(ResultRoute(recordJson))
                },
                onNavigateToProfile = {
                    navController.navigate(ProfileRoute)
                }
            )
        }

        composable<DisplayRoute> {
            DisplayScreen(
                onNavigateToRecent = {
                    navController.navigate(RecentRoute)
                }
            )
        }

        composable<StatisticsRoute> {
            StatisticsScreen(
                onNavigateToHome = {
                    navController.navigate(HomeRoute) {
                        popUpTo(HomeRoute) { inclusive = true }
                    }
                }
            )
        }

        composable<ResultRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ResultRoute>()
            ResultScreen(
                recordJson = route.recordJson,
                onNavigateBack = {
                    //获取当前页面回退栈中的上一级条目
                    navController.previousBackStackEntry
                        //安全地获取该上一级页面的 SavedStateHandle
                        ?.savedStateHandle
                        ?.set("show_delete_success", true)
                    navController.popBackStack()
                },
                onNavigateToMain = {
                    navController.navigate(DisplayRoute) {
                        popUpTo(HomeRoute) { inclusive = false }
                    }
                }
            )
        }

        composable<RecentRoute> {
            RecentScreen(
                viewModel = hiltViewModel(),
                onRecordClick = { recordId ->
                    navController.navigate(HistoryDetailRoute(recordId))
                },
                onNavigateBack = {
                    navController.navigateUp()//回到当前页面的逻辑上一级
                }
            )
        }

        composable<HistoryDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<HistoryDetailRoute>()
            HistoryDetailScreen(
                recordId = route.recordId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(HomeRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable<ProfileRoute> {
            ProfileScreen(
                onNavigateToLanguage = {
                    navController.navigate(LanguageRoute)
                },
                onNavigateToFeedback = {
                    navController.navigate(FeedbackRoute)
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onRateUs = {
                    val url = "https://play.google.com/store/apps/details?id=bmicalculator.bmi.calculator.weightlosstracker"
                    context.openUrl(url)
                }
            )
        }

        composable<FeedbackRoute> {
            FeedbackScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }

        composable<LanguageRoute> {
            LanguageScreen(
                viewModel = hiltViewModel(),
                onNavigateToMain = {
                    // 重建 Activity
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent)
                },
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }
    }
}

fun Context.openUrl(url: String): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        // 强制用外部浏览器，不经过应用内选择器
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
        true
    } else {
        false
    }
}