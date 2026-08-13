package com.example.bmi.ui.bmigauge

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.withRotation
import androidx.core.graphics.withTranslation
import com.example.bmi.R
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun BmiGauge(
    config: BmiGaugeConfig,
    bmi: Double,
    modifier: Modifier = Modifier,
    showPointer: Boolean = true,
    animate: Boolean = true,
    animationProgress: Float = 1f
) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density

    // 固定尺寸
    val pointerOverflowPx = 11f * density
    val outerRadiusPx = 153f * density
    val innerRadiusPx = 65f * density
    val centerRadiusPx =
        (outerRadiusPx + innerRadiusPx) / 2f
    val ringWidthPx =
        outerRadiusPx - innerRadiusPx

    val pointerWidthPx = 90f * density
    val pointerHeightPx = 22f * density
    val pointerAnchorOffsetXPx = 79f * density
    val labelRadiusPx = 158f * density

    // BMI
    val targetBmi = bmi
        .toFloat()
        .coerceIn(config.min, config.max)

    // 是否动画
    val progress = if (animate) {
        animationProgress.coerceIn(0f, 1f)
    } else {
        1f
    }

    // 当前绘制的 BMI
    val displayBmi = config.min + (targetBmi - config.min) * progress

    // 4. 加载指针 Bitmap
    val pointerBitmap = remember(context) {
        val drawable: Drawable? = ResourcesCompat.getDrawable(
            context.resources,
            R.drawable.layer_8,
            null
        )
        drawable?.let {
            val width = it.intrinsicWidth
            val height = it.intrinsicHeight
            if (width > 0 && height > 0) {
                val bitmap = createBitmap(width, height)
                val canvas = Canvas(bitmap)
                it.setBounds(0, 0, canvas.width, canvas.height)
                it.draw(canvas)
                bitmap
            } else {
                null
            }
        }
    }

    // 5. 预缩放指针
    val scaledPointerBitmap = remember(pointerBitmap, pointerWidthPx, pointerHeightPx) {
        pointerBitmap?.let { bitmap ->
            val targetWidth = pointerWidthPx.roundToInt()
            val targetHeight = pointerHeightPx.roundToInt()
            if (bitmap.width == targetWidth && bitmap.height == targetHeight) {
                bitmap
            } else {
                bitmap.scale(targetWidth, targetHeight)
            }
        }
    }

    // 6. 缓存分段信息
    val splitPoints = remember(config.min, config.max, config.splitPoints) {
        listOf(config.min) + config.splitPoints + listOf(config.max)
    }

    val segmentColors = remember(config.colors) {
        config.colors.map { Color(it) }
    }

    // 7. 缓存标签角度
    val labelAngles = remember(config.labels, config.min, config.max) {
        config.labels.map { value ->
            value to bmiToAngle(value, config.min, config.max)
        }
    }

    // 8. 创建 Typeface
    val typeface = remember(context) {
        ResourcesCompat.getFont(context, R.font.montserrat_extrabold)
    }

    // 9. 创建 Paint
    val labelPaint = remember(density, typeface) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = 10f * density
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
        }
    }

    // 10. Canvas
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val cx = canvasWidth / 2f
        val cy = canvasHeight - pointerOverflowPx

        // 扇环区域
        val arcRect = androidx.compose.ui.geometry.Rect(
            left = cx - centerRadiusPx,
            top = cy - centerRadiusPx,
            right = cx + centerRadiusPx,
            bottom = cy + centerRadiusPx
        )

        // 绘制分段扇环
        for (i in segmentColors.indices) {
            val startAngle = bmiToAngle(splitPoints[i], config.min, config.max)
            val endAngle = bmiToAngle(splitPoints[i + 1], config.min, config.max)
            val sweepAngle = endAngle - startAngle
            drawArc(
                color = segmentColors[i],
                topLeft = Offset(arcRect.left, arcRect.top),
                size = arcRect.size,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = ringWidthPx)
            )
        }

        // 绘制刻度标签
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            labelAngles.forEach { (value, angle) ->
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
                    drawText(text, 0f, 0f, labelPaint)
                }
            }
        }

        // 绘制指针
        if (showPointer) {
            val targetAngle = bmiToAngle(displayBmi, config.min, config.max)
            drawIntoCanvas { canvas ->
                val nativeCanvas = canvas.nativeCanvas
                nativeCanvas.withRotation(targetAngle - 180f, cx, cy) {
                    val left = cx - pointerAnchorOffsetXPx
                    val top = cy - pointerHeightPx / 2f
                    scaledPointerBitmap?.let { bitmap ->
                        drawBitmap(bitmap, left, top, null)
                    } ?: run {
                        val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.RED
                            style = Paint.Style.FILL
                        }
                        val right = left + pointerWidthPx
                        val bottom = top + pointerHeightPx
                        drawRect(left, top, right, bottom, fallbackPaint)
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