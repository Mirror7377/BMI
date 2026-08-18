package com.example.bmi.ui.display

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bmi.R
import com.example.bmi.ui.bmigauge.BmiClassifier
import com.example.bmi.ui.bmigauge.BmiConfigProvider
import com.example.bmi.ui.bmigauge.BmiGauge
import com.example.bmi.ui.bmigauge.BmiLevel
import com.example.bmi.ui.common.BmiLegend
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayScreen(
    viewModel: DisplayViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    onNavigateToRecent: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val record = state.record

    // 在 UI 层直接计算 bmiLevel（与原始 DisplayFragment 逻辑一致）
    val bmiLevel = if (record != null) {
        if (record.age <= 20) {
            BmiConfigProvider.classifyChild(record.age, record.gender, record.bmi)
        } else {
            BmiClassifier.classifyAdult(record.bmi)
        }
    } else {
        BmiLevel.NORMAL
    }

    LaunchedEffect(Unit) {
        viewModel.handleIntent(DisplayIntent.LoadLatest)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is DisplayEffect.NavigateTo -> {
                    when (effect.destination) {
                        DisplayIntent.Destination.RECENT -> onNavigateToRecent()
                    }
                }
            }
        }
    }

    val bgGray = Color(0xFFEAEAEE)
    val bgWhite = Color(0xFFFFFFFF)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgWhite)
    ) {
        if (record == null) {
            Box(modifier = Modifier.fillMaxSize())
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 15.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 左侧：BMI 标签 + 日期（垂直排列）
                Column(
                ) {
                    // BMI 固定标签（24sp）
                    Text(
                        text =stringResource(R.string.bmi),
                        fontSize = 24.sp,
                        fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
                    )
                    // 日期（原内容）
                    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    Text(
                        text = dateFormat.format(record.timestamp),
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.montserrat_regular)),
                    )
                }
                Text(
                    text = stringResource(R.string.recent),
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_regular)),
                    color = Color(0xFF3659CF),
                    modifier = Modifier.clickable {
                        viewModel.handleIntent(DisplayIntent.NavigateTo(DisplayIntent.Destination.RECENT))
                    }
                )
            }

            // ===== 可滚动内容 =====
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 100.dp)
                    .verticalScroll(rememberScrollState())
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
                    animate = false  // 直接显示，无动画
                )

                Spacer(modifier = Modifier.height(16.dp))

                // "Your BMI is" 标题
                Text(
                    text = stringResource(R.string.your_bmi_is),
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
                    color = Color(0xFF222222),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )


                // BMI 数值（大号）
                Text(
                    text = String.format("%.1f", record.bmi),
                    fontSize = 64.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )


                // 状态标签（与原始 DisplayFragment 一致，无信息图标）
                Card(
                    modifier = Modifier
                        .wrapContentWidth()
                        .align(Alignment.CenterHorizontally),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(bmiLevel.cardBgColor)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(bmiLevel.statusTextRes),
                            color = Color.White,
                            fontSize = 19.sp,
                            fontFamily = FontFamily(Font(R.font.montserrat_extrabold))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // BMI 信息（体重、身高、性别、年龄）
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


                // 图例（完整 BMI 分级）
                BmiLegend(
                    bmiLevel = bmiLevel,
                    age = record.age,
                    gender = record.gender,
                    bmi = record.bmi
                )

            }
        }
    }
}