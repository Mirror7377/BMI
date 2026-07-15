package com.example.bmi.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.example.bmi.R

class BmiGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 扇环尺寸
    private val outerRadiusDp = 153f
    private val innerRadiusDp = 65f
    private val pointerOverflowDp = 11f
    private val centerRadiusDp = (outerRadiusDp + innerRadiusDp) / 2f
    private val ringWidthDp = outerRadiusDp - innerRadiusDp

    // BMI分段与对应色值
    private val bmiSplitPoints = listOf(15.6f, 16f, 17f, 18.5f, 25f, 30f, 35f, 40f, 40.3f)
    private val segmentColors = listOf(
        0xFF286DE6.toInt(),
        0xFF349CEA.toInt(),
        0xFF5BB1F5.toInt(),
        0xFFA8C526.toInt(),
        0xFFFECD2E.toInt(),
        0xFFFD9845.toInt(),
        0xFFF67D3C.toInt(),
        0xFFF04E46.toInt()
    )
    private val minBmi = bmiSplitPoints.first()
    private val maxBmi = bmiSplitPoints.last()
    private val bmiRange = maxBmi - minBmi

    // 外侧显示刻度
    private val labelValues = listOf(17f, 18.5f, 25f, 30f, 35f, 40f)
    private val labelRadiusDp = 158f
    private val labelTextSizeSp = 10f
    private lateinit var labelPaint: Paint

    // 指针参数
    private val pointerWidthDp = 90f
    private val pointerHeightDp = 22f
    private val pointerAnchorOffsetXDp = 79f
    private var pointerDrawable: Drawable? = null
    private var currentBmi: Float = 22.5f

    private lateinit var ringPaint: Paint

    init {
        // 扇环画笔
        ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dpToPx(ringWidthDp)
            strokeCap = Paint.Cap.BUTT
        }

        // 刻度文字样式严格匹配需求
        labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF000000.toInt()
            textSize = spToPx(labelTextSizeSp)
            typeface = ResourcesCompat.getFont(context, R.font.montserrat_extrabold)
            textAlign = Paint.Align.CENTER
            val textSizePx = spToPx(labelTextSizeSp)
            letterSpacing = (-0.094f / textSizePx)
        }

        // 仅加载layer_8指针，无其他layer资源读取
        val pointerResId = context.resources.getIdentifier("layer_8", "drawable", context.packageName)
        if (pointerResId != 0) {
            pointerDrawable = ResourcesCompat.getDrawable(context.resources, pointerResId, null)
        }
    }

    fun setBmi(bmi: Float) {
        currentBmi = bmi.coerceIn(minBmi, maxBmi)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val viewW = width.toFloat()
        val viewH = height.toFloat()

        // 圆心向上偏移11dp，容纳向下溢出的指针
        val cx = viewW / 2f
        val cy = viewH - dpToPx(pointerOverflowDp)
        val centerRadiusPx = dpToPx(centerRadiusDp)
        val arcRect = RectF(
            cx - centerRadiusPx,
            cy - centerRadiusPx,
            cx + centerRadiusPx,
            cy + centerRadiusPx
        )

        // 绘制彩色扇环分段
        for (i in segmentColors.indices) {
            val startAngle = bmiToAngle(bmiSplitPoints[i])
            val endAngle = bmiToAngle(bmiSplitPoints[i + 1])
            ringPaint.color = segmentColors[i]
            canvas.drawArc(arcRect, startAngle, endAngle - startAngle, false, ringPaint)
        }

        // 绘制外侧倾斜刻度文字
        val labelRadiusPx = dpToPx(labelRadiusDp)
        val fontMetrics = labelPaint.fontMetrics
        val textOffsetY = -(fontMetrics.ascent + fontMetrics.descent) / 2f
        labelValues.forEach { bmiVal ->
            val angle = bmiToAngle(bmiVal)
            val rad = Math.toRadians(angle.toDouble())
            val x = cx + Math.cos(rad).toFloat() * labelRadiusPx
            val y = cy + Math.sin(rad).toFloat() * labelRadiusPx

            canvas.save()
            canvas.translate(x, y)
            canvas.rotate(angle + 90f)
            canvas.drawText(formatLabel(bmiVal), 0f, textOffsetY, labelPaint)
            canvas.restore()
        }

        // 绘制layer_8指针
        pointerDrawable?.let { drawable ->
            val ptrW = dpToPx(pointerWidthDp)
            val ptrH = dpToPx(pointerHeightDp)
            val anchorX = dpToPx(pointerAnchorOffsetXDp)
            val targetAngle = bmiToAngle(currentBmi)

            canvas.save()
            canvas.rotate(targetAngle - 180f, cx, cy)
            val left = cx - anchorX
            val top = cy - ptrH / 2f
            drawable.setBounds(left.toInt(), top.toInt(), (left + ptrW).toInt(), (top + ptrH).toInt())
            drawable.draw(canvas)
            canvas.restore()
        }
    }

    private fun bmiToAngle(bmi: Float): Float {
        val ratio = (bmi - minBmi) / bmiRange
        return 180f + ratio * 180f
    }

    private fun formatLabel(bmi: Float): String {
        return if (bmi % 1 == 0f) bmi.toInt().toString() else String.format("%.1f", bmi)
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
    }

    private fun spToPx(sp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)
    }
}