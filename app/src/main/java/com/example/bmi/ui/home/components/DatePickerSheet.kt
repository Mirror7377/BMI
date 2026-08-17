package com.example.bmi.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bmi.R
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerSheet(
    currentTimestamp: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val today = Calendar.getInstance()
    val currentYear = today.get(Calendar.YEAR)
    val currentMonth = today.get(Calendar.MONTH)
    val currentDay = today.get(Calendar.DAY_OF_MONTH)

    val calendar = Calendar.getInstance().apply {
        timeInMillis = currentTimestamp.coerceAtMost(System.currentTimeMillis())
    }

    var selectedYear by remember {
        mutableStateOf(calendar.get(Calendar.YEAR))
    }

    var selectedMonth by remember {
        mutableStateOf(calendar.get(Calendar.MONTH))
    }

    var selectedDay by remember {
        mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH) - 1)
    }

    val monthNames = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "June",
        "July", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    val years = (1900..currentYear).map { it.toString() }

    // 计算最大月份（选择当前年份时，月份不能超过当前月份）
    val maxMonth = if (selectedYear == currentYear) currentMonth else 11
    if (selectedMonth > maxMonth) {
        selectedMonth = maxMonth
    }
    val months = monthNames.subList(0, maxMonth + 1)

    // 计算最大日期
    val maxDay = if (selectedYear == currentYear && selectedMonth == currentMonth) {
        currentDay
    } else {
        getDaysInMonth(selectedYear, selectedMonth + 1)
    }
    if (selectedDay >= maxDay) {
        selectedDay = maxDay - 1
    }
    val days = (1..maxDay).map { String.format("%02d", it) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "DATE",
                fontSize = 18.sp,
                fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
                color = Color.Black,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(224.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                WheelPicker(
                    items = months,
                    initialIndex = selectedMonth.coerceIn(0, months.lastIndex),
                    itemHeight = 32.dp,
                    visibleItems = 7,
                    modifier = Modifier.weight(1f),
                    onItemSelected = { newMonth ->
                        selectedMonth = newMonth
                        // 根据新的年月重新计算最大日期
                        val newMaxDay = if (selectedYear == currentYear && selectedMonth == currentMonth) {
                            currentDay
                        } else {
                            getDaysInMonth(selectedYear, selectedMonth + 1)
                        }
                        if (selectedDay >= newMaxDay) {
                            selectedDay = newMaxDay - 1
                        }
                    }
                )

                WheelPicker(
                    items = days,
                    initialIndex = selectedDay.coerceIn(0, days.lastIndex),
                    itemHeight = 32.dp,
                    visibleItems = 7,
                    modifier = Modifier.weight(1f),
                    onItemSelected = { newDay ->
                        selectedDay = newDay
                    }
                )

                WheelPicker(
                    items = years,
                    initialIndex = (selectedYear - 1900).coerceIn(0, years.lastIndex),
                    itemHeight = 32.dp,
                    visibleItems = 7,
                    modifier = Modifier.weight(1f),
                    onItemSelected = { newYearIndex ->
                        selectedYear = newYearIndex + 1900
                        // 如果切换到当前年份且月份超过当前月份，自动修正
                        if (selectedYear == currentYear && selectedMonth > currentMonth) {
                            selectedMonth = currentMonth
                        }
                        // 重新计算最大日期
                        val newMaxDay = if (selectedYear == currentYear && selectedMonth == currentMonth) {
                            currentDay
                        } else {
                            getDaysInMonth(selectedYear, selectedMonth + 1)
                        }
                        if (selectedDay >= newMaxDay) {
                            selectedDay = newMaxDay - 1
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF1F1F1),
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        "CANCEL",
                        fontSize = 18.sp,
                        fontFamily = FontFamily(Font(R.font.montserrat_extrabold))
                    )
                }

                Button(
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            set(selectedYear, selectedMonth, selectedDay + 1)
                        }
                        onDateSelected(cal.timeInMillis)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.bg_rounded_blue),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        "DONE",
                        fontSize = 18.sp,
                        fontFamily = FontFamily(Font(R.font.montserrat_extrabold))
                    )
                }
            }
        }
    }
}

private fun getDaysInMonth(year: Int, month: Int): Int {
    val cal = Calendar.getInstance().apply {
        set(year, month - 1, 1)
    }
    return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
}