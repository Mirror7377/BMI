package com.example.bmi.utils

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ==================== 数据模型 ====================
data class BannerData(
    val iconRes: Int,
    val message: String,
    val durationMillis: Long = 2000
)

// ==================== 状态管理 ====================
class BannerState {
    var visible by mutableStateOf(false)
        private set
    var data by mutableStateOf<BannerData?>(null)
        private set

    fun show(iconRes: Int, message: String, duration: Long = 2000) {
        data = BannerData(iconRes, message, duration)
        visible = true
    }

    fun hide() {
        visible = false
    }
}

@Composable
fun rememberBannerState(): BannerState = remember { BannerState() }

// ==================== UI 组件 ====================
@Composable
fun Banner(
    state: BannerState,
    modifier: Modifier = Modifier
) {
    var lastData by remember { mutableStateOf(state.data) }
    if (state.data != null) lastData = state.data
    val data = state.data ?: lastData ?: return

    AnimatedVisibility(
        visible = state.visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 300)) +
                scaleIn(initialScale = 0.9f, animationSpec = tween(durationMillis = 300)),
        exit = fadeOut(animationSpec = tween(durationMillis = 300)) +
                scaleOut(targetScale = 0.9f, animationSpec = tween(durationMillis = 300)),
        modifier = modifier
    ) {
        LaunchedEffect(data) {
            delay(data.durationMillis)
            state.hide()
        }

        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(top = 60.dp),
            shape = RoundedCornerShape(15.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = data.iconRes),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = data.message,
                    color = Color.Black,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentHeight()
                )
            }
        }
    }
}