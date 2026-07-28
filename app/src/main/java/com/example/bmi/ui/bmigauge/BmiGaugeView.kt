package com.example.bmi.ui.bmigauge


import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.bmi.R
import com.example.bmi.data.enums.Gender
import kotlin.math.cos
import kotlin.math.sin

class BmiGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var showPointer: Boolean = true

    // 尺寸常量
    //指针底部圆心的半径
    private val pointerOverflowDp = 11f
    private val outerRadiusDp = 153f   // 外圈半径
    private val innerRadiusDp = 65f    // 内圈半径
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

    private var ringPaint: Paint
    private var labelPaint: Paint

    private val arcRect = RectF()//（浮点型矩形）

    init {
        // 扇环画笔                     开启抗锯齿
        ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE//设置画笔为描边模式
            strokeWidth = dpToPx(ringWidthDp)//笔头宽度为 88dp（外径153 - 内径65）画成圆环
            strokeCap = Paint.Cap.BUTT//弧线两端不加圆头或方头装饰，保持平直切口
        }

        // 刻度文字样式
        labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF000000.toInt()
            textSize = spToPx(10f)
            typeface = ResourcesCompat.getFont(context, R.font.montserrat_extrabold)
            textAlign = Paint.Align.CENTER//文字水平居中对齐
            letterSpacing = -0.094f / textSize
        }

        // 加载指针图形 (layer_8)
        pointerDrawable = ContextCompat.getDrawable(context, R.drawable.layer_8)

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
        invalidate()// 触发 onDraw
    }

    /**
     * 设置BMI值的动画
     */
    fun setBmi(bmi: Float, animate: Boolean = true) {
        val clamped = bmi.coerceIn(currentMin, currentMax)
        targetBmi = clamped
        gaugeAnimator?.cancel()//取消旧动画


        //多方法复用
        if (!animate) {
            //不需要动画
            displayBmi = targetBmi//把显示值“瞬间”设为目标值
            //标记首次加载：isFirstLoad = false
            isFirstLoad = false
            invalidate()//数值变了，视图必须刷新,会调用onDraw(Canvas) 方法。去绘制指针
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
                //刷新数据
                displayBmi = it.animatedValue as Float
                invalidate()//重新绘制
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //表示这个自定义控件实际占用的像素宽高。
        val viewW = width.toFloat()
        val viewH = height.toFloat()

        // 圆心位置
        val cx = viewW / 2f
        val cy = viewH - dpToPx(pointerOverflowDp)
        val centerRadiusPx = dpToPx(centerRadiusDp)
        arcRect.set(
            cx - centerRadiusPx,
            cy - centerRadiusPx,
            cx + centerRadiusPx,
            cy + centerRadiusPx
        )

        // 绘制分段扇环           各个bmi值
        val splitPoints = listOf(currentMin) + currentSplitPoints + listOf(currentMax)
        for (i in currentColors.indices) {//8种bmi颜色
            //把 BMI 数值转换成角度
            val startAngle = bmiToAngle(splitPoints[i])
            val endAngle = bmiToAngle(splitPoints[i + 1])
            // 设置画笔颜色
            ringPaint.color = currentColors[i]
                                //模具    起点角度                //扫过的角度               是否连接圆心        画笔
            canvas.drawArc(arcRect, startAngle, endAngle - startAngle, false, ringPaint)
        }

        // 绘制数字刻度
        // 确定“标签放在多远的半径上
        val labelRadiusPx = dpToPx(158f)
        val fontMetrics = labelPaint.fontMetrics//获取当前画笔对象的字体数据
        //ascent文字最顶部   descent文字最底部    Y轴向下为正
        val textOffsetY = -(fontMetrics.ascent + fontMetrics.descent) / 2f
        currentLabels.forEach { value ->
            val angle = bmiToAngle(value)//角度
            val rad = Math.toRadians(angle.toDouble())//把角度转成弧度
            //cos(rad) 和 sin(rad) 分别是角度在 X 轴和 Y 轴上的分量（方向向量）。
            //乘以半径后，就得到了相对于圆心 (cx, cy) 的偏移量。
            //加上圆心坐标，最终得到了 (x, y)
            val x = cx + cos(rad).toFloat() * labelRadiusPx
            val y = cy + sin(rad).toFloat() * labelRadiusPx
            canvas.save()                         //  保存当前画布状态（备份）
            canvas.translate(x, y)                //  把原点（0,0）移动到 (x, y)
            canvas.rotate(angle + 90f)            //  旋转90度，此时文字垂直圆心
            //格式化文本（去掉多余的 .0）
            val text = if (value % 1 == 0f) value.toInt().toString() else String.format("%.1f", value)
            //绘制文字
            canvas.drawText(text, 0f, textOffsetY, labelPaint)
            //把之前 save() 的状态恢复，撤销 translate 和 rotate 的影响。
            //撤销最近一次 canvas.save() 之后所做的所有变换操作
            canvas.restore()
        }

        // 绘制指针
        if (showPointer) {
            pointerDrawable?.let { drawable ->
                val ptrW = dpToPx(pointerWidthDp)//90
                val ptrH = dpToPx(pointerHeightDp)//11
                val anchorX = dpToPx(pointerAnchorOffsetXDp)// 旋转“枢轴”偏移量（79dp）留了底部圆心的半径11
                val targetAngle = bmiToAngle(displayBmi)//根据bmi值计算目标角度

                canvas.save()
                // 以表盘圆心 (cx, cy) 为轴旋转画布。
        // 因为指针图片默认朝左（180°），而 bmiToAngle 返回 180°~360°，
        // 所以减去 180° 能让图片的“基准朝左”恰好对准数值起始的 180° 方向。
                canvas.rotate(targetAngle - 180f, cx, cy)

                // 指针图片上的“旋转枢轴点”（锚点）距离图片左边界为 anchorX。
                // 将左边界往左移 anchorX，使得这个枢轴点恰好落在表盘圆心 (cx, cy) 上。
                val left = cx - anchorX
                // 垂直方向居中（让指针在 Y 轴上下对称）
                val top = cy - ptrH / 2f
                // 划定指针图片在屏幕上的实际显示区域（矩形框）
                drawable.setBounds(
                    left.toInt(),
                    top.toInt(),
                    (left + ptrW).toInt(),
                    (top + ptrH).toInt()
                )
                // 将指针绘制到画布上。
                // 此时画布已被旋转，因此画出来的指针会随着画布旋转到目标角度。
                drawable.draw(canvas)
                // 恢复画布变换（撤销旋转），避免影响后续其他元素的绘制。
                canvas.restore()
            }
        }
    }

    /**
     * 将BMI值映射到角度（0~180°）
     */
    private fun bmiToAngle(bmi: Float): Float {
        //计算百分比
        val ratio = (bmi - currentMin) / (currentMax - currentMin)
        return 180f + ratio * 180f
        //在 Android 的 Canvas 里，0° 是 3 点钟方向（正右），180° 是 9 点钟方向（正左）
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
        invalidate()//绘制数据
    }
}