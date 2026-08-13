package com.example.bmi.ui.historydetai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bmi.R
import com.example.bmi.ui.bmigauge.BmiConfigProvider
import com.example.bmi.ui.bmigauge.BmiGauge
import com.example.bmi.ui.common.AppRecommendations
import com.example.bmi.ui.common.BmiLegend
import com.example.bmi.ui.common.BmiLegendSheetContent
import com.example.bmi.ui.common.DiscardConfirmDialog
import com.example.bmi.ui.common.HealthTip
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    viewModel: HistoryDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    showDeleteDialog: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val record = state.record

    // 本地状态
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showBmiLegendSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { true }
    )

    // 监听 Effect
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HistoryDetailEffect.NavigateBack -> onNavigateBack()
                is HistoryDetailEffect.NavigateToHome -> onNavigateToHome()
                is HistoryDetailEffect.ShowDeleteDialog -> showDeleteDialog()
                is HistoryDetailEffect.ShowBmiLegend -> { showBmiLegendSheet = true }
            }
        }
    }

    val bgRed = Color(0xFFE80C0C)
    val bgWhite = Color(0xFFFFFFFF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgWhite)
    ) {
        if (record == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No data available",
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_regular))
                )
            }
        } else {
            // ===== 顶部导航栏 =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp)
                    .padding(top = 10.dp, bottom = 10.dp)
                    .statusBarsPadding()
                    .align(Alignment.TopStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_left),
                    contentDescription = "Back",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onNavigateBack() },
                )

                Text(
                    text = stringResource(R.string.delete),
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_regular)),
                    modifier = Modifier.clickable {
                        showDiscardDialog = true
                    }
                )
            }

            // ===== 可滚动内容 =====
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 100.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 80.dp)
            ) {
                // 仪表盘
                val config = BmiConfigProvider.getConfig(record.age, record.gender)
                BmiGauge(
                    config = config,
                    bmi = record.bmi,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 16.dp),
                    showPointer = true,
                    animate = false  // 无动画，直接显示
                )

                Spacer(modifier = Modifier.height(16.dp))

                // "Your BMI is" 标题
                Text(
                    text = stringResource(R.string.your_bmi_is),
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .offset(y = 10.dp),
                )

                // BMI 数值（直接显示，无动画）
                Text(
                    text = String.format("%.1f", record.bmi),
                    fontSize = 64.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 状态标签
                Card(
                    modifier = Modifier
                        .wrapContentWidth()
                        .align(Alignment.CenterHorizontally)
                        .clickable {
                            viewModel.handleIntent(HistoryDetailIntent.ShowBmiLegend)
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(state.bmiLevel.cardBgColor)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(state.bmiLevel.statusTextRes),
                            color = Color.White,
                            fontSize = 19.sp,
                            fontFamily = FontFamily(Font(R.font.montserrat_extrabold))
                        )
                        // 显示信息图标（有历史记录时，本页总有记录，所以显示）
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_info_outline),
                            contentDescription = "Info",
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // BMI 信息
                val weightText = String.format("%.2f %s", record.weightInput, record.weightUnit.lowercase())
                val heightText = if (record.heightUnit == "FT_IN") {
                    "${record.feetInput ?: 0} ft ${record.inchesInput ?: 0} in"
                } else {
                    String.format("%.1f cm", record.heightCm)
                }
                val genderText = if (record.gender == "MALE") {
                    stringResource(R.string.gender_male)
                } else {
                    stringResource(R.string.gender_female)
                }
                val ageText = stringResource(R.string.age_years_old, record.age)

                Text(
                    text = "$weightText | $heightText | $genderText | $ageText",
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_regular)),
                    color = Color(0xFF888888),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 健康建议
                HealthTip(
                    bmiLevel = state.bmiLevel,
                    record = record
                )


                // 格式化日期
                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                val dateStr = dateFormat.format(record.timestamp)
                val dateTime = "$dateStr ${record.timeOfDay}"

                if (state.recommendedApps.isNotEmpty()) {
                    AppRecommendations(
                        apps = state.recommendedApps,
                        dateTime = dateTime
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // ===== 删除确认对话框 =====
            if (showDiscardDialog) {
                DiscardConfirmDialog(
                    onDismiss = { showDiscardDialog = false },
                    onConfirm = {
                        showDiscardDialog = false
                        viewModel.handleIntent(HistoryDetailIntent.DeleteRecord)
                    }
                )
            }

            // ===== BMI 图例弹窗 =====
            if (showBmiLegendSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showBmiLegendSheet = false },
                    sheetState = sheetState,
                    containerColor = Color.White,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    dragHandle = null
                ) {
                    BmiLegendSheetContent(
                        bmiLevel = state.bmiLevel,
                        age = record.age,
                        gender = record.gender,
                        bmi = record.bmi,
                        onGotIt = { showBmiLegendSheet = false }
                    )
                }
            }
        }
    }
}