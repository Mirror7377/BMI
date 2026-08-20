package com.example.bmi.ui.home

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bmi.R
import com.example.bmi.data.database.BmiRecord
import com.example.bmi.data.enums.Gender
import com.example.bmi.data.enums.HeightUnit
import com.example.bmi.data.enums.WeightUnit
import com.example.bmi.ui.home.components.AgePicker
import com.example.bmi.ui.home.components.DatePickerSheet
import com.example.bmi.ui.home.components.TimePickerSheet
import com.example.bmi.utils.Banner
import com.example.bmi.utils.rememberBannerState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    showDeleteSuccess: Boolean = false,
    onConsumeDeleteSuccess: () -> Unit = {},
    onNavigateToResult: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val focusManager = LocalFocusManager.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val onAgeSelected = remember { { age: Int -> viewModel.sendIntent(HomeIntent.AgeChanged(age)) } }
    val errorMsgHeight = stringResource(R.string.error_height_invalid)
    val errorMsgInches = stringResource(R.string.error_height_inches_invalid)
    val errorMsgFull =stringResource(R.string.error_height_full_invalid)
    val errorMsgFeet = stringResource(R.string.error_height_ft_invalid)
    val errorMsgWeight = stringResource(R.string.error_weight_invalid)

    // ---- 体重输入框状态 ----
    var weightText by remember { mutableStateOf(String.format("%.2f", state.weightInput)) }

    // ---- 身高输入框状态（ft-in 模式） ----
    var feetText by remember { mutableStateOf(state.feetInput.toString()) }
    var inchesText by remember { mutableStateOf(state.inchesInput.toString()) }

    // ---- 身高输入框状态（cm 模式） ----
    var cmText by remember { mutableStateOf(String.format("%.1f", state.heightCm)) }

    var weightEdited by remember { mutableStateOf(false) }
    var feetEdited by remember { mutableStateOf(false) }
    var inchesEdited by remember { mutableStateOf(false) }
    var cmEdited by remember { mutableStateOf(false) }

    // ---- 弹窗状态 ----
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // ---- 监听 ViewModel 状态变化，同步 UI 文本 ----
    LaunchedEffect(state.weightInput) {
        weightText = String.format("%.2f", state.weightInput)
    }
    LaunchedEffect(state.feetInput) {
        feetText = state.feetInput.toString()
    }
    LaunchedEffect(state.inchesInput) {
        inchesText = state.inchesInput.toString()
    }

    LaunchedEffect(state.heightCm) { cmText = String.format("%.1f", state.heightCm) }

    val bannerState = rememberBannerState()

    LaunchedEffect(Unit) {
        viewModel.bannerEvent.collect { data ->
            bannerState.show(data.iconRes, data.message)
        }
    }

    // 监听删除成功标记
    LaunchedEffect(showDeleteSuccess) {
        if (showDeleteSuccess) {
            bannerState.show(
                iconRes = R.drawable.check_circle,
                message = "Deleted successfully."
            )
            onConsumeDeleteSuccess() // 消费掉，防止重组时重复显示
        }
    }

    // ---- 监听导航事件 ----
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HomeEffect.NavigateToResult -> {
                    onNavigateToResult.invoke(effect.recordJson)  // 传 JSON 字符串
                }
            }
        }
    }

    // ---- 资源与样式 ----
    val bgGray = colorResource(R.color.bg_gray)
    val textBlack = colorResource(R.color.text_black)
    val genderBg = colorResource(R.color.gender)
    val white = colorResource(R.color.white)
    val blueButton = colorResource(R.color.bg_rounded_blue)
    val bgText = colorResource(R.color.bg_text)

    val regularFont = FontFamily(Font(R.font.montserrat_regular))
    val boldFont = FontFamily(Font(R.font.montserrat_extrabold))

    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val dateDisplay = dateFormat.format(Date(state.timestamp))

    val noRippleInteractionSource = remember { MutableInteractionSource() }
    val context = LocalContext.current

    // ---- 主布局 ----
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGray)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .pointerInput(Unit) {
                    detectTapGestures { focusManager.clearFocus() }
                }
        ) {
            // ---- 标题栏 ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(top = 30.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.calculator),
                    fontSize = 24.sp,
                    fontFamily = boldFont,
                    color = textBlack,
                    modifier = Modifier.weight(1f)
                )
                Image(
                    painter = painterResource(R.drawable.ic_person),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(
                            indication = null,
                            interactionSource = noRippleInteractionSource
                        ) {
                            onNavigateToProfile()
                        }
                )
            }

            Column {
                // ---- 体重 / 身高 标签 ----
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 15.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = stringResource(R.string.weight),
                        fontSize = 14.sp,
                        fontFamily = regularFont,
                        color = textBlack,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.height),
                        fontSize = 14.sp,
                        fontFamily = regularFont,
                        color = textBlack,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }

                // ---- 体重 + 身高卡片 ----
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(15.dp, Alignment.CenterHorizontally)
                ) {
                    // ========== 体重卡片（可编辑） ==========
                    Card(
                        modifier = Modifier
                            .width(160.dp)
                            .height(68.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = CardDefaults.cardColors(containerColor = white),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicTextField(
                                value = weightText,
                                onValueChange = { newText ->
                                    val lengthLimited = newText.take(6)
                                    if (
                                        lengthLimited.isEmpty() ||
                                        lengthLimited.matches(Regex("^\\d*\\.?\\d*$"))
                                    ) {
                                        weightText = lengthLimited
                                        weightEdited = true
                                    }
                                },
                                textStyle = TextStyle(
                                    fontSize = 27.sp,
                                    fontFamily = boldFont,
                                    color = textBlack,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(37.dp)
                                    .background(Color.Transparent)
                                    .onFocusChanged { focusState ->
                                        if (!focusState.isFocused && weightEdited) {
                                            validateWeight(
                                                weightText = weightText,
                                                weightUnit = state.weightUnit,
                                                currentWeight = state.weightInput,
                                                onShowBanner = { iconRes, message ->
                                                    bannerState.show(iconRes, message)
                                                },
                                                errorMsgWeight = errorMsgWeight,
                                                onUpdateWeight = { value ->
                                                    viewModel.sendIntent(HomeIntent.WeightChanged(value))
                                                },
                                                onResetText = { text -> weightText = text }
                                            )
                                            weightEdited = false
                                        }
                                    },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Done
                                )
                            )
                        }
                    }

                    // ========== 身高卡片（可编辑，根据模式切换） ==========
                    if (state.heightUnit == HeightUnit.FT_IN) {
                        // ---- ft-in 模式：两个输入框并排 ----
                        Row(
                            modifier = Modifier
                                .width(160.dp)
                                .height(68.dp),
                            horizontalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            // 英尺卡片（占 72dp）
                            Card(
                                modifier = Modifier
                                    .width(72.dp)
                                    .height(68.dp),
                                shape = RoundedCornerShape(15.dp),
                                colors = CardDefaults.cardColors(containerColor = white),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BasicTextField(
                                            value = feetText,
                                            onValueChange = { newText ->
                                                val filtered = newText
                                                    .filter { it.isDigit() }
                                                    .take(1)
                                                if (filtered.length <= 1) {
                                                    feetText = filtered
                                                    feetEdited = true
                                                }
                                            },
                                            textStyle = TextStyle(
                                                fontSize = 27.sp,
                                                fontFamily = boldFont,
                                                color = textBlack,
                                                textAlign = TextAlign.Center
                                            ),
                                            modifier = Modifier
                                                .width(35.dp)
                                                .height(37.dp)
                                                .background(Color.Transparent)
                                                .onFocusChanged { focusState ->
                                                    if (!focusState.isFocused && feetEdited) {
                                                        validateFeet(
                                                            feetText = feetText,
                                                            currentFeet = state.feetInput,
                                                            onShowBanner = { iconRes, message ->
                                                                bannerState.show(iconRes, message)
                                                            },
                                                            errorMsgFeet = errorMsgFeet,
                                                            onUpdateFeet = { value ->
                                                                viewModel.sendIntent(HomeIntent.FeetChanged(value))
                                                            },
                                                            onResetText = { text -> feetText = text }
                                                        )
                                                        feetEdited = false
                                                    }
                                                },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number,
                                                imeAction = ImeAction.Done
                                            )
                                        )
                                        Text(
                                            text = stringResource(R.string.yh),
                                            fontSize = 30.sp,
                                            fontFamily = boldFont,
                                            color = textBlack,
                                            modifier = Modifier.padding(start = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(5.dp))

                            // 英寸卡片（占 83dp）
                            Card(
                                modifier = Modifier
                                    .width(83.dp)
                                    .height(68.dp),
                                shape = RoundedCornerShape(15.dp),
                                colors = CardDefaults.cardColors(containerColor = white),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BasicTextField(
                                            value = inchesText,
                                            onValueChange = { newText ->
                                                val filtered = newText
                                                    .filter { it.isDigit() }
                                                    .take(2)
                                                if (filtered.length <= 2) {
                                                    inchesText = filtered
                                                    inchesEdited = true
                                                }
                                            },
                                            textStyle = TextStyle(
                                                fontSize = 27.sp,
                                                fontFamily = boldFont,
                                                color = textBlack,
                                                textAlign = TextAlign.Center
                                            ),
                                            modifier = Modifier
                                                .width(45.dp)
                                                .height(37.dp)
                                                .background(Color.Transparent)
                                                .onFocusChanged { focusState ->
                                                    if (!focusState.isFocused && inchesEdited) {
                                                        validateInches(
                                                            inchesText = inchesText,
                                                            feetText = feetText,
                                                            currentInches = state.inchesInput,
                                                            onShowBanner = { iconRes, message ->
                                                                bannerState.show(iconRes, message)
                                                            },
                                                            errorMsgInches = errorMsgInches ,
                                                            errorMsgFull = errorMsgFull,
                                                            onUpdateInches = { value ->
                                                                viewModel.sendIntent(HomeIntent.InchesChanged(value))
                                                            },
                                                            onResetText = { text -> inchesText = text }
                                                        )
                                                        inchesEdited = false
                                                    }
                                                },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number,
                                                imeAction = ImeAction.Done
                                            )
                                        )
                                        Text(
                                            text = stringResource(R.string.quot),
                                            fontSize = 30.sp,
                                            fontFamily = boldFont,
                                            color = textBlack,
                                            modifier = Modifier.padding(start = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // ---- cm 模式：单个输入框 ----
                        Card(
                            modifier = Modifier
                                .width(160.dp)
                                .height(68.dp),
                            shape = RoundedCornerShape(15.dp),
                            colors = CardDefaults.cardColors(containerColor = white),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicTextField(
                                    value = cmText,
                                    onValueChange = { newText ->
                                        val lengthLimited = newText.take(5)
                                        if (
                                            lengthLimited.isEmpty() ||
                                            lengthLimited.matches(Regex("^\\d*\\.?\\d*$"))
                                        ) {
                                            cmText = lengthLimited
                                            cmEdited = true
                                        }
                                    },
                                    textStyle = TextStyle(
                                        fontSize = 27.sp,
                                        fontFamily = boldFont,
                                        color = textBlack,
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier
                                        .width(100.dp)
                                        .height(37.dp)
                                        .background(Color.Transparent)
                                        .onFocusChanged { focusState ->
                                            if (!focusState.isFocused && cmEdited) {
                                                validateHeightCm(
                                                    cmText = cmText,
                                                    currentHeightCm = state.heightCm,
                                                    errorMsg = errorMsgHeight,
                                                    onShowBanner = { iconRes, message ->
                                                        bannerState.show(iconRes, message)
                                                    },
                                                    onUpdateHeight = { value ->
                                                        viewModel.sendIntent(HomeIntent.HeightCmChanged(value))
                                                    },
                                                    onResetText = { text -> cmText = text }
                                                )
                                                cmEdited = false
                                            }
                                        },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = ImeAction.Done
                                    )
                                )
                            }
                        }
                    }
                }

                // ---- 单位切换 ----
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(15.dp, Alignment.CenterHorizontally)
                ) {
                    UnitSwitch(
                        modifier = Modifier.width(160.dp),
                        options = listOf("lb", "kg"),
                        selectedIndex = if (state.weightUnit == WeightUnit.LB) 0 else 1,
                        onOptionSelected = { index ->
                            val unit = if (index == 0) WeightUnit.LB else WeightUnit.KG
                            viewModel.sendIntent(HomeIntent.WeightUnitChanged(unit))
                        }
                    )
                    UnitSwitch(
                        modifier = Modifier.width(160.dp),
                        options = listOf("ft·in", "cm"),
                        selectedIndex = if (state.heightUnit == HeightUnit.FT_IN) 0 else 1,
                        onOptionSelected = { index ->
                            val unit = if (index == 0) HeightUnit.FT_IN else HeightUnit.CM
                            viewModel.sendIntent(HomeIntent.HeightUnitChanged(unit))
                        }
                    )
                }

                // ---- Time ----
                Text(
                    text = stringResource(R.string.time),
                    fontSize = 14.sp,
                    fontFamily = regularFont,
                    color = textBlack,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
                )

                // ---- 日期 & 时间 ----
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(15.dp, Alignment.CenterHorizontally)
                ) {
                    Card(
                        modifier = Modifier
                            .width(160.dp)
                            .height(60.dp)
                            .clickable(
                                indication = null,
                                interactionSource = noRippleInteractionSource
                            ) {
                                showDatePicker = true
                            },
                        shape = RoundedCornerShape(15.dp),
                        colors = CardDefaults.cardColors(containerColor = white),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dateDisplay,
                                fontSize = 20.sp,
                                fontFamily = boldFont,
                                color = textBlack,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .width(160.dp)
                            .height(60.dp)
                            .clickable(
                                indication = null,
                                interactionSource = noRippleInteractionSource
                            ) {
                                showTimePicker = true
                            },
                        shape = RoundedCornerShape(15.dp),
                        colors = CardDefaults.cardColors(containerColor = white),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(state.timeOfDay.displayName),
                                fontSize = 20.sp,
                                fontFamily = boldFont,
                                color = textBlack,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // ---- Age ----
                Text(
                    text = stringResource(R.string.age),
                    fontSize = 14.sp,
                    fontFamily = regularFont,
                    color = textBlack,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
                )

                // ---- 年龄卡片 ----
                AgePicker(
                    selectedAge = state.age,
                    onAgeSelected = onAgeSelected,
                    modifier = Modifier
                        .width(335.dp)
                        .align(Alignment.CenterHorizontally)
                )

                // ---- 性别 ----
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(15.dp, Alignment.CenterHorizontally)
                ) {
                    GenderCard(
                        modifier = Modifier.width(160.dp),
                        gender = Gender.MALE,
                        isSelected = state.gender == Gender.MALE,
                        onGenderSelected = {
                            viewModel.sendIntent(HomeIntent.GenderSelected(Gender.MALE))
                        }
                    )
                    GenderCard(
                        modifier = Modifier.width(160.dp),
                        gender = Gender.FEMALE,
                        isSelected = state.gender == Gender.FEMALE,
                        onGenderSelected = {
                            viewModel.sendIntent(HomeIntent.GenderSelected(Gender.FEMALE))
                        }
                    )
                }

                // ---- 计算按钮 ----
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(top = 20.dp)
                        .clickable(
                            indication = null,
                            interactionSource = noRippleInteractionSource
                        ) {
                            focusManager.clearFocus()
                            viewModel.sendIntent(HomeIntent.Calculate)
                        },
                    shape = RoundedCornerShape(28.75.dp),
                    colors = CardDefaults.cardColors(containerColor = blueButton),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.calculate),
                            fontSize = 20.sp,
                            fontFamily = boldFont,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        // ---- Banner 覆盖层 ----
        Banner(
            state = bannerState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    // ---- 日期/时间选择器弹窗 ----
    if (showDatePicker) {
        DatePickerSheet(
            currentTimestamp = state.timestamp,
            onDateSelected = { newTimestamp ->
                viewModel.sendIntent(HomeIntent.TimeChanged(newTimestamp, state.timeOfDay))
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showTimePicker) {
        TimePickerSheet(
            currentTimeOfDay = state.timeOfDay,
            onTimeSelected = { newTimeOfDay ->
                viewModel.sendIntent(HomeIntent.TimeChanged(state.timestamp, newTimeOfDay))
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

// ---- 验证函数 ----

private fun validateWeight(
    weightText: String,
    weightUnit: WeightUnit,
    currentWeight: Double,
    errorMsgWeight: String,
    onShowBanner: (Int, String) -> Unit,
    onUpdateWeight: (Double) -> Unit,
    onResetText: (String) -> Unit
) {
    val trimmed = weightText.trim()
    val (min, max) = when (weightUnit) {
        WeightUnit.LB -> 2.0 to 551.0
        WeightUnit.KG -> 1.0 to 250.0
    }
    val errorMsg = String.format(errorMsgWeight, min, max)
    when {
        trimmed.isEmpty() -> {
            val default = when (weightUnit) {
                WeightUnit.LB -> 140.00
                WeightUnit.KG -> 65.00
            }
            onResetText(String.format("%.2f", default))
            onShowBanner(R.drawable.warning, errorMsg)
            onUpdateWeight(default)
        }
        trimmed.toDoubleOrNull() == null -> {
            onResetText(String.format("%.2f", currentWeight))
            onShowBanner(R.drawable.warning, errorMsg)
        }
        else -> {
            val value = trimmed.toDouble()
            val clamped = value.coerceIn(min, max)
            onResetText(String.format("%.2f", clamped))
            if (value != clamped) {
                onShowBanner(R.drawable.warning, errorMsg)
            }
            onUpdateWeight(clamped)
        }
    }
}

private fun validateHeightCm(
    cmText: String,
    currentHeightCm: Double,
    errorMsg: String,
    onShowBanner: (Int, String) -> Unit,
    onUpdateHeight: (Double) -> Unit,
    onResetText: (String) -> Unit
) {
    val trimmed = cmText.trim()
    when {
        trimmed.isEmpty() -> {
            onResetText("170.0")
            onShowBanner(R.drawable.warning, errorMsg)
            onUpdateHeight(170.0)
        }
        trimmed.toDoubleOrNull() == null -> {
            onResetText(String.format("%.1f", currentHeightCm))
            onShowBanner(R.drawable.warning, errorMsg)
        }
        else -> {
            val value = trimmed.toDouble()
            val clamped = value.coerceIn(1.0, 250.0)
            onResetText(String.format("%.1f", clamped))
            if (value != clamped) {
                onShowBanner(R.drawable.warning, errorMsg)
            }
            onUpdateHeight(clamped)
        }
    }
}

private fun validateFeet(
    feetText: String,
    currentFeet: Int,
    onShowBanner: (Int, String) -> Unit,
    onUpdateFeet: (Int) -> Unit,
    errorMsgFeet: String,
    onResetText: (String) -> Unit
) {
    val trimmed = feetText.trim()

    when {
        trimmed.isEmpty() -> {
            onResetText("5")
            onShowBanner(R.drawable.warning, errorMsgFeet)
            onUpdateFeet(5)
        }
        else -> {
            val value = trimmed.toIntOrNull() ?: currentFeet
            val clamped = value.coerceIn(1, 8)
            onResetText(clamped.toString())
            if (value != clamped) {
                onShowBanner(R.drawable.warning, errorMsgFeet)
            }
            onUpdateFeet(clamped)
        }
    }
}

private fun validateInches(
    inchesText: String,
    feetText: String,
    currentInches: Int,
    errorMsgInches: String,
    errorMsgFull: String,
    onShowBanner: (Int, String) -> Unit,
    onUpdateInches: (Int) -> Unit,
    onResetText: (String) -> Unit
) {
    val trimmed = inchesText.trim()
    val feet = feetText.toIntOrNull() ?: 5
    val min = 0
    val max = if (feet >= 8) 2 else 11


    when {
        trimmed.isEmpty() -> {
            onResetText("0")
            onUpdateInches(0)
        }
        else -> {
            val value = trimmed.toIntOrNull() ?: currentInches
            val clamped = value.coerceIn(min, max)
            onResetText(clamped.toString())
            if (value != clamped) {
                val msg = if (feet == 8 && value > 2) errorMsgFull else errorMsgInches
                onShowBanner(R.drawable.warning, msg)
            }
            onUpdateInches(clamped)
        }
    }
}

// ---- 单位切换器 ----
@Composable
fun UnitSwitch(
    modifier: Modifier = Modifier,
    options: List<String>,
    selectedIndex: Int,
    selectedBgWidth: Dp = 90.dp,
    selectedBgOffset: Dp = 0.dp,
    onOptionSelected: (Int) -> Unit
) {
    val genderBg = colorResource(R.color.gender)
    val textBlack = colorResource(R.color.text_black)
    val bgText = colorResource(R.color.bg_text)
    val boldFont = FontFamily(Font(R.font.montserrat_extrabold))
    val noRippleInteractionSource = remember { MutableInteractionSource() }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = genderBg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().height(36.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(selectedBgWidth)
                    .height(36.dp)
                    .offset(
                        x = if (selectedIndex == 0) {
                            selectedBgOffset
                        } else {
                            selectedBgOffset + 70.dp
                        }
                    )
                    .background(Color.White, RoundedCornerShape(18.dp))
            )
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                options.forEachIndexed { index, label ->
                    Text(
                        text = label,
                        fontSize = 16.sp,
                        fontFamily = boldFont,
                        color = if (index == selectedIndex) textBlack else bgText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .offset(x = if (index == 0) 5.dp else (-5).dp)
                            .clickable(
                                indication = null,
                                interactionSource = noRippleInteractionSource
                            ) { onOptionSelected(index) }
                    )
                }
            }
        }
    }
}

// ---- 性别卡片 ----
@Composable
fun GenderCard(
    modifier: Modifier = Modifier,
    gender: Gender,
    isSelected: Boolean,
    onGenderSelected: () -> Unit
) {
    val textBlack = colorResource(R.color.text_black)
    val regularFont = FontFamily(Font(R.font.montserrat_regular))
    val genderBg = colorResource(R.color.gender)
    val white = colorResource(R.color.white)

    val iconRes = if (gender == Gender.MALE) R.drawable.ic_male else R.drawable.ic_female
    val genderText = if (gender == Gender.MALE) stringResource(R.string.male) else stringResource(R.string.female)
    val containerColor = if (isSelected) white else genderBg

    Card(
        modifier = modifier
            .height(90.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onGenderSelected() }
                )
            },
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 20.dp, x = 0.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                text = genderText,
                fontSize = 14.sp,
                fontFamily = regularFont,
                color = textBlack,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-12).dp)
            )
            if (isSelected) {
                Image(
                    painter = painterResource(R.drawable.check_circle),
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-7).dp, y = 6.dp)
                )
            }
        }
    }
}