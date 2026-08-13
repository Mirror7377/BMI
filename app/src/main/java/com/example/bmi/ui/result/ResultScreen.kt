package com.example.bmi.ui.result

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bmi.R
import com.example.bmi.data.database.BmiRecord
import com.example.bmi.data.database.RecommendApp
import com.example.bmi.ui.bmigauge.BmiConfigProvider
import com.example.bmi.ui.bmigauge.BmiGauge
import com.example.bmi.ui.bmigauge.BmiLevel
import com.example.bmi.utils.BmiUiUtils
import com.example.bmi.utils.UnitConverter
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bmi.ui.common.DiscardConfirmDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ResultScreen(
    viewModel: ResultViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToMain: (Boolean) -> Unit,
    onShowDiscardDialog: () -> Unit,
    onShowBmiLegend: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val record = state.record

    // 本地状态：控制弹窗显示
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showBmiLegendSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { true }
    )

    // 拦截返回键
    BackHandler(enabled = true) {
        showDiscardDialog = true
    }

    // 监听 Effect
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ResultEffect.NavigateToHome -> onNavigateToMain(effect.isFirstSave)
            }
        }
    }

    val bgGray = Color(0xFFEAEAEE)
    val bgWhite = Color(0xFFFFFFFF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgWhite)
            .systemBarsPadding()//处理顶部和底部导航栏
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
            // ===== Discard 文字（遮阳布效果，需要白色背景）=====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 15.dp, top = 10.dp, bottom = 10.dp)
                    .background(bgWhite)  // 白色背景，盖住下面的内容
                    .align(Alignment.TopStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.discard),
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_regular)),
                    modifier = Modifier.clickable {
                        showDiscardDialog = true
                    }
                )
            }

            // ===== 可滚动内容 =====
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 45.dp)
                    .align(Alignment.TopStart)
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
                    animate = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ---- "Your BMI is" 标题 ----
                Text(
                    text = stringResource(R.string.your_bmi_is),
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
                    color = Color(0xFF222222),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // ---- BMI 数值动画 ----
                val animatedBmi = remember { Animatable(0f) }  // 使用 Float
                LaunchedEffect(record.bmi) {
                    animatedBmi.animateTo(
                        targetValue = record.bmi.toFloat(),
                        animationSpec = tween(
                            durationMillis = 800,
                            easing = FastOutSlowInEasing
                        )
                    )
                }

                Text(
                    text = String.format("%.1f", animatedBmi.value),
                    fontSize = 64.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ---- 状态标签 ----
                Card(
                    modifier = Modifier
                        .wrapContentWidth()
                        .align(Alignment.CenterHorizontally)
                        .clickable {
                            showBmiLegendSheet = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(state.bmiLevel.cardBgColor)
                    )
                )  {
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
                        if (state.hasSavedRecord) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.ic_info_outline),
                                contentDescription = "Info",
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ---- BMI 信息 ----
                val weightText =
                    String.format("%.2f %s", record.weightInput, record.weightUnit.lowercase())
                val heightText = if (record.heightUnit == "FT_IN") {
                    "${record.feetInput ?: 0} ft ${record.inchesInput ?: 0} in"
                } else {
                    String.format("%.1f cm", record.heightCm)
                }
                val genderText =
                    if (record.gender == "MALE") stringResource(R.string.gender_male) else stringResource(
                        R.string.gender_female
                    )
                val ageText = stringResource(R.string.age_years_old, record.age)

                Text(
                    text = "$weightText | $heightText | $genderText | $ageText",
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_regular)),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )


                // ---- 底部内容（图例 或 广告） ----
                if (!state.hasSavedRecord) {
                    // 无历史记录：显示图例
                    BmiLegend(
                        bmiLevel = state.bmiLevel,
                        age = record.age,
                        gender = record.gender,
                        bmi = record.bmi
                    )
                    HealthTip(
                        bmiLevel = state.bmiLevel,
                        record = record
                    )
                } else {
                    Spacer(modifier = Modifier.height(15.dp))
                    // 有历史记录：显示健康建议 + 广告推荐
                    HealthTip(
                        bmiLevel = state.bmiLevel,
                        record = record
                    )
                    Spacer(modifier = Modifier.height(30.dp))

                    if (state.recommendedApps.isNotEmpty()) {
                        AppRecommendations(apps = state.recommendedApps)
                    }
                }
                Spacer(modifier = Modifier.height(100.dp))
            }


            // ===== 底部 SAVE 按钮（悬浮效果，只有按钮本身遮挡内容）=====
            Button(
                onClick = { viewModel.handleIntent(ResultIntent.SaveRecord) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .align(Alignment.BottomCenter)
                    .height(55.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3659CF)
                )
            ) {
                Text(
                    text = stringResource(R.string.save),
                    fontSize = 20.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
                    color = Color.White
                )
            }

            // ===== Discard 确认对话框 =====
            if (showDiscardDialog) {
                DiscardConfirmDialog(
                    onDismiss = { showDiscardDialog = false },
                    onConfirm = {
                        showDiscardDialog = false
                        onNavigateBack()
                    }
                )
            }

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
// ---- 图例组件 ----
@Composable
private fun BmiLegend(
    bmiLevel: BmiLevel,
    age: Int,
    gender: String,
    bmi: Double
) {
    val isChild = age in 2..20
    val config = BmiConfigProvider.getConfig(age, gender)
    val splitPoints = config.splitPoints

    val visibleIndices = if (isChild) listOf(2, 3, 4, 5) else (0..7).toList()
    val legendLevels = listOf(
        BmiLevel.VERY_SEVERELY_UNDERWEIGHT,
        BmiLevel.SEVERELY_UNDERWEIGHT,
        BmiLevel.UNDERWEIGHT,
        BmiLevel.NORMAL,
        BmiLevel.OVERWEIGHT,
        BmiLevel.OBESE_CLASS_I,
        BmiLevel.OBESE_CLASS_II,
        BmiLevel.OBESE_CLASS_III
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            legendLevels.forEachIndexed { index, level ->
                if (!visibleIndices.contains(index)) return@forEachIndexed

                val isHighlighted = level == bmiLevel
                val levelColor = Color(level.cardBgColor)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .then(
                            if (isHighlighted) {
                                Modifier.background(
                                    color = levelColor,
                                    shape = RoundedCornerShape(15.dp)
                                )
                            } else {
                                Modifier
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(15.dp))  // 左边距 15dp
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (isHighlighted) Color.White else levelColor,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))

                    // 等级名称
                    Text(
                        text = stringResource(level.statusTextRes),
                        fontSize = 14.sp,
                        fontFamily = if (isHighlighted) {
                            FontFamily(Font(R.font.montserrat_extrabold))
                        } else {
                            FontFamily(Font(R.font.montserrat_regular))
                        },
                        color = if (isHighlighted) Color.White else Color(0xFF000000),
                        modifier = Modifier.weight(1f)
                    )

                    // 范围
                    val rangeText = if (isChild) {
                        when (index) {
                            2 -> "＜${splitPoints[0]}"
                            3 -> "${splitPoints[0]} - ${splitPoints[1]}"
                            4 -> "${splitPoints[1]} - ${splitPoints[2]}"
                            5 -> "≥${splitPoints[2]}"
                            else -> ""
                        }
                    } else {
                        when (index) {
                            0 -> "＜16"
                            1 -> "16.0-16.9"
                            2 -> "17.0-18.4"
                            3 -> "18.5-24.9"
                            4 -> "25.0-29.9"
                            5 -> "30.0-34.9"
                            6 -> "35.0-39.9"
                            7 -> "≥40.0"
                            else -> ""
                        }
                    }
                    Text(
                        text = rangeText,
                        fontSize = 14.sp,
                        fontFamily = if (isHighlighted) {
                            FontFamily(Font(R.font.montserrat_extrabold))
                        } else {
                            FontFamily(Font(R.font.montserrat_regular))
                        },
                        color = if (isHighlighted) Color.White else Color(0xFF000000),
                        modifier = Modifier.padding(end = 15.dp)
                    )
                }
            }
        }
    }
}

// ---- 健康建议组件 ----
@Composable
private fun HealthTip(
    bmiLevel: BmiLevel,
    record: BmiRecord
) {
    val (stdMinKg, stdMaxKg) = BmiUiUtils.getStandardWeightRangeCm(
        record.heightCm,
        record.age,
        record.gender
    )
    val isUserKg = record.weightUnit == "KG"
    val (stdMinShow, stdMaxShow, userWeightShow) = if (isUserKg) {
        Triple(stdMinKg, stdMaxKg, record.weightInput)
    } else {
        Triple(
            UnitConverter.kgToLb(stdMinKg),
            UnitConverter.kgToLb(stdMaxKg),
            record.weightInput
        )
    }
    val unitStr = if (isUserKg) "kg" else "lb"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F4F4)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 描述文字
            Text(
                text = stringResource(bmiLevel.descTextRes),
                fontSize = 14.sp,
                lineHeight = 16.sp,
                fontFamily = FontFamily(Font(R.font.montserrat_regular)),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4
            )

            if (bmiLevel != BmiLevel.NORMAL) {

                Spacer(modifier = Modifier.height(4.dp))

                // "Normal weight for height ..."
                val heightDisplayText = if (record.heightUnit == "FT_IN") {
                    "${record.feetInput ?: 0} ft ${record.inchesInput ?: 0} in"
                } else {
                    String.format("%.1f cm", record.heightCm)
                }
                Text(
                    text = stringResource(R.string.normal_weight_for_height, heightDisplayText),
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_regular)),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 范围 + 差值（高亮）
                val rangeStr = String.format("%.1f%s - %.1f%s", stdMinShow, unitStr, stdMaxShow, unitStr)
                val diffValue = if (userWeightShow < stdMinShow) {
                    stdMinShow - userWeightShow
                } else {
                    userWeightShow - stdMaxShow
                }
                val diffSign = if (userWeightShow < stdMinShow) "+" else "-"
                val diffText = String.format(" (%s%.1f%s)", diffSign, diffValue, unitStr)

                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                fontFamily = FontFamily(Font(R.font.montserrat_extrabold))
                            )
                        ) {
                            append(rangeStr)
                        }
                        withStyle(
                            style = SpanStyle(
                                color = Color(0xFFFF3333),
                                fontFamily = FontFamily(Font(R.font.montserrat_extrabold))
                            )
                        ) {
                            append(diffText)
                        }
                    },
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_regular)),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ---- 广告推荐组件 ----
@Composable
private fun AppRecommendations(apps: List<RecommendApp>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        HorizontalDivider(
            Modifier, thickness = 0.5.dp, color = Color(0xFFCCCCCC)
        )
        Spacer(modifier = Modifier.height(25.dp))

        Text(
            text = stringResource(R.string.apps_you_might_need),
            fontSize = 16.sp,
            fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
        )

        Spacer(modifier = Modifier.height(12.dp))

        apps.forEach { app ->
            AppRecommendationCard(app = app)
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun AppRecommendationCard(app: RecommendApp) {
    val appContext = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F4F4)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val url = "https://play.google.com/store/apps/details?id=${app.packageName}"
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    // 使用 appContext 替代 context
                    if (intent.resolveActivity(appContext.packageManager) != null) {
                        appContext.startActivity(intent)
                    }
                }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        )  {
            // 图标占位
            Image(
                painter = painterResource(id = app.iconResId),
                contentDescription = app.name,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray, RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = app.name,
                    fontSize = 13.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_regular)),
                    maxLines = 1,
                    modifier = Modifier.height(19.dp)
                )
                Text(
                    text = app.category,
                    fontSize = 13.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_regular)),
                    modifier = Modifier.height(19.dp)
                )
                // 五星评分组件
                StarRating(rating = app.rating)
            }
        }
    }
}

@Composable
private fun StarRating(rating: Double) {
    val maxStars = 5
    val fullStars = rating.toInt()          // 满星数量
    val hasHalfStar = rating - fullStars >= 0.5  // 是否有半星

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(19.dp)
    ) {
        repeat(maxStars) { index ->
            val starIcon = when {
                index < fullStars -> Icons.Filled.Star          // 满星
                index == fullStars && hasHalfStar -> Icons.AutoMirrored.Filled.StarHalf  // 半星
                else -> Icons.Outlined.Star                     // 空星
            }

            Icon(
                imageVector = starIcon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = String.format("%.1f", rating),
            fontSize = 12.sp,
            fontFamily = FontFamily(Font(R.font.montserrat_regular)),
        )
    }
}


// ---- 弹窗内容组件 ----
@Composable
fun BmiLegendSheetContent(
    bmiLevel: BmiLevel,
    age: Int,
    gender: String,
    bmi: Double,
    onGotIt: () -> Unit
) {
    val isChild = age in 2..20
    val config = BmiConfigProvider.getConfig(age, gender)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        // 标题
        Text(
            text = if (isChild) stringResource(R.string.bmi_for_teenagers)
            else stringResource(R.string.bmi_for_adults),
            fontSize = 20.sp,
            fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
            color = Color.Black,
            modifier = Modifier.padding(start = 16.dp)
        )

        // 年龄性别（仅儿童显示）
        if (isChild) {
            val genderText = if (gender == "MALE") stringResource(R.string.gender_male)
            else stringResource(R.string.gender_female)
            Text(
                text = stringResource(R.string.age_gender_format, age, genderText),
                fontSize = 14.sp,
                fontFamily = FontFamily(Font(R.font.montserrat_regular)),
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        // 无指针的仪表盘
        BmiGauge(
            config = config,
            bmi = bmi,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            showPointer = false,
            animate = false  // 静态显示
        )

        // 图例列表（复用 BmiLegend）
        BmiLegend(
            bmiLevel = bmiLevel,
            age = age,
            gender = gender,
            bmi = bmi
        )


        // GOT IT 按钮
        Button(
            onClick = onGotIt,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp)
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3659CF)
            )
        ) {
            Text(
                text = stringResource(R.string.got_it),
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}