package com.example.bmi.ui.bmigauge

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.example.bmi.R
import kotlin.math.cos
import kotlin.math.sin
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.withTranslation
import androidx.core.graphics.withRotation

@Composable
fun BmiGauge(
    config: BmiGaugeConfig,
    bmi: Double,
    modifier: Modifier = Modifier,
    showPointer: Boolean = true,
    animate: Boolean = true
) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density

    // ===== 加载指针 Drawable 并缓存为 Bitmap =====
    val pointerBitmap = remember {
        val drawable: Drawable? = ResourcesCompat.getDrawable(
            context.resources,
            R.drawable.layer_8,
            null
        )
        drawable?.let {
            // 创建 Bitmap，尺寸与 Drawable 的 intrinsic 尺寸一致
            val bitmap = createBitmap(it.intrinsicWidth, it.intrinsicHeight)
            val canvas = Canvas(bitmap)
            it.setBounds(0, 0, canvas.width, canvas.height)
            it.draw(canvas)
            bitmap
        }
    }

    // 尺寸常量（与原始 View 完全一致）
    val pointerOverflowPx = 11f * density
    val outerRadiusPx = 153f * density
    val innerRadiusPx = 65f * density
    val centerRadiusPx = (outerRadiusPx + innerRadiusPx) / 2f
    val ringWidthPx = outerRadiusPx - innerRadiusPx
    val pointerWidthPx = 90f * density
    val pointerHeightPx = 22f * density
    val pointerAnchorOffsetXPx = 79f * density
    val labelRadiusPx = 158f * density

    // 动画
    val targetBmi = bmi.toFloat().coerceIn(config.min, config.max)
    val animatedBmi = remember { Animatable(if (animate) config.min else targetBmi) }

    LaunchedEffect(targetBmi) {
        if (animate) {
            animatedBmi.animateTo(
                targetValue = targetBmi,
                animationSpec = tween(
                    durationMillis = 800,
                    easing = FastOutSlowInEasing
                )
            )
        } else {
            animatedBmi.snapTo(targetBmi)
        }
    }

    val displayBmi = if (animate) animatedBmi.value else targetBmi

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val cx = canvasWidth / 2f
        val cy = canvasHeight - pointerOverflowPx

        // ---- 1. 绘制分段扇环 ----
        val splitPoints = listOf(config.min) + config.splitPoints + listOf(config.max)
        val arcRect = androidx.compose.ui.geometry.Rect(
            left = cx - centerRadiusPx,
            top = cy - centerRadiusPx,
            right = cx + centerRadiusPx,
            bottom = cy + centerRadiusPx
        )

        for (i in config.colors.indices) {
            val startAngle = bmiToAngle(splitPoints[i], config.min, config.max)
            val endAngle = bmiToAngle(splitPoints[i + 1], config.min, config.max)
            val sweepAngle = endAngle - startAngle

            drawArc(
                color = Color(config.colors[i]),
                topLeft = Offset(arcRect.left, arcRect.top),
                size = arcRect.size,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = ringWidthPx)
            )
        }

        // ---- 2. 绘制刻度标签 ----
        val typeface = ResourcesCompat.getFont(context, R.font.montserrat_extrabold)
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 10f * density
                this.typeface = typeface
                textAlign = android.graphics.Paint.Align.CENTER
            }

            config.labels.forEach { value ->
                val angle = bmiToAngle(value, config.min, config.max)
                val rad = Math.toRadians(angle.toDouble())
                val x = cx + cos(rad).toFloat() * labelRadiusPx
                val y = cy + sin(rad).toFloat() * labelRadiusPx

                val text = if (value % 1.0f == 0.0f) {
                    value.toInt().toString()
                } else {
                    String.format("%.1f", value)
                }

                nativeCanvas.withTranslation(x, y) {
                    rotate(angle + 90f)
                    drawText(text, 0f, 0f, paint)
                }
            }
        }

        // ---- 3. 绘制指针（使用真实图片） ----
        if (showPointer) {
            val targetAngle = bmiToAngle(displayBmi, config.min, config.max)

            drawIntoCanvas { canvas ->
                val nativeCanvas = canvas.nativeCanvas
                nativeCanvas.withRotation(targetAngle - 180f, cx, cy) {
                    val left = cx - pointerAnchorOffsetXPx
                    val top = cy - pointerHeightPx / 2f

                    // 绘制指针图片
                    pointerBitmap?.let { bitmap ->
                        // 缩放图片到目标尺寸
                        val scaledBitmap =
                            bitmap.scale(pointerWidthPx.toInt(), pointerHeightPx.toInt())
                        drawBitmap(scaledBitmap, left, top, null)
                        // 如果每次都缩放，建议缓存缩放后的图片，但这里简单起见直接缩放
                    } ?: run {
                        // 降级方案：如果图片加载失败，用红色矩形占位
                        val paintPtr = android.graphics.Paint().apply {
                            color = android.graphics.Color.RED
                            style = android.graphics.Paint.Style.FILL
                        }
                        val right = left + pointerWidthPx
                        val bottom = top + pointerHeightPx
                        drawRect(left, top, right, bottom, paintPtr)
                    }

                }
            }
        }
    }
}

private fun bmiToAngle(bmi: Float, min: Float, max: Float): Float {
    val ratio = (bmi - min) / (max - min)
    return 180f + ratio * 180f
}