package com.example.bmi.ui.statistics.components

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.spring
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.example.bmi.R
import com.example.bmi.data.enums.ChartMode
import com.example.bmi.ui.statistics.components.ChartConfig
import com.example.bmi.ui.statistics.components.getMonthAbbr
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 月份锚点
 * 对应原 BaseChartView.MonthAnchor
 */
data class MonthAnchor(val month: Int, val dataIndex: Int)

/** 对应原 BaseChartView.ChartPoint */
private data class ChartPoint(val dataIndex: Int, val x: Float, val y: Float)

@Composable
fun <T> StatisticsChart(
    data: List<T>,
    mode: ChartMode,
    config: ChartConfig<T>,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current


    val displayData = remember(data, mode, config) {
        if (data.isEmpty()) {
            data
        } else {
            val lastDate = config.getDate(data.last()).clone() as Calendar
            when (mode) {
                ChartMode.DAY -> lastDate.add(Calendar.DAY_OF_YEAR, 1)
                ChartMode.WEEK -> lastDate.add(Calendar.DAY_OF_YEAR, 7)
                ChartMode.MONTH -> lastDate.add(Calendar.MONTH, 1)
            }
            data + config.createPlaceholder(lastDate)
        }
    }

    val viewWidthPx = with(density) { 345.dp.toPx() }
    val viewHeightPx = with(density) { 237.5f.dp.toPx() }
    val yPaddingTopPx = with(density) { 29.5f.dp.toPx() }
    val yPaddingBottomPx = with(density) { 37f.dp.toPx() }
    val yLabelLeftPx = with(density) { 25f.dp.toPx() }
    val xPaddingRightPx = with(density) { 20.5f.dp.toPx() }
    val monthTopMarginPx = with(density) { 14.5f.dp.toPx() }
    val gridStartYPx = with(density) { 36f.dp.toPx() }
    val gridHeightPx = with(density) { 160f.dp.toPx() }
    val dateLabelYPx = viewHeightPx - with(density) { 17.5f.dp.toPx() }
    val touchSlopPx = with(density) { 10f.dp.toPx() }

    // ========== Y 轴计算（对应原 BaseChartView.updateYAxis） ==========
    val (yMin, yMax, yStep) = remember(displayData) { config.computeAxis(displayData) }
    val yAvailableHeight = viewHeightPx - yPaddingTopPx - yPaddingBottomPx
    val yInterval = yAvailableHeight / 5f

    // ========== 文字画笔（对应原 BaseChartView.textPaint） ==========
    val textPaint = remember(density, context) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.LEFT
            typeface = ResourcesCompat.getFont(context, R.font.montserrat_extrabold)
            textSize = with(density) { 12.sp.toPx() }
        }
    }

    // 计算最大 Y 标签宽度（对应原 BaseChartView.calculateMaxYLabelWidth）
    val maxLabelWidth = remember(yMin, yStep, textPaint) {
        var maxW = 0f
        for (i in 0 until 6) {
            val value = yMin + i * yStep
            val label = String.format("%.1f", value)
            val w = textPaint.measureText(label)
            if (w > maxW) maxW = w
        }
        maxW
    }

    val yLabelRightX = yLabelLeftPx + maxLabelWidth
    val xStart = yLabelRightX + with(density) { 6f.dp.toPx() }
    val xAvailableWidth = viewWidthPx - xStart - xPaddingRightPx
    val xInterval = xAvailableWidth / 7f

    // ========== 滚动状态 ==========
    val scrollOffset = remember { Animatable(0f) }
    val minScrollX = remember(xInterval) { if (xInterval > 0) -xInterval / 2f else 0f }
    val maxScrollX = remember(displayData, xInterval) {
        if (displayData.isEmpty() || xInterval <= 0) 0f
        else max(0f, (displayData.size - 1) * xInterval - 7 * xInterval)
    }

    // 数据变化时重置到最右侧
    LaunchedEffect(displayData, xInterval, minScrollX, maxScrollX) {
        if (displayData.isNotEmpty() && xInterval > 0) {
            val targetStart = (displayData.size - 8).coerceAtLeast(0)
            val targetOffset = targetStart * xInterval
            scrollOffset.snapTo(targetOffset.coerceIn(minScrollX, maxScrollX))
        } else {
            scrollOffset.snapTo(0f)
        }
    }


    var selectedDataIndex by remember { mutableStateOf<Int?>(null) }
    var selectedValue by remember { mutableStateOf<Float?>(null) }


    val monthAnchors = remember(displayData, mode) { buildMonthAnchors(displayData, mode, config) }


    val chartModifier = Modifier
        .size(345.dp, 237.5.dp)
        .pointerInput(displayData, xInterval, minScrollX, maxScrollX, config) {
            val velocityTracker = VelocityTracker()

            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown()
                    velocityTracker.resetTracking()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)

                    var isDragging = false
                    var isHorizontal = false
                    var lastX = down.position.x
                    var lastY = down.position.y
                    var totalDx = 0f
                    var totalDy = 0f

                    var change = down
                    while (change.pressed) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { pointerChange ->
                            if (pointerChange.id != down.id) return@forEach

                            change = pointerChange

                            // 手指抬起时不计算位移，但记录速度
                            if (!pointerChange.pressed) {
                                velocityTracker.addPosition(
                                    pointerChange.uptimeMillis,
                                    pointerChange.position
                                )
                                return@forEach
                            }

                            // dx = last - current（与原始 BaseChartView 一致）
                            val dx = lastX - pointerChange.position.x
                            val dy = lastY - pointerChange.position.y
                            lastX = pointerChange.position.x
                            lastY = pointerChange.position.y
                            totalDx += dx
                            totalDy += dy

                            velocityTracker.addPosition(
                                pointerChange.uptimeMillis,
                                pointerChange.position
                            )

                            if (!isDragging) {
                                val distSq = totalDx * totalDx + totalDy * totalDy
                                if (distSq > touchSlopPx * touchSlopPx) {
                                    isDragging = true
                                    isHorizontal = abs(totalDx) > abs(totalDy)
                                }
                            }

                            if (isDragging && isHorizontal) {
                                pointerChange.consume()
                                val newOffset = (scrollOffset.value + dx)
                                    .coerceIn(minScrollX, maxScrollX)
                                coroutineScope.launch {
                                    scrollOffset.snapTo(newOffset)
                                }
                            }
                        }
                    }

                    // ---- 手指抬起后处理 ----
                    if (isDragging && isHorizontal) {
                        val velocity = velocityTracker.calculateVelocity()
                        if (abs(velocity.x) > 500f) {
                            coroutineScope.launch {
                                val decay = splineBasedDecay<Float>(this@pointerInput)
                                val target = decay.calculateTargetValue(
                                    scrollOffset.value,
                                    -velocity.x
                                )
                                val clamped = target.coerceIn(minScrollX, maxScrollX)
                                if (clamped != target) {
                                    scrollOffset.animateTo(
                                        clamped,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                } else {
                                    scrollOffset.animateDecay(-velocity.x, decay)
                                }
                            }
                        }
                    } else if (!isDragging) {
                        // ---- 点击选中----
                        val dataX = down.position.x + scrollOffset.value
                        val radius = with(density) {
                            (config.selectedDotRadiusDp.dp.toPx() + 6f.dp.toPx())
                        }
                        val startIdx = (scrollOffset.value / xInterval).toInt()
                        val endIdx = min(startIdx + 8 + 1, displayData.size)

                        var hitIndex: Int? = null
                        var hitValue: Float? = null

                        for (i in startIdx until endIdx) {
                            if (i >= displayData.size) break
                            val d = displayData[i]
                            val v = config.getValue(d) ?: continue
                            val px = xStart + i * xInterval
                            val py = config.valueToY(v, yMin, yMax, yAvailableHeight, yPaddingTopPx)
                            val distSq = (dataX - px) * (dataX - px) +
                                    (down.position.y - py) * (down.position.y - py)
                            if (distSq < radius * radius) {
                                hitIndex = i
                                hitValue = v
                                break
                            }
                        }

                        if (hitIndex != null) {
                            selectedDataIndex = hitIndex
                            selectedValue = hitValue
                        } else {
                            selectedDataIndex = null
                            selectedValue = null
                        }
                    }
                }
            }
        }

    // ========== Canvas 绘制 ==========
    Box(modifier = modifier.then(chartModifier)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (displayData.isEmpty()) {
                drawContext.canvas.nativeCanvas.drawText(
                    "No Data",
                    size.width / 2,
                    size.height / 2,
                    textPaint.apply { textAlign = Paint.Align.CENTER }
                )
                return@Canvas
            }

            val visibleStart = (scrollOffset.value / xInterval).toInt()
                .coerceAtLeast(0)
            val visibleEnd = min(visibleStart + 8 + 1, displayData.size)

            // ---- 1. Y 轴标签 ----
            for (i in 0 until 6) {
                val value = yMin + i * yStep
                val y = yPaddingTopPx + (5 - i) * yInterval
                val label = String.format("%.1f", value)
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    yLabelLeftPx,
                    y + textPaint.textSize / 3f,
                    textPaint
                )
            }

            // ---- 2. 设置 Clip 区域 ----
            drawContext.canvas.save()
            drawContext.canvas.clipRect(
                xStart, 0f, size.width, size.height
            )

            // ---- 3. Canvas 平移 ----
            drawContext.canvas.translate(-scrollOffset.value, 0f)

            // ---- 4. 垂直网格线 ----
            val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#EEEEEE")
                style = Paint.Style.STROKE
                strokeWidth = 0.5f
            }
            val gridEndY = gridStartYPx + gridHeightPx
            for (i in visibleStart until visibleEnd) {
                if (i >= displayData.size) break
                val x = xStart + i * xInterval
                drawContext.canvas.nativeCanvas.drawLine(x, gridStartYPx, x, gridEndY, gridPaint)
            }

            // ---- 5. X 轴标签 ----
            val xLabelPaint = Paint(textPaint).apply {
                textAlign = Paint.Align.CENTER
            }
            val visibleLeft = scrollOffset.value + xStart
            val visibleRight = scrollOffset.value + size.width

            for (i in visibleStart until visibleEnd) {
                if (i >= displayData.size) break
                val d = displayData[i]
                val x = xStart + i * xInterval
                val label = config.getXLabel(d, mode)
                val labelWidth = xLabelPaint.measureText(label)
                val left = x - labelWidth / 2
                val right = x + labelWidth / 2
                if (left >= visibleLeft && right <= visibleRight) {
                    drawContext.canvas.nativeCanvas.drawText(label, x, dateLabelYPx, xLabelPaint)
                }
            }

            // ---- 6. 月份/年份标签 ----
            val monthPaint = Paint(textPaint).apply {
                textSize = with(density) { 11.sp.toPx() }
                textAlign = Paint.Align.CENTER
            }
            val monthLabelY = monthTopMarginPx + monthPaint.textSize / 2

            when (mode) {
                ChartMode.DAY -> {
                    var pos = -1
                    for (i in visibleStart until visibleEnd) {
                        if (i >= displayData.size) break
                        if (config.getDayOfMonth(displayData[i]) == 1) {
                            pos = i
                            break
                        }
                    }
                    if (pos != -1 && pos < displayData.size) {
                        val x = xStart + pos * xInterval
                        val text = getMonthAbbr(config.getMonth(displayData[pos]))
                        val textWidth = monthPaint.measureText(text)
                        val left = x - textWidth / 2
                        val right = x + textWidth / 2
                        if (left >= visibleLeft && right <= visibleRight) {
                            drawContext.canvas.nativeCanvas.drawText(text, x, monthLabelY, monthPaint)
                        }
                    }
                }
                ChartMode.WEEK -> {
                    for (anchor in monthAnchors) {
                        val x = xStart + anchor.dataIndex * xInterval
                        val text = getMonthAbbr(anchor.month)
                        val textWidth = monthPaint.measureText(text)
                        val left = x - textWidth / 2
                        val right = x + textWidth / 2
                        if (left >= visibleLeft && right <= visibleRight) {
                            drawContext.canvas.nativeCanvas.drawText(text, x, monthLabelY, monthPaint)
                        }
                    }
                }
                ChartMode.MONTH -> {
                    for (i in visibleStart until visibleEnd) {
                        if (i >= displayData.size) break
                        if (config.getMonth(displayData[i]) == 0) {
                            val x = xStart + i * xInterval
                            val text = config.getYear(displayData[i]).toString()
                            val textWidth = monthPaint.measureText(text)
                            val left = x - textWidth / 2
                            val right = x + textWidth / 2
                            if (left >= visibleLeft && right <= visibleRight) {
                                drawContext.canvas.nativeCanvas.drawText(text, x, monthLabelY, monthPaint)
                            }
                        }
                    }
                }
            }

            // ---- 预计算所有数据点 ----
            val allPoints = mutableListOf<ChartPoint>()
            for (i in displayData.indices) {
                val d = displayData[i]
                val v = config.getValue(d) ?: continue
                val x = xStart + i * xInterval
                val y = config.valueToY(v, yMin, yMax, yAvailableHeight, yPaddingTopPx)
                allPoints.add(ChartPoint(i, x, y))
            }

            // ---- 7. 渐变填充区域 ----
            if (allPoints.size >= 2) {
                val fillPath = Path().apply {
                    moveTo(allPoints[0].x, allPoints[0].y)
                    for (i in 1 until allPoints.size) {
                        val p1 = allPoints[i - 1]
                        val p2 = allPoints[i]
                        val dy = abs(p2.y - p1.y)
                        if (dy <= 1f) {
                            lineTo(p2.x, p2.y)
                        } else {
                            val midX = p1.x + (p2.x - p1.x) / 2f
                            cubicTo(midX, p1.y, midX, p2.y, p2.x, p2.y)
                        }
                    }
                    val lastX = allPoints.last().x
                    val firstX = allPoints.first().x
                    val bottomY = size.height - yPaddingBottomPx
                    lineTo(lastX, bottomY)
                    lineTo(firstX, bottomY)
                    close()
                }
                val gradientBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x66FFFFFF),
                        Color(0x2EFFFFFF),
                        Color(0x00FFFFFF)
                    ),
                    startY = yPaddingTopPx,
                    endY = size.height - yPaddingBottomPx
                )
                drawPath(fillPath, brush = gradientBrush)
            }

            // ---- 8. 折线 ----
            if (allPoints.size >= 2) {
                val linePath = Path().apply {
                    moveTo(allPoints[0].x, allPoints[0].y)
                    for (i in 1 until allPoints.size) {
                        val p1 = allPoints[i - 1]
                        val p2 = allPoints[i]
                        val dy = abs(p2.y - p1.y)
                        if (dy <= 1f) {
                            lineTo(p2.x, p2.y)
                        } else {
                            val midX = p1.x + (p2.x - p1.x) / 2f
                            cubicTo(midX, p1.y, midX, p2.y, p2.x, p2.y)
                        }
                    }
                }
                drawPath(
                    linePath,
                    color = Color.White.copy(alpha = 0.6f),
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )
            }

            // ---- 9. 数据圆点 ----
            for (i in visibleStart until visibleEnd) {
                if (i >= displayData.size) break
                val d = displayData[i]
                val v = config.getValue(d) ?: continue
                val x = xStart + i * xInterval
                val y = config.valueToY(v, yMin, yMax, yAvailableHeight, yPaddingTopPx)
                val isSelected = selectedDataIndex == i

                if (isSelected) {
                    with(config) {
                        drawSelectedDot(Offset(x, y), v)
                    }
                } else {
                    with(config) {
                        drawNormalDot(Offset(x, y))
                    }
                }
            }

            // ---- 10. 选中值弹窗 ----
            if (selectedDataIndex != null && selectedValue != null) {
                val point = allPoints.find { it.dataIndex == selectedDataIndex }
                if (point != null) {
                    val label = config.getSelectedValueLabel(selectedValue!!)

                    val valuePaint = Paint(monthPaint).apply {
                        textSize = with(density) { 12.sp.toPx() }
                    }
                    val fm = valuePaint.fontMetrics
                    val textWidth = valuePaint.measureText(label)
                    val textHeight = fm.descent - fm.ascent

                    val paddingH = with(density) { 8f.dp.toPx() }
                    val paddingTop = with(density) { 6f.dp.toPx() }
                    val paddingBottom = with(density) { 6f.dp.toPx() }
                    val gap = with(density) { 9f.dp.toPx() }
                    val selectedRadiusPx = with(density) { config.selectedDotRadiusDp.dp.toPx() }

                    val bgWidth = textWidth + paddingH * 2
                    val bgHeight = textHeight + paddingTop + paddingBottom
                    val bgLeft = point.x - bgWidth / 2
                    val bgBottom = point.y - selectedRadiusPx - gap
                    val bgTop = bgBottom - bgHeight
                    val bgRight = bgLeft + bgWidth

                    drawRoundRect(
                        color = Color(0xFF2C2C2E),
                        topLeft = Offset(bgLeft, bgTop),
                        size = Size(bgWidth, bgHeight),
                        cornerRadius = CornerRadius(with(density) { 5f.dp.toPx() })
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        point.x,
                        bgTop + paddingTop - fm.ascent,
                        valuePaint.apply { textAlign = Paint.Align.CENTER }
                    )

                    val triHeight = with(density) { 3f.dp.toPx() }
                    val triHalfWidth = with(density) { 5f.dp.toPx() }
                    val triPath = Path().apply {
                        moveTo(point.x - triHalfWidth, bgBottom)
                        lineTo(point.x + triHalfWidth, bgBottom)
                        lineTo(point.x, bgBottom + triHeight)
                        close()
                    }
                    drawPath(triPath, color = Color(0xFF2C2C2E))
                }
            }

            // ---- 恢复画布 ----
            drawContext.canvas.restore()
        }
    }
}

/**
 * 构建月份锚点
 * 对应原 BaseChartView.rebuildMonthAnchors()
 */
private fun <T> buildMonthAnchors(
    data: List<T>,
    mode: ChartMode,
    config: ChartConfig<T>
): List<MonthAnchor> {
    if (mode != ChartMode.WEEK || data.isEmpty()) return emptyList()

    val anchors = mutableListOf<MonthAnchor>()
    val groupedByMonth = data.groupBy { config.getYear(it) to config.getMonth(it) }

    groupedByMonth.forEach { (key, dataList) ->
        val month = key.second
        val firstMondayIndex = dataList.indexOfFirst { d ->
            val cal = config.getDate(d)
            cal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY
        }
        if (firstMondayIndex != -1) {
            val target = dataList[firstMondayIndex]
            val globalIndex = data.indexOfFirst {
                config.getYear(it) == config.getYear(target) &&
                        config.getMonth(it) == config.getMonth(target) &&
                        config.getDayOfMonth(it) == config.getDayOfMonth(target)
            }
            if (globalIndex != -1) {
                anchors.add(MonthAnchor(month, globalIndex))
            }
        }
    }
    return anchors
}