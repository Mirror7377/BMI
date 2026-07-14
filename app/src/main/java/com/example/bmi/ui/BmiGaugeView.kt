package com.example.bmi.ui

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat

class BmiGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 固定尺寸（dp）
    private val rectWidthDp = 306f
    private val rectHeightDp = 153f
    private val outerRadiusDp = 153f
    private val innerRadiusDp = 75f   // 新增：内半径

    // 每张图片的宽度（dp），索引 0~7
    private val imageWidthDp = floatArrayOf(
        153f, 153f, 153f, 153f,  // 0~3
        237f, 307f, 307f, 307f   // 4~7
    )

    private val drawables = mutableListOf<Drawable>()

    init {
        for (i in 0..7) {
            val resId = context.resources.getIdentifier("layer_$i", "drawable", context.packageName)
            ContextCompat.getDrawable(context, resId)?.let {
                drawables.add(it)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (drawables.size < 8) return

        // 转换为 px
        val outerRadius = dpToPx(outerRadiusDp)
        val innerRadius = dpToPx(innerRadiusDp)
        val rectW = dpToPx(rectWidthDp)
        val rectH = dpToPx(rectHeightDp)

        // 居中矩形
        val viewW = width.toFloat()
        val viewH = height.toFloat()
        val left = (viewW - rectW) / 2
        val top = (viewH - rectH) / 2
        val right = left + rectW
        val bottom = top + rectH

        // 圆心（底边中点）
        val cx = left + rectW / 2
        val cy = bottom

        // 矩形左下角
        val lx = left
        val ly = bottom

        // ----- 构建扇环裁剪路径（外半圆 - 内半圆） -----
        val outerPath = Path().apply {
            moveTo(cx - outerRadius, cy)
            arcTo(
                RectF(cx - outerRadius, cy - outerRadius, cx + outerRadius, cy + outerRadius),
                180f, 180f
            )
            close()
        }

        val innerPath = Path().apply {
            moveTo(cx - innerRadius, cy)
            arcTo(
                RectF(cx - innerRadius, cy - innerRadius, cx + innerRadius, cy + innerRadius),
                180f, 180f
            )
            close()
        }

        // 计算差集：外半圆 - 内半圆 = 扇环
        val ringPath = Path()
        ringPath.op(outerPath, innerPath, Path.Op.DIFFERENCE)

        // ----- 应用裁剪 -----
        canvas.save()
        // 先裁剪矩形边界（保证不超出 View）
        canvas.clipRect(left, top, right, bottom)
        // 再裁剪扇环路径
        canvas.clipPath(ringPath)

        // ----- 绘制图片（从大到小）-----
        for (i in 7 downTo 0) {
            val drawable = drawables[i]
            val intrinsicW = drawable.intrinsicWidth.toFloat()
            val intrinsicH = drawable.intrinsicHeight.toFloat()
            if (intrinsicW <= 0 || intrinsicH <= 0) continue

            val targetWidthPx = dpToPx(imageWidthDp[i])
            val scale = targetWidthPx / intrinsicW
            val scaledW = targetWidthPx
            val scaledH = intrinsicH * scale

            val bounds = if (i < 4) {
                // 0~3：右下角对齐圆心
                RectF(cx - scaledW, cy - scaledH, cx, cy)
            } else {
                // 4~7：左下角对齐矩形左下角
                RectF(lx, ly - scaledH, lx + scaledW, ly)
            }

            drawable.setBounds(
                bounds.left.toInt(),
                bounds.top.toInt(),
                bounds.right.toInt(),
                bounds.bottom.toInt()
            )
            drawable.draw(canvas)
        }

        canvas.restore()
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
    }
}