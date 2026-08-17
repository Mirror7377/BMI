package com.example.bmi.ui.home

import android.content.Intent
import android.util.Log
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
import com.example.bmi.ui.profile.ProfileActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToResult: ((BmiRecord) -> Unit)? = null,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val focusManager = LocalFocusManager.current
    val state by viewModel.state.collectAsStateWithLifecycle()

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

    // ---- 监听导航事件 ----
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HomeEffect.NavigateToResult -> {
                    onNavigateToResult?.invoke(effect.record)
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
    Column(
        modifier = modifier
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
                        context.startActivity(Intent(context, ProfileActivity::class.java))
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
                                fontSize = 25.sp,
                                fontFamily = boldFont,
                                color = textBlack,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier
                                .width(91.dp)
                                .height(37.dp)
                                .background(Color.Transparent)
                                .onFocusChanged { focusState ->
                                    if (!focusState.isFocused && weightEdited) {
                                        val trimmed = weightText.trim()
                                        if (trimmed.isNotEmpty()) {
                                            val validValue = trimmed.toDoubleOrNull()

                                            if (validValue != null) {
                                                viewModel.sendIntent(
                                                    HomeIntent.WeightChanged(validValue)
                                                )
                                            } else {
                                                weightText = String.format("%.2f", state.weightInput)
                                            }
                                        } else {
                                            weightText = String.format("%.2f", state.weightInput)
                                        }
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
                                            fontSize = 25.sp,
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
                                                    val trimmed = feetText.trim()
                                                    if (trimmed.isNotEmpty()) {
                                                        val value = trimmed.toIntOrNull()

                                                        if (value != null) {
                                                            viewModel.sendIntent(
                                                                HomeIntent.FeetChanged(value)
                                                            )
                                                        } else {
                                                            feetText = state.feetInput.toString()
                                                        }
                                                    } else {
                                                        feetText = state.feetInput.toString()
                                                    }
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
                                            fontSize = 25.sp,
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
                                                    val trimmed = inchesText.trim()
                                                    if (trimmed.isNotEmpty()) {
                                                        val value = trimmed.toIntOrNull()
                                                        if (value != null) {
                                                            viewModel.sendIntent(
                                                                HomeIntent.InchesChanged(value)
                                                            )
                                                        } else {
                                                            inchesText = state.inchesInput.toString()
                                                        }
                                                    } else {
                                                        inchesText = state.inchesInput.toString()
                                                    }
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
                                    fontSize = 25.sp,
                                    fontFamily = boldFont,
                                    color = textBlack,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier
                                    .width(91.dp)
                                    .height(37.dp)
                                    .background(Color.Transparent)
                                    .onFocusChanged { focusState ->
                                        if (!focusState.isFocused && cmEdited) {
                                            val trimmed = cmText.trim()

                                            if (trimmed.isNotEmpty()) {
                                                val validValue = trimmed.toDoubleOrNull()

                                                if (validValue != null) {
                                                    viewModel.sendIntent(HomeIntent.HeightCmChanged(validValue))
                                                } else {
                                                    cmText = String.format("%.1f", state.heightCm)
                                                }
                                            } else {
                                                cmText = String.format("%.1f", state.heightCm)
                                            }
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
                onAgeSelected = { age ->
                    viewModel.sendIntent(HomeIntent.AgeChanged(age))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
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
                    .offset(y = (-17).dp)
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