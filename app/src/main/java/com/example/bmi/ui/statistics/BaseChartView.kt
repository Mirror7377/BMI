package com.example.bmi.ui.statistics

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.widget.OverScroller
import com.example.bmi.R
import com.example.bmi.data.enums.ChartMode
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 图表基类，提取 BmiChartView 和 WeightChartView 的公共逻辑
 * @param T 数据类型（DayBmiData 或 DayWeightData）
 */
abstract class BaseChartView<T>(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ===== 枚举 =====

    // ===== 常量 =====
    // ===== 图表布局与绘制常量 =====
    protected companion object {
        // ---------- Y 轴边距 ----------
        /** Y 轴顶部留白（dp），让折线不紧贴图表顶边 */
        const val Y_PADDING_TOP = 29.5f
        /** Y 轴底部留白（dp），为 X 轴标签（日期数字）留出空间 */
        const val Y_PADDING_BOTTOM = 37f
        /** Y 轴标签的数量（含首尾），共显示 6 个刻度值 */
        const val Y_LABEL_COUNT = 6
        /** Y 轴标签距离屏幕左边缘的固定边距（dp），让数字不贴边 */
        const val Y_LABEL_LEFT_MARGIN = 25f

        // ---------- X 轴边距与密度 ----------
        /** X 轴右侧留白（dp），防止最右侧的数据点紧贴屏幕边缘 */
        const val X_PADDING_RIGHT = 20.5f
        /** 屏幕上同时显示的数据点数量，即 X 轴可见刻度数 */
        const val X_LABEL_COUNT = 8

        // ---------- 月份/年份标签 ----------
        /** 月份标签（如 Jan、Feb）距离图表顶部的边距（dp） */
        const val MONTH_TOP_MARGIN = 14.5f

        // ---------- 数据点样式 ----------
        /** 普通数据点的圆点半径（dp），未被选中时的默认大小 */
        const val DOT_RADIUS_NORMAL = 3f
    }

    // ===== 月份锚点 =====
    protected val monthAnchors = mutableListOf<MonthAnchor>()

    protected data class MonthAnchor(
        val month: Int,
        val dataIndex: Int,
        var x: Float = 0f
    )

    // ===== 画笔（所有子类共用） =====
    protected val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.LEFT//左对齐
        typeface = resources.getFont(R.font.montserrat_extrabold)
        textSize = spToPx(12f)
    }

    protected val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = resources.getFont(R.font.montserrat_extrabold)
        textSize = spToPx(11f)
    }

    protected val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = resources.getFont(R.font.montserrat_extrabold)
        textSize = spToPx(12f)
    }

    protected val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    protected val dotFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    protected val verticalGridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EEEEEE")
        style = Paint.Style.STROKE
        strokeWidth = 0.5f
    }

    protected val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = 0x99
        strokeWidth = 4.0f
        style = Paint.Style.STROKE
    }

    protected val valueBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2C2C2E")
        style = Paint.Style.FILL
    }

    // ===== 数据与状态 =====
    protected var allData: List<T> = emptyList()
    protected val displayCount = X_LABEL_COUNT
    protected var scrollOffset = 0f
    protected var minScrollX = 0f
    protected var maxScrollX = 0f
    protected val visibleStartIndex: Float get() = scrollOffset / xInterval

    protected var selectedDataIndex: Int? = null
    protected var selectedValue: Float? = null

    // ===== 视图尺寸 =====
    protected var viewWidth = 0f           // 整个 View 的总宽度（像素）
    protected var viewHeight = 0f          // 整个 View 的总高度（像素）

    // ===== Y 轴边距（控制图表上下左右留白） =====
    protected var yPaddingTopPx = 0f       // Y 轴顶部留白（让折线不紧贴顶边）
    protected var yPaddingBottomPx = 0f    // Y 轴底部留白（给 X 轴标签留出空间）
    protected var yLabelLeftPx = 0f        // Y 轴标签距离屏幕左边缘的距离（固定边距）
    protected var xPaddingRightPx = 0f     // X 轴右侧留白（防止最右侧的数据点贴边）

    // ===== 绘图区域尺寸 =====
    protected var yAvailableHeight = 0f    // Y 轴可绘制高度 = viewHeight - yPaddingTop - yPaddingBottom
    protected var xAvailableWidth = 0f     // X 轴可绘制宽度 = viewWidth - xStart - xPaddingRight

    // ===== 刻度和步长 =====
    protected var yInterval = 0f           // Y 轴每个刻度的像素间隔（6 个刻度，共 5 段）
    protected var xInterval = 0f           // X 轴每个数据点之间的像素间隔（8 个点，共 7 段）

    // ===== 绘图起点 =====
    protected var xStart = 0f              // 图表绘图区域的 X 轴起始位置（Y 轴标签右边缘 + 6dp）
    protected var monthLabelY = 0f         // 月份/年份标签在 Y 轴上的固定位置（图表顶部）

    // ===== 滚动与触摸 =====
    protected val scroller = OverScroller(context)  // 惯性滚动引擎（处理 Fling 动画）
    protected var velocityTracker: VelocityTracker? = null  // 速度追踪器（用于计算手指抬起时的滑动速度）
    protected var isDragging = false        // 是否正在拖拽（用于区分“点击”和“滑动”）
    protected var lastTouchX = 0f           // 上一次触摸事件记录的 X 坐标（用于计算滑动距离）
    protected val touchSlop = dpToPx(10f)   // 触发拖拽的最小滑动距离（防止轻微抖动误触）


    // ===== 模式支持 =====
    protected var chartMode = ChartMode.DAY
    fun setMode(mode: ChartMode) {
        if (chartMode != mode) {
            chartMode = mode
            invalidate()
        }
    }

    // ===== 子类必须实现的抽象方法 =====
    /** 获取数据中的数值（BMI 或 Weight） */
    protected abstract fun getValue(data: T): Float?

    /** 获取数据中的日期 */
    protected abstract fun getDate(data: T): Calendar

    /** 获取数据中的月份 */
    protected abstract fun getMonth(data: T): Int

    /** 获取数据中的年份 */
    protected abstract fun getYear(data: T): Int

    /** 获取数据中的日 */
    protected abstract fun getDayOfMonth(data: T): Int

    /** 获取数据中的时间戳（用于比较） */
    protected abstract fun getTimeInMillis(data: T): Long

    /** 获取 Y 轴最小值 */
    protected abstract fun getYMin(): Float

    /** 获取 Y 轴最大值 */
    protected abstract fun getYMax(): Float

    /** 获取 Y 轴步长 */
    protected abstract fun getYStep(): Float

    /** 更新 Y 轴数据 */
    protected abstract fun updateYAxis(data: List<T>)

    /** 将数值转换为 Y 坐标 */
    protected abstract fun valueToY(value: Float): Float

    /** 获取圆点颜色 */
    protected abstract fun getDotColor(value: Float): Int

    /** 获取选中的圆点半径 */
    protected abstract fun getSelectedDotRadius(): Float

    /** 获取选中值的显示标签 */
    protected abstract fun getSelectedValueLabel(value: Float): String

    /** 获取 X 轴标签（日/月数字） */
    protected abstract fun getXLabel(data: T): String



    init {
        isClickable = true
        isFocusable = true
        setWillNotDraw(false)
    }

    // ===== 尺寸与布局 =====
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        //获取画布尺寸
        viewWidth = w.toFloat()
        viewHeight = h.toFloat()

        yPaddingTopPx = dpToPx(Y_PADDING_TOP)
        yPaddingBottomPx = dpToPx(Y_PADDING_BOTTOM)
        yLabelLeftPx = dpToPx(Y_LABEL_LEFT_MARGIN)
        xPaddingRightPx = dpToPx(X_PADDING_RIGHT)

        yAvailableHeight = viewHeight - yPaddingTopPx - yPaddingBottomPx
        yInterval = yAvailableHeight / (Y_LABEL_COUNT - 1)

        monthLabelY = dpToPx(MONTH_TOP_MARGIN) + monthPaint.textSize / 2

        if (allData.isNotEmpty()) {
            updateYAxis(allData)
        }

        updateLayoutMetrics()
        updateScrollBounds()
        clampScrollOffset()
        invalidate()
    }

    // ===== 数据设置 =====
    fun setData(data: List<T>) {
        when (chartMode) {
            ChartMode.DAY -> setDataDay(data)
            ChartMode.WEEK -> setDataWeek(data)
            ChartMode.MONTH -> setDataMonth(data)
        }
    }

    // ---------- 模板方法：DAY 模式 ----------
    protected open fun setDataDay(data: List<T>) {
    }

    // ---------- 模板方法：WEEK 模式 ----------
    protected open fun setDataWeek(data: List<T>) {
        //进行升序排序
        allData = data.sortedBy { getTimeInMillis(it) }
        selectedDataIndex = null
        selectedValue = null
        updateYAxis(allData)
        updateLayoutMetrics()
        //算出当前屏幕下第一个数据的下标
        val targetStart = (allData.size - displayCount).coerceAtLeast(0)
        scrollOffset = targetStart.toFloat() * xInterval
        updateScrollBounds()
        clampScrollOffset()
        invalidate()
        rebuildMonthAnchors()
        updateMonthAnchorPositions()
    }

    // ---------- 模板方法：MONTH 模式 ----------
    protected open fun setDataMonth(data: List<T>) {
        allData = data.sortedBy { getTimeInMillis(it) }
        selectedDataIndex = null
        selectedValue = null
        updateYAxis(allData)
        updateLayoutMetrics()
        //算出当前屏幕下第一个数据的下标
        val targetStart = (allData.size - displayCount).coerceAtLeast(0)
        scrollOffset = targetStart * xInterval + xInterval / 2f
        updateScrollBounds()
        clampScrollOffset()
        invalidate()
        monthAnchors.clear()
    }

    // ===== Y 轴计算工具 =====
    protected fun computeAxis(
        minVal: Float,
        maxVal: Float
    ): Triple<Float, Float, Float> {
        //所有数据都相同
        if (minVal == maxVal) {
            val value = minVal
            //取绝对值，用来判断数值的大小尺度。
            val absVal = abs(value)
            //absVal < 4（比如 BMI 在 0~4 之间），步长取 max(absVal/4, 0.1)。
            // 这样步长至少是 0.1，且会随着数值增大而略微增大（比如值=2，步长=0.5）。
            val step = if (absVal < 4f) {
                max(absVal / 4f, 0.1f)
            } else {
                //步长直接取 1。
                1f
            }
            //（向下减一个步长）
            val axisMin = value - step
            //均匀增加 5 个步长，得到 6 个刻度点
            val axisMax = axisMin + step * (Y_LABEL_COUNT - 1)
            return Triple(axisMin, axisMax, step)
        }

        val dataSpan = maxVal - minVal
        val totalSpan = dataSpan / 0.75f
        val bottomBlank = totalSpan * 0.05f
        val topBlank = totalSpan * 0.20f

        var axisMin = minVal - bottomBlank
        var axisMax = maxVal + topBlank

        var step = (axisMax - axisMin) / (Y_LABEL_COUNT - 1)
        step = (step * 10).toInt().toFloat() / 10f
        if (step < 0.1f) step = 0.1f

        //将轴最小值向下对齐到步长的整数倍
        axisMin = kotlin.math.floor(axisMin / step) * step
        //从新的 axisMin 出发，重新计算 axisMax
        axisMax = axisMin + step * (Y_LABEL_COUNT - 1)

        return Triple(axisMin, axisMax, step)
    }

    // ===== 控制图表可以“左右滚动多远” =====
    protected fun updateScrollBounds() {
        if (allData.isEmpty()) {
            minScrollX = 0f
            maxScrollX = 0f
            return
        }
        //整个数据序列从第 1 个点到最后一个点铺开的总长度
        val totalWidth = (allData.size - 1) * xInterval
        //屏幕一次性可以显示的长度
        val visibleWidth = (displayCount - 1) * xInterval
        //允许向左多滑出半个间隔
        minScrollX = -xInterval / 2f
        //                          数据总宽度减去屏幕可见宽度。
        maxScrollX = max(0f, totalWidth - visibleWidth)
    }

    //强制把滚动偏移量拉回合法轨道
    protected fun clampScrollOffset() {
        scrollOffset = scrollOffset.coerceIn(minScrollX, maxScrollX)
    }


    // ===== 布局度量 =====
    protected fun updateLayoutMetrics() {
        val maxLabelWidth = calculateMaxYLabelWidth()
        // Y 轴标签的右边界
        val yLabelRightX = yLabelLeftPx + maxLabelWidth
        //在 Y 轴标签的右边界基础上，再向右偏移 6dp不拥挤
        xStart = yLabelRightX + dpToPx(6f)
        //纯粹用来画折线、网格、点的水平区域的总宽度。
        xAvailableWidth = viewWidth - xStart - xPaddingRightPx
        //计算每个数据点之间的水平间距：
        xInterval = if (displayCount > 1) {
            xAvailableWidth / (displayCount - 1)
        } else {
            0f
        }
        //控制图表可以“左右滚动多远”
        updateScrollBounds()
        //重新计算每个锚点应处的精确像素位置
        updateMonthAnchorPositions()
    }

    //找出Y轴上的最大宽度
    protected open fun calculateMaxYLabelWidth(): Float {
        var maxWidth = 0f
        for (i in 0 until Y_LABEL_COUNT) {
            //遍历得到y轴上的所有值
            val value = getYMin() + i * getYStep()
            val label = String.format("%.1f", value)
            val w = textPaint.measureText(label)
            if (w > maxWidth) maxWidth = w
        }
        return maxWidth
    }

    // ===== 绘制主流程 =====
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (allData.isEmpty()) {
            drawEmptyState(canvas)
            return
        }
        drawYLabels(canvas)

        //定义一个矩形区域
        val clipLeft = xStart
        val clipTop = 0f
        val clipRight = viewWidth
        val clipBottom = viewHeight
        canvas.save()//保存
        //挡板，只有这个区域的数据才会显示
        canvas.clipRect(clipLeft, clipTop, clipRight, clipBottom)

        //获取当前可见的最左侧索引的下标
        val startIdx = visibleStartIndex.toInt()
        //多取一个点作为缓冲
        val endIdx = min(startIdx + displayCount + 1, allData.size)
        //返回当前索引下的视图
        val visibleSubList = allData.subList(startIdx, endIdx)
        //将x，y移动制定距离    scrollOffset触摸交互
        canvas.translate(-scrollOffset, 0f)

        //画竖线
        drawVerticalGridLines(canvas, startIdx, visibleSubList)
        //绘制x轴标签
        drawXLabels(canvas, startIdx, visibleSubList)
        //绘制顶部标签
        drawMonthLabel(canvas, startIdx, endIdx, visibleSubList)

        //填充渐变
        drawFillArea(canvas)
        //画曲线
        drawDataLine(canvas)

        //画数据点的圆点
        drawDots(canvas, startIdx, visibleSubList)
        drawSelectedValue(canvas, startIdx, visibleSubList)

        canvas.restore()
    }

    // ===== 绘制方法 =====
    protected fun drawEmptyState(canvas: Canvas) {
        textPaint.textSize = spToPx(14f)
        textPaint.color = Color.WHITE
        textPaint.alpha = 0x80
        canvas.drawText("No Data", viewWidth / 2, viewHeight / 2, textPaint)
        textPaint.alpha = 0xFF
        textPaint.textSize = spToPx(12f)
    }

    protected fun drawYLabels(canvas: Canvas) {
        for (i in 0 until Y_LABEL_COUNT) {
            val value = getYMin() + i * getYStep()
            //          图表顶部的留白
            val y = yPaddingTopPx + (Y_LABEL_COUNT - 1 - i) * yInterval
            val label = String.format("%.1f", value)
            canvas.drawText(label, yLabelLeftPx, y + textPaint.textSize / 3, textPaint)
        }
    }

    protected fun drawFillArea(canvas: Canvas) {
        val points = getFullDataPoints()
        //小于2不绘制
        if (points.size < 2) return

        //构建曲线路径
        val fillPath = Path()
        //将画笔的起始点移动到第一个数据点的坐标
        fillPath.moveTo(points[0].x, points[0].y)

        for (i in 1 until points.size) {
            //取出前一个数据点
            val p1 = points[i - 1]
            //取出当前数据点
            val p2 = points[i]
            //计算前后两个点的 Y 坐标差值的绝对值
            val dy = abs(p2.y - p1.y)

            //几乎在同一水平线上
            if (dy <= 1f) {
                //直线连接
                fillPath.lineTo(p2.x, p2.y)
            } else {
                //midX 是 p1.x 和 p2.x 的中点（水平方向的中点）
                val midX = p1.x + (p2.x - p1.x) / 2f
                //使用三次贝塞尔曲线（cubicTo） 来连接
                fillPath.cubicTo(midX, p1.y, midX, p2.y, p2.x, p2.y)
            }
        }

        val lastX = points.last().x
        val firstX = points.first().x
        val bottomY = viewHeight - yPaddingBottomPx

        fillPath.lineTo(lastX, bottomY)   // 从最后一个数据点垂直向下到底部
        fillPath.lineTo(firstX, bottomY)  // 横向移动到底部最左端
        fillPath.close()                  // 从底部左端画回第一个数据点，闭合路径

        val topColor = Color.argb(102, 255, 255, 255)
        val midColor = Color.argb(46, 255, 255, 255)
        val bottomColor = Color.argb(0, 255, 255, 255)

        val shader = LinearGradient(
            0f, yPaddingTopPx,                               // 起点坐标 (左上角)
            0f, viewHeight - yPaddingBottomPx,               // 终点坐标 (左下角)
            intArrayOf(topColor, midColor, bottomColor),     // 颜色数组（从上到下）
            floatArrayOf(0f, 0.5f, 1f),                      // 位置数组（0% → 50% → 100%）
            Shader.TileMode.CLAMP                            // 边界延伸模式
        )
        fillPaint.shader = shader
        //应用着色器并绘制路径
        canvas.drawPath(fillPath, fillPaint)
    }

    //为当前屏幕可见的每个数据点，绘制对应的日期/月份数字标签
    protected open fun drawXLabels(canvas: Canvas, startIdx: Int, visibleData: List<T>) {
        //底部留白17.5
        val dateY = viewHeight - dpToPx(17.5f)
        textPaint.textAlign = Paint.Align.CENTER
        val visibleLeft = scrollOffset + xStart
        val visibleRight = scrollOffset + viewWidth
        for (i in visibleData.indices) {
            val dataIndex = startIdx + i
            if (dataIndex >= allData.size) break
            //取出当前索引对应的数据对象
            val data = allData[dataIndex]
            //计算数据点的屏幕 X 坐标
            val x = xStart + dataIndex * xInterval
            //获取标签
            val label = getXLabel(data)
            //获取像素宽度
            val labelWidth = textPaint.measureText(label)
            //文字是以 x 为中心绘制的所以/2
            val left = x - labelWidth / 2
            val right = x + labelWidth / 2
            //只有当文字的左边界和右边界都完全落在屏幕可视区域内时，才绘制该标签。
            if (left >= visibleLeft && right <= visibleRight) {
                canvas.drawText(label, x, dateY, textPaint)
            }
        }
        //将 textPaint 恢复为左对齐，避免影响后续绘制。
        textPaint.textAlign = Paint.Align.LEFT
    }

    //在图表顶部绘制月份缩写（如 Jan、Feb）或年份（如 2026），并且只绘制那些文字完全显示在屏幕内的标签。
    protected open fun drawMonthLabel(canvas: Canvas, startIdx: Int, endIdx: Int, visibleData: List<T>) {
        when (chartMode) {
            ChartMode.DAY -> {
                var pos = -1
                //在当前的可见数据子列表中
                for (i in visibleData.indices) {
                    val dataIndex = startIdx + i
                    //当当前索引的数据值为1时，
                    if (getDayOfMonth(allData[dataIndex]) == 1 && dataIndex < allData.size) {
                        pos = dataIndex
                        break
                    }
                }
                if (pos != -1) {
                    //找到x横坐标
                    val x = xStart + pos * xInterval
                    val text = getMonthAbbr(getMonth(allData[pos]))
                    //绘制的文本
                    val textWidth = monthPaint.measureText(text)
                    val left = x - textWidth / 2
                    val right = x + textWidth / 2
                    val visibleLeft = scrollOffset + xStart
                    val visibleRight = scrollOffset + viewWidth
                    //只有当文字的左边界和右边界都完全落在屏幕可视区域内时，才绘制该标签。
                    if (left >= visibleLeft && right <= visibleRight) {
                        canvas.drawText(text, x, monthLabelY, monthPaint)
                    }
                }
            }

            ChartMode.WEEK -> {
                //遍历月份锚点rebuildMonthAnchors方法构建
                for (anchor in monthAnchors) {
                    //从当前锚点中取出已经计算好的 X 坐标
                    val x = anchor.x
                    //获取标签
                    val text = getMonthAbbr(anchor.month)
                    val textWidth = monthPaint.measureText(text)
                    val left = x - textWidth / 2
                    val right = x + textWidth / 2
                    val visibleLeft = scrollOffset + xStart
                    val visibleRight = scrollOffset + viewWidth
                    if (left >= visibleLeft && right <= visibleRight) {
                        canvas.drawText(text, x, monthLabelY, monthPaint)
                    }
                }
            }

            ChartMode.MONTH -> {
                //遍历当前屏幕可见的数据子列表（visibleData）中的所有元素。
                for (i in visibleData.indices) {
                    val dataIndex = startIdx + i
                    if (dataIndex >= allData.size) break
                    val data = allData[dataIndex]
                    //检查当前数据点是否属于 1 月（Calendar 中 1 月对应 0）
                    if (getMonth(data) == 0) {
                        val x = xStart + dataIndex * xInterval
                        val text = getYear(data).toString()
                        val textWidth = monthPaint.measureText(text)
                        val left = x - textWidth / 2
                        val right = x + textWidth / 2
                        val visibleLeft = scrollOffset + xStart
                        val visibleRight = scrollOffset + viewWidth
                        if (left >= visibleLeft && right <= visibleRight) {
                            canvas.drawText(text, x, monthLabelY, monthPaint)
                        }
                    }
                }
            }
        }
    }

    protected fun drawVerticalGridLines(canvas: Canvas, startIdx: Int, visibleData: List<T>) {
        //起始y坐标
        val startY = dpToPx(36f)
        val endY = startY + dpToPx(160f)
        for (i in visibleData.indices) {
            val dataIndex = startIdx + i
            if (dataIndex >= allData.size) break
            val x = xStart + dataIndex * xInterval
            //画一条从 (startX, startY) 到 (stopX, stopY) 的直线。
            canvas.drawLine(x, startY, x, endY, verticalGridPaint)
        }
    }

    protected fun drawDataLine(canvas: Canvas) {
        val points = getFullDataPoints()
        if (points.size < 2) return

        val path = Path()
        path.moveTo(points[0].x, points[0].y)

        for (i in 1 until points.size) {
            val p1 = points[i - 1]
            val p2 = points[i]
            val dy = abs(p2.y - p1.y)

            if (dy <= 1f) {
                path.lineTo(p2.x, p2.y)
            } else {
                val midX = p1.x + (p2.x - p1.x) / 2f
                path.cubicTo(midX, p1.y, midX, p2.y, p2.x, p2.y)
            }
        }

        canvas.drawPath(path, linePaint)
    }

    protected open fun drawDots(canvas: Canvas, startIdx: Int, visibleData: List<T>) {
        //获取数据点
        val points = getDataPoints(startIdx, visibleData)
        //数据点半径
        val normalRadius = dpToPx(DOT_RADIUS_NORMAL)

        //遍历数据点
        for (point in points) {
            //获取bmi/weight
            val value = getValue(allData[point.dataIndex])
            if (value == null) continue
            //获取对应数据点的颜色
            val color = getDotColor(value)
            //判断当前点是否被选中
            val isSelected = selectedDataIndex == point.dataIndex

            //被选中
            if (isSelected) {

                val selectedRadius = getSelectedDotRadius()
                dotFillPaint.color = Color.WHITE
                //画一个半径为 selectedRadius 的实心圆
                canvas.drawCircle(point.x, point.y, selectedRadius, dotFillPaint)
                dotFillPaint.color = color
                //画一个半径为 0.8selectedRadius 的实心圆
                canvas.drawCircle(point.x, point.y, selectedRadius * 0.8f, dotFillPaint)
            } else {
                dotFillPaint.color = Color.WHITE
                canvas.drawCircle(point.x, point.y, normalRadius, dotFillPaint)
            }
        }
    }

    protected open fun drawSelectedValue(canvas: Canvas, startIdx: Int, visibleData: List<T>) {
        if (selectedDataIndex == null || selectedValue == null) return

        val points = getDataPoints(startIdx, visibleData)
        val point = points.find { it.dataIndex == selectedDataIndex } ?: return

        val label = getSelectedValueLabel(selectedValue!!)

        val paddingHorizontal = dpToPx(8f)
        val paddingTop = dpToPx(6f)
        val paddingBottom = dpToPx(6f)

        val textWidth = valuePaint.measureText(label)
        val textHeight = valuePaint.fontMetrics.descent - valuePaint.fontMetrics.ascent

        val bgWidth = textWidth + paddingHorizontal * 2
        val bgHeight = textHeight + paddingTop + paddingBottom

        val gap = dpToPx(9f)

        val bgLeft = point.x - bgWidth / 2
        val bgBottom = point.y - getSelectedDotRadius() - gap
        val bgTop = bgBottom - bgHeight
        val bgRight = bgLeft + bgWidth

        canvas.drawRoundRect(
            bgLeft,
            bgTop,
            bgRight,
            bgBottom,
            dpToPx(5f),
            dpToPx(5f),
            valueBgPaint
        )

        val fm = valuePaint.fontMetrics
        val textX = point.x
        val textY = bgTop + paddingTop - fm.ascent
        canvas.drawText(label, textX, textY, valuePaint)

        val triangleHeight = dpToPx(3f)
        val triangleHalfWidth = dpToPx(5f)
        val triangleTop = bgBottom
        val triangleBottom = bgBottom + triangleHeight
        val triangleCenterX = point.x

        val trianglePath = Path().apply {
            moveTo(triangleCenterX - triangleHalfWidth, triangleTop)
            lineTo(triangleCenterX + triangleHalfWidth, triangleTop)
            lineTo(triangleCenterX, triangleBottom)
            close()
        }
        canvas.drawPath(trianglePath, valueBgPaint)
    }

    // ===== 数据点提取 =====
    protected fun getDataPoints(startIdx: Int, visibleData: List<T>): List<ChartPoint> {
        val points = mutableListOf<ChartPoint>()
        for (i in visibleData.indices) {
            val dataIndex = startIdx + i
            if (dataIndex >= allData.size) break
            val data = allData[dataIndex]
            //获取bmi/weight值
            val value = getValue(data) ?: continue
            val x = xStart + dataIndex * xInterval
            //得到y的高度
            val y = valueToY(value)
            points.add(ChartPoint(dataIndex, x, y))
        }
        return points
    }

    protected fun getFullDataPoints(): List<ChartPoint> {
        val points = mutableListOf<ChartPoint>()
        for (i in allData.indices) {
            val data = allData[i]
            val value = getValue(data) ?: continue
            //得到x坐标
            val x = xStart + i * xInterval
            //得到y坐标
            val y = valueToY(value)
            points.add(ChartPoint(i, x, y))
        }
        return points
    }

    protected data class ChartPoint(
        val dataIndex: Int,
        val x: Float,
        val y: Float
    )

    // ===== 触摸事件 =====
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (allData.isEmpty()) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!scroller.isFinished) {
                    scroller.abortAnimation()
                }
                lastTouchX = event.x
                isDragging = false
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain()
                } else {
                    velocityTracker?.clear()
                }
                velocityTracker?.addMovement(event)
                selectedDataIndex = null
                selectedValue = null
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = lastTouchX - event.x
                velocityTracker?.addMovement(event)
                if (!isDragging && abs(dx) > touchSlop) {
                    isDragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                if (isDragging) {
                    var newOffset = scrollOffset + dx
                    newOffset = newOffset.coerceIn(minScrollX, maxScrollX)
                    scrollOffset = newOffset
                    lastTouchX = event.x
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                velocityTracker?.addMovement(event)
                if (isDragging) {
                    velocityTracker?.computeCurrentVelocity(1000)
                    val velocityX = velocityTracker?.xVelocity ?: 0f
                    if (abs(velocityX) > 500f) {
                        scroller.fling(
                            scrollOffset.toInt(), 0,
                            -velocityX.toInt(), 0,
                            minScrollX.toInt(), maxScrollX.toInt(),
                            0, 0,
                            (xInterval * 0.5f).toInt(), 0
                        )
                        postInvalidateOnAnimation()
                    }
                    parent?.requestDisallowInterceptTouchEvent(false)
                } else {
                    handleClick(event.x, event.y)
                }
                isDragging = false
                velocityTracker?.recycle()
                velocityTracker = null
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            val newX = scroller.currX.toFloat()
            val clampedX = newX.coerceIn(minScrollX, maxScrollX)
            if (scrollOffset != clampedX) {
                scrollOffset = clampedX
                invalidate()
            }
            if (!scroller.computeScrollOffset()) {
                val finalX = scroller.currX.toFloat().coerceIn(minScrollX, maxScrollX)
                if (scrollOffset != finalX) {
                    scrollOffset = finalX
                    invalidate()
                }
            }
        }
    }

    protected fun handleClick(x: Float, y: Float) {
        val dataX = x + scrollOffset
        val radius = getSelectedDotRadius() + dpToPx(6f)

        val startIdx = visibleStartIndex.toInt()
        val endIdx = min(startIdx + displayCount + 1, allData.size)
        val visibleSubList = allData.subList(startIdx, endIdx)
        val points = getDataPoints(startIdx, visibleSubList)

        var hitPoint: ChartPoint? = null
        for (point in points) {
            val dx = dataX - point.x
            val dy = y - point.y
            if (dx * dx + dy * dy < radius * radius) {
                hitPoint = point
                break
            }
        }

        if (hitPoint != null) {
            val value = getValue(allData[hitPoint.dataIndex])
            if (value != null) {
                selectedDataIndex = hitPoint.dataIndex
                selectedValue = value
                invalidate()
            }
        } else {
            selectedDataIndex = null
            selectedValue = null
            invalidate()
        }
    }

    // ===== 月份锚点管理 =====
    protected fun rebuildMonthAnchors() {
        monthAnchors.clear()
        if (allData.isEmpty()) return

        when (chartMode) {
            ChartMode.DAY -> {
                for (i in allData.indices) {
                    //如果当前数据的“日”是 1 号。创建锚点
                    if (getDayOfMonth(allData[i]) == 1) {
                        monthAnchors.add(MonthAnchor(getMonth(allData[i]), i))
                    }
                }
            }

            ChartMode.WEEK -> {
                // 按月份分组：将数据按 (年份 + 月份) 分组
                val groupedByMonth = allData.groupBy { data ->
                    getYear(data) to getMonth(data)  // 用 (year, month) 作为唯一键
                }

                // 对每个月的列表，找到该月第一个周一的索引
                groupedByMonth.forEach { (key, dataList) ->
                    // key 是 (year, month)，取 month
                    val month = key.second

                    //查找第一个周一
                    val firstMondayIndex = dataList.indexOfFirst { data ->
                        val cal = getDate(data)
                        cal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY
                    }

                    // 如果找到了周一，添加到锚点列表
                    if (firstMondayIndex != -1) {
                        val globalIndex = allData.indexOf(dataList[firstMondayIndex])
                        monthAnchors.add(MonthAnchor(month, globalIndex))
                    }
                }
            }

            ChartMode.MONTH -> {
                //不需要锚点
                monthAnchors.clear()
            }
        }
    }

    //遍历所有月份锚点,重新计算每个锚点应处的精确像素位置
    protected fun updateMonthAnchorPositions() {
        for (anchor in monthAnchors) {
            //xStart图表绘图区域的起始 X 坐标
            anchor.x = xStart + anchor.dataIndex * xInterval
        }
    }

    // ===== 工具方法 =====
    protected fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density
    protected fun spToPx(sp: Float): Float = sp * resources.displayMetrics.scaledDensity

    protected fun getMonthAbbr(month: Int): String {
        return arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")[month]
    }


}