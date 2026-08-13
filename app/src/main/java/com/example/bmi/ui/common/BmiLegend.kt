package com.example.bmi.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bmi.R
import com.example.bmi.ui.bmigauge.BmiConfigProvider
import com.example.bmi.ui.bmigauge.BmiLevel

// ---- 图例组件 ----
@Composable
fun BmiLegend(
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