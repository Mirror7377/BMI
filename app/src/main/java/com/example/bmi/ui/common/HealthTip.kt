package com.example.bmi.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bmi.R
import com.example.bmi.data.database.BmiRecord
import com.example.bmi.ui.bmigauge.BmiLevel
import com.example.bmi.utils.BmiUiUtils
import com.example.bmi.utils.UnitConverter

// ---- 健康建议组件 ----
@Composable
fun HealthTip(
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
                                color = Color(0xFFFF0000),
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