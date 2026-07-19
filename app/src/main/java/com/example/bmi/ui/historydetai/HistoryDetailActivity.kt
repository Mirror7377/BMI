package com.example.bmi.ui.historydetai

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
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.bmi.BaseActivity
import com.example.bmi.MainActivity
import com.example.bmi.R
import com.example.bmi.data.database.RecommendApp
import com.example.bmi.databinding.ActivityHistoryDetailBinding
import com.example.bmi.databinding.DialogBmiLegendBinding
import com.example.bmi.databinding.DialogDiscardConfirmBinding
import com.example.bmi.ui.bmigauge.BmiConfigProvider
import com.example.bmi.ui.historydetail.HistoryDetailEffect
import com.example.bmi.ui.historydetail.HistoryDetailIntent
import com.example.bmi.ui.historydetail.HistoryDetailState
import com.example.bmi.ui.historydetail.HistoryDetailViewModel
import com.example.bmi.ui.home.enums.Gender
import com.example.bmi.ui.home.enums.HeightUnit
import com.example.bmi.ui.home.enums.WeightUnit
import com.example.bmi.ui.bmigauge.BmiLevel
import com.example.bmi.utils.UnitConverter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class HistoryDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityHistoryDetailBinding
    private val viewModel: HistoryDetailViewModel by viewModels()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 从 Intent 获取 recordId
        val recordId = intent.getLongExtra("RECORD_ID", 0L)
        if (recordId == 0L) {
            Toast.makeText(this, "Invalid record", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewModel.handleIntent(HistoryDetailIntent.LoadRecord(recordId))

        // 返回按钮
        binding.ivBack.setOnClickListener {
            viewModel.handleIntent(HistoryDetailIntent.BackPressed)
        }

        // Delete 按钮
        binding.tvDelete.setOnClickListener {
            showDeleteConfirmDialog()
        }

        // 状态标签点击 → 弹出图例（若年龄≥20）
        binding.statusContainer.setOnClickListener {
            val state = viewModel.state.value
            showBmiLegendDialog(state.bmiLevel, state.age, state.gender)

        }

        // 观察状态
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    bindState(state)
                }
            }
        }

        // 观察副作用
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        is HistoryDetailEffect.NavigateBack -> finish()
                        is HistoryDetailEffect.NavigateToHome -> {
                            // 跳转到 Home 页面，并清空返回栈
                            val intent = Intent(this@HistoryDetailActivity, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            finish()
                        }
                        is HistoryDetailEffect.ShowError -> Toast.makeText(this@HistoryDetailActivity, effect.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun bindState(state: HistoryDetailState) {
        if (state.isLoading) return
        if (state.bmi <= 0) return

        // 1. 应用扇形配置（根据年龄性别）
        val config = BmiConfigProvider.getConfig(state.age, state.gender)
        binding.bmiGauge.applyConfig(config)

        // 2. 仪表盘指针：不带动画
        binding.bmiGauge.setBmi(state.bmi.toFloat(), false)

        // 3. 数字文本：直接显示，无动画
        binding.tvBmiValue.text = String.format("%.1f", state.bmi)

        // 2. 状态标签：背景根据 BMI 等级变化，显示问号
        val bmiLevel = state.bmiLevel
        binding.tvBmiStatus.text = bmiLevel.statusText
        val radius = dpToPx(19.75f).toFloat()
        val levelColor = bmiLevel.cardBgColor  // 动态获取等级颜色
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(levelColor)
        }
        binding.statusContainer.background = bg
        binding.statusIcon.visibility = View.VISIBLE
        binding.statusContainer.isClickable = true
        binding.statusContainer.isFocusable = true

        // 3. 个人信息行
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

        // 4. 日期显示
        // 在 bindState 方法中，设置顶部日期后，再设置横线中间的日期+时段
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val dateStr = dateFormat.format(Date(state.timestamp))
        val timeStr = state.timeOfDay
        // 组合显示，例如 "Mar 15, 2025 Morning"
        binding.tvDividerDateTime.text = "$dateStr $timeStr"

        // 5. 灰色提示卡片
        renderBottomTip(
            bmiLevel = bmiLevel,
            userWeightInput = state.weightInput,
            userWeightUnitStr = state.weightUnit,
            userHeightCm = state.heightCm,
            userHeightDisplayText = heightText,
            tvDesc = binding.tvTipDescHasData,
            tvMain = binding.tvTipMainHasData,
            tvRange = binding.tvTipRangeHasData,
            tvDiff = binding.tvTipDiffHasData
        )

        // 6. 推荐 App
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

        // 7. 卡片背景圆角
        setTipCardRadius(binding.llBottomTipHasData)
    }

    // ---------- 辅助方法（从 ResultActivity 复制） ----------

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
        tvDiff: TextView
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

        tvDesc.text = descText
        tvDesc.visibility = View.VISIBLE

        if (bmiLevel == BmiLevel.NORMAL) {
            tvMain.visibility = View.GONE
            tvRange.visibility = View.GONE
        } else {
            val mainText = "Normal weight for your height($userHeightDisplayText)"
            tvMain.text = mainText
            tvMain.visibility = View.VISIBLE

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
            tvRange.text = spannable
            tvRange.visibility = View.VISIBLE
        }
        tvDiff.visibility = View.GONE
    }

    // ---------- 图例弹窗 ----------
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

    //todo 代码复用
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

    // ---------- 绑定 App 卡片 ----------
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

        cardView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${app.packageName}"))
            startActivity(intent)
        }
    }

    // ---------- 工具 ----------
    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        bmiAnimator?.cancel()
        bmiAnimator = null
        super.onDestroy()
    }

    // todo 公共
    private fun showDeleteConfirmDialog() {
        val dialogBinding = DialogDiscardConfirmBinding.inflate(layoutInflater)
        val dialog = Dialog(this)
        dialog.setContentView(dialogBinding.root)
        val window = dialog.window ?: return

        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.decorView.setPadding(0, 0, 0, 0)
        window.setGravity(Gravity.CENTER)
        window.setLayout(dpToPx(301f), dpToPx(154f))
        dialog.setCancelable(true)

        dialogBinding.tvCancel.setOnClickListener {
            dialog.dismiss()
        }
        //todo 返回到home页面后弹出 toast显示已删除
        dialogBinding.tvDelete.setOnClickListener {
            dialog.dismiss()
            viewModel.handleIntent(HistoryDetailIntent.DeleteRecord)
        }

        dialog.show()
    }
}