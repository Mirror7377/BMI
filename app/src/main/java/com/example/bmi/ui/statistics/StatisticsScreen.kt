package com.example.bmi.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bmi.R
import com.example.bmi.data.enums.ChartMode
import com.example.bmi.ui.statistics.components.BmiChartConfig
import com.example.bmi.ui.statistics.components.StatisticsChart
import com.example.bmi.ui.statistics.components.StatisticsPeriodSwitcher
import com.example.bmi.ui.statistics.components.WeightChartConfig

/**
 * 使用示例（在 MainActivity 的 Scaffold content 中）：
 *
 * when (currentScreen) {
 *     Screen.Statistics -> StatisticsScreen(
 *         modifier = Modifier.padding(innerPadding),
 *         onNavigateToHome = { mainViewModel.navigateTo(Screen.Home) }
 *     )
 * }
 */
@Composable
fun StatisticsScreen(
    modifier: Modifier = Modifier,
    onNavigateToHome: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    val textBlack = colorResource(R.color.text_black)
    val bgGray = colorResource(R.color.bg_gray)
    val boldFont = FontFamily(Font(R.font.montserrat_extrabold))
    val regularFont = FontFamily(Font(R.font.montserrat_regular))
    val noRippleInteractionSource = remember { MutableInteractionSource() }

    // 对应原 StatisticsFragment.onResume()：页面重新可见时刷新数据
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                when (state.mode) {
                    ChartMode.DAY -> viewModel.dispatch(StatisticsIntent.LoadDay)
                    ChartMode.WEEK -> viewModel.dispatch(StatisticsIntent.LoadWeek)
                    ChartMode.MONTH -> viewModel.dispatch(StatisticsIntent.LoadMonth)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgGray)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        // ---- 标题 "Statistics" ----
        Text(
            text = stringResource(R.string.statistics),
            fontSize = 24.sp,
            fontFamily = boldFont,
            color = textBlack,
            modifier = Modifier.padding(top = 40.dp)
        )

        // ---- Period Switcher ----
        StatisticsPeriodSwitcher(
            currentMode = state.mode,
            onModeSelected = { mode ->
                when (mode) {
                    ChartMode.DAY -> viewModel.dispatch(StatisticsIntent.LoadDay)
                    ChartMode.WEEK -> viewModel.dispatch(StatisticsIntent.LoadWeek)
                    ChartMode.MONTH -> viewModel.dispatch(StatisticsIntent.LoadMonth)
                }
            },
            modifier = Modifier
                .padding(top = 20.dp)
                .align(Alignment.CenterHorizontally)
        )

        // ---- BMI 区域 ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.bmi),
                fontSize = 18.sp,
                fontFamily = boldFont,
                color = textBlack
            )
            Text(
                text = stringResource(R.string.update),
                fontSize = 16.sp,
                fontFamily = regularFont,
                color = colorResource(R.color.splash_blue),
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = noRippleInteractionSource,
                    onClick = onNavigateToHome
                )
            )
        }

        // BMI 图表容器（修复：使用 Compose background 替代 shape drawable）
        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .width(345.dp)
                .height(237.5.dp)
                .align(Alignment.CenterHorizontally)
                .statisticsCardBackground(),
            contentAlignment = Alignment.Center
        ) {
            StatisticsChart(
                data = state.bmiData,
                mode = state.mode,
                config = remember { BmiChartConfig() },
                modifier = Modifier.fillMaxSize()
            )
        }

        // ---- Weight 区域 ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 25.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.weight),
                fontSize = 18.sp,
                fontFamily = boldFont,
                color = textBlack
            )
            Text(
                text = stringResource(R.string.update),
                fontSize = 16.sp,
                fontFamily = regularFont,
                color = colorResource(R.color.splash_blue),
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = noRippleInteractionSource,
                    onClick = onNavigateToHome
                )
            )
        }

        // Weight 图表容器（修复：使用 Compose background 替代 shape drawable）
        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .width(345.dp)
                .height(237.5.dp)
                .align(Alignment.CenterHorizontally)
                .weightStatisticsCardBackground(),
            contentAlignment = Alignment.Center
        ) {
            StatisticsChart(
                data = state.weightData,
                mode = state.mode,
                config = remember { WeightChartConfig() },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 底部留白
        Spacer(modifier = Modifier.height(30.dp))
    }
}
// 放在 StatisticsScreen.kt 底部，或单独文件
fun Modifier.statisticsCardBackground(): Modifier = this
    .clip(RoundedCornerShape(12.dp))
    .background(
        brush = Brush.horizontalGradient(
            colors = listOf(Color(0xFF5077FA), Color(0xFF264CCA))
        )
    )

fun Modifier.weightStatisticsCardBackground(): Modifier = this
    .clip(RoundedCornerShape(12.dp))
    .background(
        brush = Brush.horizontalGradient(
            colors = listOf(Color(0xFFFF931E), Color(0xFFFF8537))
        )
    )