package com.example.bmi.ui.profile.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ThumbChecked = Color(0xFF3659CF)
private val ThumbUnchecked = Color(0xFFFFFFFF)
private val TrackChecked = Color(0xFF9AA4EB)
private val TrackUnchecked = Color(0xFFEAEAEE)
private val ThumbBorderColor = Color(0xFFEAEAEE)

@Composable
fun CustomSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val switchWidth = 40f
    val switchHeight = 14f
    val thumbSize = 20f

    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) switchWidth - thumbSize else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "thumbOffset"
    )

    val trackColor = if (checked) TrackChecked else TrackUnchecked
    val thumbColor = if (checked) ThumbChecked else ThumbUnchecked

    Box(
        modifier = modifier
            .width(switchWidth.dp)
            .height(thumbSize.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // 轨道：垂直居中
        Box(
            modifier = Modifier
                .width(switchWidth.dp)
                .height(switchHeight.dp)  // 轨道保持 14dp
                .align(Alignment.CenterStart)  // ← 垂直居中
                .clip(RoundedCornerShape((switchHeight / 2).dp))
                .background(trackColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onCheckedChange(!checked) }
                )
        )

        // 滑块：垂直居中，可以溢出轨道
        Box(
            modifier = Modifier
                .offset(x = thumbOffset.dp)
                .size(thumbSize.dp)
                .align(Alignment.CenterStart),  // ← 垂直居中
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(thumbColor)
                    .then(
                        if (!checked) {
                            Modifier.border(
                                width = 1.dp,
                                color = ThumbBorderColor,
                                shape = CircleShape
                            )
                        } else {
                            Modifier
                        }
                    )
            )
        }
    }
}