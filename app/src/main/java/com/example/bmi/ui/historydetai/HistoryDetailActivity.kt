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
import android.widget.FrameLayout
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
import com.example.bmi.ui.bmigauge.BmiLevel
import com.example.bmi.ui.historydetail.HistoryDetailEffect
import com.example.bmi.ui.historydetail.HistoryDetailIntent
import com.example.bmi.ui.historydetail.HistoryDetailState
import com.example.bmi.ui.historydetail.HistoryDetailViewModel
import com.example.bmi.ui.home.enums.Gender
import com.example.bmi.ui.home.enums.HeightUnit
import com.example.bmi.ui.home.enums.TimeOfDay
import com.example.bmi.ui.home.enums.WeightUnit
import com.example.bmi.utils.UnitConverter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
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

        val recordId = intent.getLongExtra("RECORD_ID", 0L)
        if (recordId == 0L) {
            Toast.makeText(this, "Invalid record", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewModel.handleIntent(HistoryDetailIntent.LoadRecord(recordId))

        binding.ivBack.setOnClickListener {
            viewModel.handleIntent(HistoryDetailIntent.BackPressed)
        }

        binding.tvDelete.setOnClickListener {
            showDeleteConfirmDialog()
        }

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
                        is HistoryDetailEffect.NavigateBack -> finish()
                        is HistoryDetailEffect.NavigateToHome -> {
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

        // 1. 仪表盘配置
        val config = BmiConfigProvider.getConfig(state.age, state.gender)
        binding.bmiGauge.applyConfig(config)
        binding.bmiGauge.setBmi(state.bmi.toFloat(), false)

        // 2. BMI 数值
        binding.tvBmiValue.text = String.format("%.1f", state.bmi)

        // 3. 状态标签
        val bmiLevel = state.bmiLevel
        binding.tvBmiStatus.text = getString(bmiLevel.statusTextRes)
        val radius = dpToPx(19.75f).toFloat()
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(bmiLevel.cardBgColor)
        }
        binding.statusContainer.background = bg
        binding.statusIcon.visibility = View.VISIBLE
        binding.statusContainer.isClickable = true
        binding.statusContainer.isFocusable = true

        // 4. 个人信息行（体重、身高、性别、年龄）
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
            Gender.MALE.name -> getString(R.string.gender_male)
            Gender.FEMALE.name -> getString(R.string.gender_female)
            else -> getString(R.string.gender_male)
        }

        val ageText = getString(R.string.age_years_old, state.age)
        binding.tvBmiInfo.text = getString(R.string.bmi_info_format, weightText, heightText, genderText, ageText)

        // 5. 日期 + 时段（时段需本地化）
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val dateStr = dateFormat.format(Date(state.timestamp))

        // 时段显示
        val timeOfDayEnum = try {
            TimeOfDay.valueOf(state.timeOfDay)
        } catch (e: IllegalArgumentException) {
            TimeOfDay.MORNING
        }
        val timeStr = getString(timeOfDayEnum.displayName)
        binding.tvDividerDateTime.text = "$dateStr $timeStr"

        // 6. 底部提示卡片
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

        // 7. 推荐 App
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

        // 8. 卡片圆角背景
        setTipCardRadius(binding.llBottomTipHasData)
    }

    // ---------- 辅助方法 ----------
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
        val descText = getString(bmiLevel.descTextRes)

        tvDesc.text = descText
        tvDesc.visibility = View.VISIBLE

        if (bmiLevel == BmiLevel.NORMAL) {
            tvMain.visibility = View.GONE
            tvRange.visibility = View.GONE
        } else {
            val mainText = getString(R.string.normal_weight_for_height, userHeightDisplayText)
            tvMain.text = mainText
            tvMain.visibility = View.VISIBLE

            val rangeStr = String.format("%.1f%s - %.1f%s", stdMinShow, unitStr, stdMaxShow, unitStr)
            val diffValue = if (userWeightShow < stdMinShow) {
                stdMinShow - userWeightShow
            } else {
                userWeightShow - stdMaxShow
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
            tvRange.text = spannable
            tvRange.visibility = View.VISIBLE
        }
        tvDiff.visibility = View.GONE
    }

    // ---------- 图例弹窗 ----------
    private fun showBmiLegendDialog(
        bmiLevel: BmiLevel,
        age: Int,
        gender: String
    ) {
        val dialogBinding = DialogBmiLegendBinding.inflate(layoutInflater)

        val dialog = BottomSheetDialog(
            this,
            R.style.Theme_BMI_BottomSheetDialog
        )

        dialog.setContentView(dialogBinding.root)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        val isChild = age in 2..20
        dialogBinding.tvDialogTitle.text = if (isChild) {
            getString(R.string.bmi_for_teenagers)
        } else {
            getString(R.string.bmi_for_adults)
        }

        val genderText = when (gender) {
            Gender.MALE.name -> getString(R.string.gender_male)
            Gender.FEMALE.name -> getString(R.string.gender_female)
            else -> getString(R.string.gender_male)
        }

        if (isChild) {
            dialogBinding.tvAgeGender.visibility = View.VISIBLE
            dialogBinding.tvAgeGender.text = getString(R.string.age_gender_format, age, genderText)
        } else {
            dialogBinding.tvAgeGender.visibility = View.GONE
        }

        val config = BmiConfigProvider.getConfig(age, gender)
        dialogBinding.bmiGaugeDialog.applyConfig(config)
        dialogBinding.bmiGaugeDialog.setShowPointer(false)

        applyLegendHighlight(dialogBinding, bmiLevel, age, gender)

        dialogBinding.btnGotIt.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            ) ?: return@setOnShowListener

            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
            behavior.isHideable = true
            behavior.peekHeight = 0

            behavior.addBottomSheetCallback(
                object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                            dialog.dismiss()
                        }
                    }
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {}
                }
            )
        }

        dialog.show()
    }

    private fun applyLegendHighlight(
        binding: DialogBmiLegendBinding,
        currentLevel: BmiLevel,
        age: Int,
        gender: String
    ) {
        val isChild = age in 2..20
        val config = BmiConfigProvider.getConfig(age, gender)
        val splitPoints = config.splitPoints

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

        // 儿童可见索引：2,3,4,5 (Underweight, Normal, Overweight, Obese I)
        val visibleIndices = if (isChild) listOf(2, 3, 4, 5) else (0..7).toList()

        // 儿童颜色映射
        val childColors = mapOf(
            2 to 0xFF5BB1F5.toInt(), // Underweight
            3 to 0xFFA8C526.toInt(), // Normal
            4 to 0xFFFECD2E.toInt(), // Overweight
            5 to 0xFFFD9845.toInt()  // Obese I
        )

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
            }
            layout.visibility = View.VISIBLE

            // 使用资源获取等级名称
            nameTv.text = getString(level.statusTextRes)

            val color = if (isChild) {
                childColors[index] ?: level.cardBgColor
            } else {
                level.cardBgColor
            }

            val rangeText = if (isChild) {
                getRangeText(index)
            } else {
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
            val url = "https://play.google.com/store/apps/details?id=${app.packageName}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    // ---------- 工具 ----------
    private fun dpToPx(dp: Float): Int = (dp * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        bmiAnimator?.cancel()
        bmiAnimator = null
        super.onDestroy()
    }

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

        dialogBinding.tvCancel.setOnClickListener { dialog.dismiss() }
        dialogBinding.tvDelete.setOnClickListener {
            dialog.dismiss()
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            prefs.edit().putBoolean("show_delete_success", true).apply()
            viewModel.handleIntent(HistoryDetailIntent.DeleteRecord)
        }
        dialog.show()
    }
}