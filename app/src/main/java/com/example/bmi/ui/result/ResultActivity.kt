package com.example.bmi.ui.result

import android.animation.ValueAnimator
import android.app.Dialog
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.bmi.BaseActivity
import com.example.bmi.R
import com.example.bmi.databinding.ActivityResultBinding
import com.example.bmi.databinding.DialogBmiLegendBinding
import com.example.bmi.databinding.DialogDiscardConfirmBinding
import com.example.bmi.ui.BmiGaugeView
import com.example.bmi.ui.home.enums.Gender
import com.example.bmi.ui.home.enums.HeightUnit
import com.example.bmi.ui.home.enums.WeightUnit
import com.example.bmi.utils.BmiLevel
import com.example.bmi.utils.UnitConverter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ResultActivity : BaseActivity() {

    private lateinit var binding: ActivityResultBinding
    private val viewModel: ResultViewModel by viewModels()
    private var bmiAnimator: ValueAnimator? = null

    private val legendLevels = listOf(
        BmiLevel.VERY_SEVERELY_UNDERWEIGHT,
        BmiLevel.SEVERELY_UNDERWEIGHT,
        BmiLevel.UNDERWEIGHT,
        BmiLevel.NORMAL,
        BmiLevel.OVERWEIGHT,
        BmiLevel.OBESE_CLASS_I,
        BmiLevel.OBESE_CLASS_II,
        BmiLevel.OBESE_CLASS_III
    )

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            showDiscardDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        val bundle = intent.extras ?: Bundle()
        viewModel.initData(bundle)

        binding.tvDiscard.setOnClickListener { showDiscardDialog() }
        binding.tvSave.setOnClickListener { viewModel.saveRecord() }

        //  状态标签容器点击事件
        binding.statusContainer.setOnClickListener {
            val currentState = viewModel.state.value
            if (currentState.hasSavedRecord) {
                if (currentState.age >= 20) {
                    showBmiLegendDialog(currentState.bmiLevel)
                } else {
                    Toast.makeText(this, "功能仅对20岁以上成人开放", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 广告占位点击（预留）
        binding.llAdContainer.setOnClickListener {
            Toast.makeText(this, "广告推荐功能开发中", Toast.LENGTH_SHORT).show()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    bindState(state)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        ResultEffect.NavigateToHome -> finish()
                        ResultEffect.ShowDiscardDialog -> showDiscardDialog()
                    }
                }
            }
        }
    }

    private fun bindState(state: ResultState) {
        val hasData = state.bmi > 0
        val hasHistory = state.hasSavedRecord

        // ---------- 1. 仪表盘 & 数字动画 ----------
        if (hasData) {
            animateBmiNumber(state.bmi)
            binding.bmiGauge.setBmi(state.bmi.toFloat())
        }

        // ---------- 2. 状态标签（tvBmiStatus）----------
        val bmiLevel = state.bmiLevel
        binding.tvBmiStatus.text = bmiLevel.statusText
        val radius = dpToPx(19.75f).toFloat()

        if (hasHistory) {
            // 有历史记录 → 绿色背景 + 显示图标，可点击
            val greenBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = radius
                setColor(bmiLevel.cardBgColor) // 绿色
            }
            binding.statusContainer.background = greenBg
            binding.statusIcon.visibility = View.VISIBLE
            binding.statusContainer.isClickable = true
            binding.statusContainer.isFocusable = true
        } else {
            // 无历史记录 → 使用等级颜色，隐藏图标，不可点击
            val colorBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = radius
                setColor(bmiLevel.cardBgColor) // 等级颜色
            }
            binding.statusContainer.background = colorBg
            binding.statusIcon.visibility = View.GONE
            binding.statusContainer.isClickable = false
            binding.statusContainer.isFocusable = false
        }

        // ---------- 3. 个人信息行 ----------
        val weightText = when (state.weightUnit) {
            WeightUnit.KG.name -> String.format("%.2f kg", state.weightInput)
            WeightUnit.LB.name -> String.format("%.2f lb", state.weightInput)
            else -> String.format("%.2f kg", state.weightInput)
        }

        val heightText = when (state.heightUnit) {
            HeightUnit.CM.name -> String.format("%.1f cm", state.heightInput)
            HeightUnit.FT_IN.name -> "${state.feet} ft ${state.inches} in"
            else -> String.format("%.1f cm", state.heightInput)
        }

        val genderText = when (state.gender) {
            Gender.MALE.name -> "Male"
            Gender.FEMALE.name -> "Female"
            else -> "Male"
        }

        binding.tvBmiInfo.text = "$weightText | $heightText | $genderText | ${state.age} years old"

        // ---------- 4. 图例区域 ----------
        if (hasHistory) {
            // 有历史记录：只显示 Normal 行，隐藏其余所有行
            for (i in 0 until binding.llBmiLegend.childCount) {
                val child = binding.llBmiLegend.getChildAt(i)
                if (child.id == R.id.layoutLevel3) {
                    child.visibility = View.VISIBLE
                    binding.dotLevel3.visibility = View.GONE
                    binding.tvLevelRange3.visibility = View.GONE
                } else {
                    child.visibility = View.GONE
                }
            }
        } else {
            // 无历史记录：显示全部图例
            for (i in 0 until binding.llBmiLegend.childCount) {
                binding.llBmiLegend.getChildAt(i).visibility = View.VISIBLE
            }
            binding.dotLevel3.visibility = View.VISIBLE
            binding.tvLevelRange3.visibility = View.VISIBLE
        }

        // ---------- 5. 双卡片 + 广告位切换 ----------
        binding.groupNoData.visibility = if (!hasHistory) View.VISIBLE else View.GONE
        binding.groupHasData.visibility = if (hasHistory) View.VISIBLE else View.GONE
        binding.llAdContainer.visibility = if (hasHistory) View.VISIBLE else View.GONE

        // ---------- 6. 卡片背景圆角 ----------
        setTipCardRadius(binding.llBottomTip)
        setTipCardRadius(binding.llBottomTipHasData)

        // ---------- 7. 卡片内容同步 ----------
        renderBottomTip(
            bmiLevel = bmiLevel,
            userWeightInput = state.weightInput,
            userWeightUnitStr = state.weightUnit,
            userHeightCm = state.heightCm,
            userHeightDisplayText = heightText,
            tvDesc = binding.tvTipDesc,
            tvMain = binding.tvTipMain,
            tvRange = binding.tvTipRange,
            tvDiff = binding.tvTipDiff,
            tvDescHasData = binding.tvTipDescHasData,
            tvMainHasData = binding.tvTipMainHasData,
            tvRangeHasData = binding.tvTipRangeHasData,
            tvDiffHasData = binding.tvTipDiffHasData
        )

        // ---------- 8. 图例高亮（仅在无历史记录时显示） ----------
        if (!hasHistory) {
            bindBmiLegend(bmiLevel)
        }
    }

    // ---------- 辅助方法 ----------
    private fun animateBmiNumber(targetBmi: Double) {
        bmiAnimator?.cancel()
        bmiAnimator = ValueAnimator.ofFloat(0f, targetBmi.toFloat()).apply {
            duration = 800
            addUpdateListener { animation ->
                val current = animation.animatedValue as Float
                binding.tvBmiValue.text = String.format("%.1f", current)
            }
            start()
        }
    }

    private fun setTipCardRadius(vararg views: View) {
        views.forEach { view ->
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(15f).toFloat()
                setColor(0xFFF4F4F4.toInt())
            }
            view.background = drawable
        }
    }

    private fun getStandardWeightRangeCm(heightCm: Double): Pair<Double, Double> {
        val h = heightCm / 100.0
        return Pair(18.5 * h * h, 24.9 * h * h)
    }

    private fun renderBottomTip(
        bmiLevel: BmiLevel,
        userWeightInput: Double,
        userWeightUnitStr: String,
        userHeightCm: Double,
        userHeightDisplayText: String,
        tvDesc: TextView,
        tvMain: TextView,
        tvRange: TextView,
        tvDiff: TextView,
        tvDescHasData: TextView,
        tvMainHasData: TextView,
        tvRangeHasData: TextView,
        tvDiffHasData: TextView
    ) {
        val (stdMinKg, stdMaxKg) = getStandardWeightRangeCm(userHeightCm)
        val isUserKg = userWeightUnitStr == WeightUnit.KG.name

        val (stdMinShow, stdMaxShow, userWeightShow) = if (isUserKg) {
            Triple(stdMinKg, stdMaxKg, userWeightInput)
        } else {
            Triple(
                UnitConverter.kgToLb(stdMinKg),
                UnitConverter.kgToLb(stdMaxKg),
                userWeightInput
            )
        }

        val unitStr = if (isUserKg) "kg" else "lb"
        val descText = bmiLevel.descText

        // 描述文字
        listOf(tvDesc, tvDescHasData).forEach { it.text = descText; it.visibility = View.VISIBLE }

        if (bmiLevel == BmiLevel.NORMAL) {
            listOf(tvMain, tvMainHasData).forEach { it.visibility = View.GONE }
            listOf(tvRange, tvRangeHasData).forEach { it.visibility = View.GONE }
        } else {
            val mainText = "Normal weight for your height($userHeightDisplayText)"
            listOf(tvMain, tvMainHasData).forEach {
                it.text = mainText
                it.visibility = View.VISIBLE
            }

            val rangeStr = String.format("%.1f%s - %.1f%s", stdMinShow, unitStr, stdMaxShow, unitStr)
            val diffValue = if (userWeightShow < stdMinShow) {
                stdMinShow - userWeightShow
            } else {
                stdMaxShow - userWeightShow
            }
            val diffSign = if (userWeightShow < stdMinShow) "+" else ""
            val diffText = String.format(" (%s%.1f%s)", diffSign, diffValue, unitStr)
            val fullText = "$rangeStr$diffText"
            val spannable = SpannableString(fullText)
            val redColor = 0xFFFF3333.toInt()
            spannable.setSpan(
                ForegroundColorSpan(redColor),
                rangeStr.length,
                fullText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            listOf(tvRange, tvRangeHasData).forEach {
                it.text = spannable
                it.visibility = View.VISIBLE
            }
        }
        listOf(tvDiff, tvDiffHasData).forEach { it.visibility = View.GONE }
    }

    private fun bindBmiLegend(currentLevel: BmiLevel) {
        val radius = dpToPx(15f).toFloat()
        val whiteColor = 0xFFFFFFFF.toInt()
        val blackTextColor = 0xFF000000.toInt()

        val boldTypeface = resources.getFont(R.font.montserrat_extrabold)
        val regularTypeface = resources.getFont(R.font.montserrat_regular)

        val layouts = listOf(
            binding.layoutLevel0, binding.layoutLevel1, binding.layoutLevel2, binding.layoutLevel3,
            binding.layoutLevel4, binding.layoutLevel5, binding.layoutLevel6, binding.layoutLevel7
        )
        val dots = listOf(
            binding.dotLevel0, binding.dotLevel1, binding.dotLevel2, binding.dotLevel3,
            binding.dotLevel4, binding.dotLevel5, binding.dotLevel6, binding.dotLevel7
        )
        val nameTvs = listOf(
            binding.tvLevelName0, binding.tvLevelName1, binding.tvLevelName2, binding.tvLevelName3,
            binding.tvLevelName4, binding.tvLevelName5, binding.tvLevelName6, binding.tvLevelName7
        )
        val rangeTvs = listOf(
            binding.tvLevelRange0, binding.tvLevelRange1, binding.tvLevelRange2, binding.tvLevelRange3,
            binding.tvLevelRange4, binding.tvLevelRange5, binding.tvLevelRange6, binding.tvLevelRange7
        )

        legendLevels.forEachIndexed { index, level ->
            val levelColor = level.cardBgColor
            val layout = layouts[index]
            val dot = dots[index]
            val nameTv = nameTvs[index]
            val rangeTv = rangeTvs[index]

            if (level == currentLevel) {
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = radius
                    setColor(levelColor)
                }
                layout.background = bg
                (dot.background as GradientDrawable).setColor(whiteColor)
                nameTv.typeface = boldTypeface
                rangeTv.typeface = boldTypeface
                nameTv.setTextColor(whiteColor)
                rangeTv.setTextColor(whiteColor)
            } else {
                layout.background = null
                (dot.background as GradientDrawable).setColor(levelColor)
                nameTv.typeface = regularTypeface
                rangeTv.typeface = regularTypeface
                nameTv.setTextColor(blackTextColor)
                rangeTv.setTextColor(blackTextColor)
            }
        }
    }

    // ----------  弹出层相关 ----------
    private fun showBmiLegendDialog(bmiLevel: BmiLevel) {
        val dialogBinding = DialogBmiLegendBinding.inflate(layoutInflater)
        val dialog = Dialog(this)
        dialog.setContentView(dialogBinding.root)

        val window = dialog.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        // 🎯 从底部弹出，固定宽度 375dp，高度 538.5dp
        window?.setGravity(Gravity.BOTTOM)
        window?.setLayout(
            dpToPx(375f),
            dpToPx(600f)
        )

        // 可选：添加从底部滑入的动画（需要创建动画资源）
        // window?.setWindowAnimations(R.style.BottomSheetAnimation)

        // 设置扇形图无指针
        dialogBinding.bmiGaugeDialog.setShowPointer(false)

        // 应用图例高亮
        applyLegendHighlight(dialogBinding, bmiLevel)

        // GOT IT 按钮
        dialogBinding.btnGotIt.setOnClickListener {
            dialog.dismiss()
        }

        // 点击外部可关闭
        dialog.setCanceledOnTouchOutside(true)

        dialog.show()
    }

    /**
     * 对弹窗中的图例应用高亮
     */
    private fun applyLegendHighlight(binding: DialogBmiLegendBinding, currentLevel: BmiLevel) {
        val radius = dpToPx(15f).toFloat()
        val whiteColor = 0xFFFFFFFF.toInt()
        val blackTextColor = 0xFF000000.toInt()

        val boldTypeface = resources.getFont(R.font.montserrat_extrabold)
        val regularTypeface = resources.getFont(R.font.montserrat_regular)

        // 使用 binding 中的视图引用
        val layouts = listOf(
            binding.layoutLevel0, binding.layoutLevel1, binding.layoutLevel2, binding.layoutLevel3,
            binding.layoutLevel4, binding.layoutLevel5, binding.layoutLevel6, binding.layoutLevel7
        )
        val dots = listOf(
            binding.dotLevel0, binding.dotLevel1, binding.dotLevel2, binding.dotLevel3,
            binding.dotLevel4, binding.dotLevel5, binding.dotLevel6, binding.dotLevel7
        )
        val nameTvs = listOf(
            binding.tvLevelName0, binding.tvLevelName1, binding.tvLevelName2, binding.tvLevelName3,
            binding.tvLevelName4, binding.tvLevelName5, binding.tvLevelName6, binding.tvLevelName7
        )
        val rangeTvs = listOf(
            binding.tvLevelRange0, binding.tvLevelRange1, binding.tvLevelRange2, binding.tvLevelRange3,
            binding.tvLevelRange4, binding.tvLevelRange5, binding.tvLevelRange6, binding.tvLevelRange7
        )

        legendLevels.forEachIndexed { index, level ->
            val levelColor = level.cardBgColor
            val layout = layouts[index]
            val dot = dots[index]
            val nameTv = nameTvs[index]
            val rangeTv = rangeTvs[index]

            if (level == currentLevel) {
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = radius
                    setColor(levelColor)
                }
                layout.background = bg
                (dot.background as GradientDrawable).setColor(whiteColor)
                nameTv.typeface = boldTypeface
                rangeTv.typeface = boldTypeface
                nameTv.setTextColor(whiteColor)
                rangeTv.setTextColor(whiteColor)
            } else {
                layout.background = null
                (dot.background as GradientDrawable).setColor(levelColor)
                nameTv.typeface = regularTypeface
                rangeTv.typeface = regularTypeface
                nameTv.setTextColor(blackTextColor)
                rangeTv.setTextColor(blackTextColor)
            }
        }
    }

    // ---------- 工具方法 ----------
    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun showDiscardDialog() {
        val dialogBinding = DialogDiscardConfirmBinding.inflate(layoutInflater)
        val dialog = Dialog(this)
        dialog.setContentView(dialogBinding.root)
        val window = dialog.window ?: return

        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.decorView.setPadding(0, 0, 0, 0)
        window.setGravity(Gravity.CENTER)
        window.setLayout(dpToPx(301f), dpToPx(154f))
        dialog.setCancelable(true)

        dialogBinding.tvCancel.setOnClickListener { dialog.dismiss() }
        dialogBinding.tvDelete.setOnClickListener {
            dialog.dismiss()
            finish()
        }
        dialog.show()
    }

    override fun onDestroy() {
        bmiAnimator?.cancel()
        bmiAnimator = null
        super.onDestroy()
    }
}