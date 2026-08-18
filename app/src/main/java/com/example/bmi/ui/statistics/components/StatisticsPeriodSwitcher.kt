package com.example.bmi.ui.statistics.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bmi.R
import com.example.bmi.data.enums.ChartMode

/**
 * Day / Week / Month 切换器
 * 对应原 XML 中的 periodContainer + selectedPeriodBg
 */
@Composable
fun StatisticsPeriodSwitcher(
    currentMode: ChartMode,
    onModeSelected: (ChartMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val genderBg = colorResource(R.color.gender)
    val boldFont = FontFamily(Font(R.font.montserrat_extrabold))
    val selectedColor = Color.Black
    val unselectedColor = Color(0xFF9B9B9B)

    val options = listOf(
        ChartMode.DAY to "Day",
        ChartMode.WEEK to "Week",
        ChartMode.MONTH to "Month"
    )

    val selectedIndex = options.indexOfFirst { it.first == currentMode }.coerceAtLeast(0)
    val targetOffset = (selectedIndex * 115).dp
    val animatedOffset by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = spring(stiffness = 300f, dampingRatio = 0.7f),
        label = "period_bg_offset"
    )

    Card(
        modifier = modifier
            .width(345.dp)
            .height(36.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = genderBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 白色选中背景块
            Box(
                modifier = Modifier
                    .width(115.dp)
                    .fillMaxHeight()
                    .offset(x = animatedOffset)
                    .background(Color.White, RoundedCornerShape(18.dp))
            )

            Row(modifier = Modifier.fillMaxSize()) {
                options.forEachIndexed { index, (mode, label) ->
                    val isSelected = index == selectedIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                onModeSelected(mode)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 16.sp,
                            fontFamily = boldFont,
                            color = if (isSelected) selectedColor else unselectedColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}