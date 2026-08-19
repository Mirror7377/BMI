package com.example.bmi.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.bmi.R
import kotlinx.coroutines.joinAll
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val DESIGN_WIDTH = 375f
private const val DESIGN_HEIGHT = 750f

@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onNavigate: (Boolean) -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp.value
    val screenHeightDp = configuration.screenHeightDp.dp.value

    // 缩放比例
    val scale = remember(screenWidthDp, screenHeightDp) {
        minOf(
            screenWidthDp / DESIGN_WIDTH,
            screenHeightDp / DESIGN_HEIGHT
        )
    }

    // 动画状态
    val fadeAlpha = remember { Animatable(0.3f) }
    val translationY = remember { Animatable(150f * scale) }
    val rotation = remember { Animatable(-30f) }

    LaunchedEffect(Unit) {
        // 自定义缓动曲线：标准 ease-in-out（起始慢、中间快、结束慢）
        val splashEasing = CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f)
        val animationDuration = 1000  // 统一时长

        coroutineScope {
            val move = launch {
                translationY.animateTo(
                    0f,
                    tween(
                        durationMillis = animationDuration,
                        easing = splashEasing
                    )
                )
            }

            val alpha = launch {
                fadeAlpha.animateTo(
                    1f,
                    tween(
                        durationMillis = animationDuration,
                        easing = splashEasing
                    )
                )
            }

            val rotate = launch {
                rotation.animateTo(
                    45f,
                    tween(
                        durationMillis = animationDuration,
                        easing = splashEasing
                    )
                )
            }

            joinAll(move, alpha, rotate)
        }

        // 第二段：45° → -45°
        rotation.animateTo(
            -45f,
            tween(
                durationMillis = 800,
                easing = CubicBezierEasing(
                    0.1f,
                    0f,
                    0.25f,
                    0.1f
                )
            )
        )

        viewModel.checkData()
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.isReady) {
        if (state.isReady) onNavigate(state.hasData)
    }

    // ---- 计算各元素尺寸 ----
    val gaugeWidth = (73f * scale).dp
    val gaugeHeight = (53f * scale).dp
    val titleWidth = (170f * scale).dp
    val titleHeight = (70f * scale).dp
    val leapWidth = (150f * scale).dp
    val leapHeight = (40f * scale).dp

    val gap = 10.dp          // 扇环与标题间距

    // ---- 计算底部 logo 的底部边距 ----
    val bottomMargin = (screenHeightDp.dp * 0.03f) // 屏幕高度的 1/20

    // ===== 主布局 =====
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
    ) {
        // ---- 主内容 Column：垂直居中，左侧偏移十分之一 ----
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = screenWidthDp.dp * 0.075f)
                .offset(y = translationY.value.dp)
                .graphicsLayer {
                    alpha = fadeAlpha.value
                    clip = false
                }
        ) {
            // 1. 扇环 + 指针
            Box(
                modifier = Modifier
                    .size(gaugeWidth, gaugeHeight)
            ) {
                // 表盘
                Image(
                    painter = painterResource(R.drawable.gauge),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                // 指针
                Image(
                    painter = painterResource(R.drawable.needle),
                    contentDescription = null,
                    modifier = Modifier
                        .size(
                            width = (8f * scale).dp,
                            height = (23f * scale).dp
                        )
                        .align(Alignment.TopCenter)
                        .offset(y = (15f * scale).dp)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0.5f, 1f)
                            rotationZ = rotation.value
                        },
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(gap))

            // 2. 标题
            Image(
                painter = painterResource(R.drawable.bmi),
                contentDescription = null,
                modifier = Modifier
                    .size(titleWidth, titleHeight),
                contentScale = ContentScale.Fit
            )
        }

        // ---- 底部 Logo：底部居中，距底部 1/20 屏幕高度 ----
        Image(
            painter = painterResource(R.drawable.leap_logo),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomMargin)
                .size(leapWidth, leapHeight),
            contentScale = ContentScale.Fit
        )
    }
}