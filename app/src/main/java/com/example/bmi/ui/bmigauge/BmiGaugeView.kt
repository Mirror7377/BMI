package com.example.bmi.ui.bmigauge


import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.res.ResourcesCompat
import com.example.bmi.R
import com.example.bmi.ui.home.enums.Gender

class BmiGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var showPointer: Boolean = true

    // 尺寸常量
    private val outerRadiusDp = 153f
    private val innerRadiusDp = 65f
    private val pointerOverflowDp = 11f
    private val centerRadiusDp = (outerRadiusDp + innerRadiusDp) / 2f
    private val ringWidthDp = outerRadiusDp - innerRadiusDp

    // 指针参数
    private val pointerWidthDp = 90f
    private val pointerHeightDp = 22f
    private val pointerAnchorOffsetXDp = 79f
    private var pointerDrawable: Drawable? = null

    // 当前配置
    private var currentConfig: BmiGaugeConfig? = null
    private var currentMin: Float = 15.6f
    private var currentMax: Float = 40.3f
    private var currentSplitPoints: List<Float> = emptyList()
    private var currentColors: List<Int> = emptyList()
    private var currentLabels: List<Float> = emptyList()

    // 当前BMI值及动画
    private var targetBmi: Float = 15.6f
    private var displayBmi: Float = 15.6f
    private var isFirstLoad = true
    private var gaugeAnimator: ValueAnimator? = null

    private lateinit var ringPaint: Paint
    private lateinit var labelPaint: Paint

    init {
        // 扇环画笔
        ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dpToPx(ringWidthDp)
            strokeCap = Paint.Cap.BUTT
        }

        // 刻度文字样式
        labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF000000.toInt()
            textSize = spToPx(10f)
            typeface = ResourcesCompat.getFont(context, R.font.montserrat_extrabold)
            textAlign = Paint.Align.CENTER
            letterSpacing = -0.094f / textSize
        }

        // 加载指针图形 (layer_8)
        val pointerResId = context.resources.getIdentifier("layer_8", "drawable", context.packageName)
        if (pointerResId != 0) {
            pointerDrawable = ResourcesCompat.getDrawable(context.resources, pointerResId, null)
        }

        // 默认应用成年配置（作为备用）
        applyConfig(BmiConfigProvider.getConfig(21, Gender.MALE.name))
    }

    /**
     * 应用扇形分段配置
     */
    fun applyConfig(config: BmiGaugeConfig) {
        currentConfig = config
        currentMin = config.min
        currentMax = config.max
        currentSplitPoints = config.splitPoints
        currentColors = config.colors
        currentLabels = config.labels
        // 重置显示值，避免越界
        displayBmi = displayBmi.coerceIn(currentMin, currentMax)
        targetBmi = targetBmi.coerceIn(currentMin, currentMax)
        invalidate()
    }

    /**
     * 设置BMI值，带动画开关
     */
    fun setBmi(bmi: Float, animate: Boolean = true) {
        val clamped = bmi.coerceIn(currentMin, currentMax)
        targetBmi = clamped
        gaugeAnimator?.cancel()

        if (!animate) {
            displayBmi = targetBmi
            isFirstLoad = false
            invalidate()
            return
        }

        val startValue = if (isFirstLoad) {
            isFirstLoad = false
            currentMin
        } else {
            displayBmi
        }

        gaugeAnimator = ValueAnimator.ofFloat(startValue, targetBmi).apply {
            duration = 800
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                displayBmi = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val viewW = width.toFloat()
        val viewH = height.toFloat()

        // 圆心位置
        val cx = viewW / 2f
        val cy = viewH - dpToPx(pointerOverflowDp)
        val centerRadiusPx = dpToPx(centerRadiusDp)
        val arcRect = RectF(
            cx - centerRadiusPx,
            cy - centerRadiusPx,
            cx + centerRadiusPx,
            cy + centerRadiusPx
        )

        // 绘制分段扇环
        val splitPoints = listOf(currentMin) + currentSplitPoints + listOf(currentMax)
        for (i in currentColors.indices) {
            val startAngle = bmiToAngle(splitPoints[i])
            val endAngle = bmiToAngle(splitPoints[i + 1])
            ringPaint.color = currentColors[i]
            canvas.drawArc(arcRect, startAngle, endAngle - startAngle, false, ringPaint)
        }

        // 绘制刻度标签
        val labelRadiusPx = dpToPx(158f)
        val fontMetrics = labelPaint.fontMetrics
        val textOffsetY = -(fontMetrics.ascent + fontMetrics.descent) / 2f
        currentLabels.forEach { value ->
            val angle = bmiToAngle(value)
            val rad = Math.toRadians(angle.toDouble())
            val x = cx + Math.cos(rad).toFloat() * labelRadiusPx
            val y = cy + Math.sin(rad).toFloat() * labelRadiusPx
            canvas.save()
            canvas.translate(x, y)
            canvas.rotate(angle + 90f)
            val text = if (value % 1 == 0f) value.toInt().toString() else String.format("%.1f", value)
            canvas.drawText(text, 0f, textOffsetY, labelPaint)
            canvas.restore()
        }

        // 绘制指针
        if (showPointer) {
            pointerDrawable?.let { drawable ->
                val ptrW = dpToPx(pointerWidthDp)
                val ptrH = dpToPx(pointerHeightDp)
                val anchorX = dpToPx(pointerAnchorOffsetXDp)
                val targetAngle = bmiToAngle(displayBmi)

                canvas.save()
                canvas.rotate(targetAngle - 180f, cx, cy)
                val left = cx - anchorX
                val top = cy - ptrH / 2f
                drawable.setBounds(
                    left.toInt(),
                    top.toInt(),
                    (left + ptrW).toInt(),
                    (top + ptrH).toInt()
                )
                drawable.draw(canvas)
                canvas.restore()
            }
        }
    }

    /**
     * 将BMI值映射到角度（0~180°）
     */
    private fun bmiToAngle(bmi: Float): Float {
        val ratio = (bmi - currentMin) / (currentMax - currentMin)
        return 180f + ratio * 180f
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
    }

    private fun spToPx(sp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        gaugeAnimator?.cancel()
        gaugeAnimator = null
        isFirstLoad = true
        displayBmi = currentMin
        targetBmi = currentMin
    }

    fun setShowPointer(show: Boolean) {
        showPointer = show
        invalidate()
    }
}