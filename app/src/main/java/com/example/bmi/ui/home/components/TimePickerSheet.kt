package com.example.bmi.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bmi.R
import com.example.bmi.data.enums.TimeOfDay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerSheet(
    currentTimeOfDay: TimeOfDay,
    onTimeSelected: (TimeOfDay) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val timeOptions = listOf("Morning", "Afternoon", "Evening", "Night")

    val timeOfDayMap = mapOf(
        0 to TimeOfDay.Morning,
        1 to TimeOfDay.Afternoon,
        2 to TimeOfDay.Evening,
        3 to TimeOfDay.Night
    )

    val initialIndex = when (currentTimeOfDay) {
        TimeOfDay.Morning -> 0
        TimeOfDay.Afternoon -> 1
        TimeOfDay.Evening -> 2
        TimeOfDay.Night -> 3
    }

    var selectedIndex by remember { mutableStateOf(initialIndex) }

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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TIME",
                fontSize = 18.sp,
                fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
                color = Color.Black,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // 7 × 30dp = 210dp
            WheelPicker(
                items = timeOptions,
                initialIndex = initialIndex,
                itemHeight = 30.dp,
                visibleItems = 7,
                lineLength = 80.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                onItemSelected = {
                    selectedIndex = it
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        val timeOfDay = timeOfDayMap[selectedIndex] ?: TimeOfDay.Morning
                        onTimeSelected(timeOfDay)
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