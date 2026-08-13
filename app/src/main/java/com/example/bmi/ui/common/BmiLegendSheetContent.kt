package com.example.bmi.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bmi.R
import com.example.bmi.ui.bmigauge.BmiConfigProvider
import com.example.bmi.ui.bmigauge.BmiGauge
import com.example.bmi.ui.bmigauge.BmiLevel

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