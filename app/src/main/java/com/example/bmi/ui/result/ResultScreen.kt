package com.example.bmi.ui.result

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
import com.example.bmi.ui.bmigauge.BmiConfigProvider
import com.example.bmi.ui.bmigauge.BmiGauge
import com.example.bmi.ui.common.AppRecommendations
import com.example.bmi.ui.common.BmiLegend
import com.example.bmi.ui.common.BmiLegendSheetContent
import com.example.bmi.ui.common.DiscardConfirmDialog
import com.example.bmi.ui.common.HealthTip
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ResultScreen(
    recordJson: String,  // 新增参数
    onNavigateBack: () -> Unit,
    onNavigateToMain: () -> Unit,
    viewModel: ResultViewModel = hiltViewModel()
) {
    // 初始化时传入 JSON
    LaunchedEffect(recordJson) {
        viewModel.handleIntent(ResultIntent.Init(recordJson))
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showDiscardDialog by remember { mutableStateOf(false) }
    var showBmiLegendSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { true }
    )

    //拦截系统返回键
    BackHandler(enabled = true) {
        showDiscardDialog = true
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ResultEffect.NavigateToHome -> onNavigateToMain()
            }
        }
    }

    val bgGray = Color(0xFFEAEAEE)
    val bgWhite = Color(0xFFFFFFFF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgWhite)
            .systemBarsPadding()
    ) {
        if (state.record == null) {
            Box(modifier = Modifier.fillMaxSize())
        } else {
            val record = state.record!!

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 15.dp, top = 10.dp, bottom = 10.dp)
                    .background(bgWhite)
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 45.dp)
                    .align(Alignment.TopStart)
                    .verticalScroll(rememberScrollState())
            ) {
                val config = BmiConfigProvider.getConfig(record.age, record.gender)
                val animationProgress = remember { Animatable(0f) }

                BmiGauge(
                    config = config,
                    bmi = record.bmi,
                    animationProgress = animationProgress.value,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 16.dp),
                    showPointer = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.your_bmi_is),
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
                    color = Color(0xFF222222),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .offset(y = 10.dp)
                )

                LaunchedEffect(Unit) {
                    animationProgress.snapTo(0f)
                    animationProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 1000,
                            easing = FastOutSlowInEasing
                        )
                    )
                }

                val animatedBmi = record.bmi.toFloat() * animationProgress.value

                Text(
                    text = String.format("%.1f", animatedBmi),
                    fontSize = 64.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .wrapContentWidth()
                        .align(Alignment.CenterHorizontally)
                        .clickable { showBmiLegendSheet = true },
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

                val weightText = String.format(
                    "%.2f %s",
                    record.weightInput,
                    record.weightUnit.lowercase()
                )
                val heightText = if (record.heightUnit == "FT_IN") {
                    "${record.feetInput ?: 0} ft ${record.inchesInput ?: 0} in"
                } else {
                    String.format("%.1f cm", record.heightCm)
                }
                val genderText =
                    if (record.gender == "MALE") stringResource(R.string.gender_male)
                    else stringResource(R.string.gender_female)
                val ageText = stringResource(R.string.age_years_old, record.age)

                Text(
                    text = "$weightText | $heightText | $genderText | $ageText",
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_regular)),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                if (!state.hasSavedRecord) {
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
                    HealthTip(
                        bmiLevel = state.bmiLevel,
                        record = record
                    )

                    if (state.recommendedApps.isNotEmpty()) {
                        AppRecommendations(apps = state.recommendedApps)
                    }
                }
                Spacer(modifier = Modifier.height(100.dp))
            }

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