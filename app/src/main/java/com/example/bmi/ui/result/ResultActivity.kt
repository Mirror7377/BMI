package com.example.bmi.ui.result

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.bmi.BaseActivity
import com.example.bmi.R
import com.example.bmi.data.database.RecommendApp
import com.example.bmi.databinding.ActivityResultBinding
import com.example.bmi.databinding.DialogBmiLegendBinding
import com.example.bmi.databinding.DialogDiscardConfirmBinding
import com.example.bmi.ui.bmigauge.BmiConfigProvider
import com.example.bmi.ui.home.enums.Gender
import com.example.bmi.ui.home.enums.HeightUnit
import com.example.bmi.ui.home.enums.WeightUnit
import com.example.bmi.ui.bmigauge.BmiLevel
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

        //返回键
        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        val bundle = intent.extras ?: Bundle()
        viewModel.initData(bundle)

        binding.tvDiscard.setOnClickListener { showDiscardDialog() }
        binding.tvSave.setOnClickListener { viewModel.saveRecord() }

        //  状态标签容器点击事件
        binding.statusContainer.setOnClickListener {
            val state = viewModel.state.value
            showBmiLegendDialog(state.bmiLevel, state.age, state.gender)
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
                        is ResultEffect.NavigateToHome -> {
                            //  保存目标到 SharedPreferences
                            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                            val target = if (effect.isFirstSave) "display" else "statistics"
                            prefs.edit().putString("post_save_target", target).apply()
                            // 2. 关闭当前页面
                            finish()
                        }
                        ResultEffect.ShowDiscardDialog -> showDiscardDialog()
                    }
                }
            }
        }
    }

    private fun bindState(state: ResultState) {
        val hasData = state.bmi > 0
        val hasHistory = state.hasSavedRecord

        // 1. 应用扇形配置（根据年龄性别）
        val config = BmiConfigProvider.getConfig(state.age, state.gender)
        binding.bmiGauge.applyConfig(config)

        // 2. 仪表盘 & 数字动画
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

        if (hasHistory) {
            val recommendedApps = state.recommendedApps
            if (recommendedApps.size >= 3) {
                bindAppToCard(
                    binding.adCard1,
                    binding.ivAppIcon1,
                    binding.tvAppName1,
                    binding.tvAppCategory1,
                    binding.rbAppRating1,
                    binding.tvAppRating1,
                    recommendedApps[0]
                )
                bindAppToCard(
                    binding.adCard2,
                    binding.ivAppIcon2,
                    binding.tvAppName2,
                    binding.tvAppCategory2,
                    binding.rbAppRating2,
                    binding.tvAppRating2,
                    recommendedApps[1]
                )
                bindAppToCard(
                    binding.adCard3,
                    binding.ivAppIcon3,
                    binding.tvAppName3,
                    binding.tvAppCategory3,
                    binding.rbAppRating3,
                    binding.tvAppRating3,
                    recommendedApps[2]
                )
            }
        }

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
            val diffSign = if (userWeightShow < stdMinShow) "+" else "-"
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
    private fun showBmiLegendDialog(bmiLevel: BmiLevel, age: Int, gender: String) {
        val dialogBinding = DialogBmiLegendBinding.inflate(layoutInflater)
        val dialog = Dialog(this)
        dialog.setContentView(dialogBinding.root)

        val window = dialog.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.setGravity(Gravity.BOTTOM)
        // 宽度固定 375dp，高度自适应 wrap_content
        window?.setLayout(dpToPx(375f), WindowManager.LayoutParams.WRAP_CONTENT)

        // 判断是否为儿童（2~20岁）
        val isChild = age in 2..20

        // 1. 设置标题
        dialogBinding.tvDialogTitle.text = if (isChild) "BMI for Teenagers" else "BMI for Adults"

        // 2. 设置年龄性别行
        val genderText = when (gender) {
            Gender.MALE.name -> "male"
            Gender.FEMALE.name -> "female"
            else -> "male"
        }
        if (isChild) {
            dialogBinding.tvAgeGender.visibility = View.VISIBLE
            dialogBinding.tvAgeGender.text = "$age years old ($genderText)"
        } else {
            dialogBinding.tvAgeGender.visibility = View.GONE
        }

        // 3. 设置仪表盘配置（动态）
        val config = BmiConfigProvider.getConfig(age, gender)
        dialogBinding.bmiGaugeDialog.applyConfig(config)
        dialogBinding.bmiGaugeDialog.setShowPointer(false)

        // 4. 应用图例高亮（动态隐藏/显示行，设置范围值）
        applyLegendHighlight(dialogBinding, bmiLevel, age, gender)

        // 5. GOT IT 按钮
        dialogBinding.btnGotIt.setOnClickListener { dialog.dismiss() }
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
    }

    /**
     * 对弹窗中的图例应用高亮 todo 代码复用
     */
    private fun applyLegendHighlight(
        binding: DialogBmiLegendBinding,
        currentLevel: BmiLevel,
        age: Int,
        gender: String
    ) {
        val isChild = age in 2..20
        val config = BmiConfigProvider.getConfig(age, gender)
        val splitPoints = config.splitPoints
        val colors = config.colors

        val radius = dpToPx(15f).toFloat()
        val whiteColor = 0xFFFFFFFF.toInt()
        val blackTextColor = 0xFF000000.toInt()

        val boldTypeface = resources.getFont(R.font.montserrat_extrabold)
        val regularTypeface = resources.getFont(R.font.montserrat_regular)

        // 所有行视图列表
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

        // 等级名称
        val levelNames = listOf(
            "Very Severely Underweight",
            "Severely Underweight",
            "Underweight",
            "Normal",
            "Overweight",
            "Obese Class I",
            "Obese Class II",
            "Obese Class III"
        )

        // 儿童可见索引：2,3,4,5 (Underweight, Normal, Overweight, Obese I)
        val visibleIndices = if (isChild) listOf(2, 3, 4, 5) else (0..7).toList()

        // 儿童颜色映射
        val childColors = mapOf(
            2 to 0xFF5BB1F5.toInt(), // Underweight
            3 to 0xFFA8C526.toInt(), // Normal
            4 to 0xFFFECD2E.toInt(), // Overweight
            5 to 0xFFFD9845.toInt()  // Obese I
        )

        // 获取该等级对应的分界点下标
        fun getSplitIndex(index: Int): Int = when (index) {
            2 -> 0      // Underweight → 第一个分界点
            3 -> 1      // Normal → 第二个分界点
            4 -> 2      // Overweight → 第三个分界点
            5 -> 2      // Obese I → 第三个分界点（从此开始）
            else -> -1
        }

        // 生成范围文本
        fun getRangeText(index: Int): String {
            if (splitPoints.isEmpty()) return ""
            val s = splitPoints
            return when (index) {
                2 -> "＜${s[0]}"
                3 -> "${s[0]} - ${s[1]}"
                4 -> "${s[1]} - ${s[2]}"
                5 -> "≥${s[2]}"
                else -> ""
            }
        }

        legendLevels.forEachIndexed { index, level ->
            val layout = layouts[index]
            val dot = dots[index]
            val nameTv = nameTvs[index]
            val rangeTv = rangeTvs[index]

            val shouldShow = index in visibleIndices

            if (!shouldShow) {
                layout.visibility = View.GONE
                return@forEachIndexed
            } else {
                layout.visibility = View.VISIBLE
            }

            // 设置名称
            nameTv.text = levelNames[index]

            // 确定颜色
            val color = if (isChild) {
                childColors[index] ?: level.cardBgColor
            } else {
                level.cardBgColor
            }

            // 确定范围文本
            val rangeText = if (isChild) {
                getRangeText(index)
            } else {
                // 成人使用原有硬编码范围（保留原样）
                when (index) {
                    0 -> "＜16"
                    1 -> "16.0-16.9"
                    2 -> "17.0-18.4"
                    3 -> "18.5-24.9"
                    4 -> "25.0-29.9"
                    5 -> "30.0-34.9"
                    6 -> "35.0-39.9"
                    7 -> "≥40.0"
                    else -> ""
                }
            }

            // 高亮当前等级
            if (level == currentLevel) {
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = radius
                    setColor(color)
                }
                layout.background = bg
                (dot.background as GradientDrawable).setColor(whiteColor)
                nameTv.typeface = boldTypeface
                rangeTv.typeface = boldTypeface
                nameTv.setTextColor(whiteColor)
                rangeTv.setTextColor(whiteColor)
            } else {
                layout.background = null
                (dot.background as GradientDrawable).setColor(color)
                nameTv.typeface = regularTypeface
                rangeTv.typeface = regularTypeface
                nameTv.setTextColor(blackTextColor)
                rangeTv.setTextColor(blackTextColor)
            }

            rangeTv.text = rangeText
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
        //todo 返回到home页面后弹出 toast显示已删除
        dialogBinding.tvDelete.setOnClickListener {
            dialog.dismiss()
            finish()
        }
        dialog.show()
    }

    private fun bindAppToCard(
        cardView: View,
        iconView: ImageView,
        nameView: TextView,
        categoryView: TextView,
        ratingBar: RatingBar,
        ratingTextView: TextView,
        app: RecommendApp?
    ) {
        if (app == null) {
            cardView.visibility = View.GONE
            return
        }
        cardView.visibility = View.VISIBLE

        iconView.setImageResource(app.iconResId)
        nameView.text = app.name
        categoryView.text = app.category
        ratingBar.rating = app.rating.toFloat()
        ratingTextView.text = String.format("%.1f", app.rating)

        // 点击跳转
        cardView.setOnClickListener {
            val intent =
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${app.packageName}"))
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        bmiAnimator?.cancel()
        bmiAnimator = null
        super.onDestroy()
    }
}