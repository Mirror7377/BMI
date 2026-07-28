package com.example.bmi.ui.result

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.bmi.BaseActivity
import com.example.bmi.R
import com.example.bmi.data.database.RecommendApp
import com.example.bmi.data.enums.Gender
import com.example.bmi.data.enums.HeightUnit
import com.example.bmi.data.enums.WeightUnit
import com.example.bmi.databinding.ActivityResultBinding
import com.example.bmi.databinding.DialogBmiLegendBinding
import com.example.bmi.databinding.DialogDiscardConfirmBinding
import com.example.bmi.ui.bmigauge.BmiClassifier
import com.example.bmi.ui.bmigauge.BmiConfigProvider
import com.example.bmi.ui.bmigauge.BmiLevel
import com.example.bmi.utils.UnitConverter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
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

        //获取启动当前 ResultActivity 的 Intent 中携带的附加数据包
        val bundle = intent.extras ?: Bundle()
        viewModel.initData(bundle)

        setupListeners()
        observeState()
        observeEffect()
    }

    // ========== 初始化方法 ==========

    private fun setupListeners() {
        // 返回键监听
        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        // 丢弃与保存按钮
        binding.tvDiscard.setOnClickListener { showDiscardDialog() }
        binding.tvSave.setOnClickListener { viewModel.saveRecord() }

        // 状态标签容器点击事件（显示图例弹窗）
        binding.statusContainer.setOnClickListener {
            val state = viewModel.state.value
            showBmiLegendDialog(state.bmiLevel, state.age, state.gender)
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    bindState(state)
                }
            }
        }
    }

    private fun observeEffect() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        is ResultEffect.NavigateToHome -> {
                            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                            val target = if (effect.isFirstSave) "display" else "statistics"
                            prefs.edit().putString("post_save_target", target).apply()
                            finish()
                        }
                        ResultEffect.ShowDiscardDialog -> showDiscardDialog()
                    }
                }
            }
        }
    }

    private fun bindState(state: ResultState) {
        // 1. 应用扇形配置（根据年龄性别）
        val config = BmiConfigProvider.getConfig(state.age, state.gender)
        binding.bmiGauge.applyConfig(config)

        // 2. 仪表盘 & 数字动画
        //设置数字动画
        animateBmiNumber(state.bmi)
        binding.bmiGauge.setBmi(state.bmi.toFloat())


        // ---------- 2. 状态标签（tvBmiStatus）----------
        val bmiLevel = state.bmiLevel
        binding.tvBmiStatus.text = getString(bmiLevel.statusTextRes)
        val radius = dpToPx(19.75f).toFloat()

        val hasHistory = state.hasSavedRecord
        if (hasHistory) {
            // 有历史记录 → 动态背景 + 显示图标，可点击
            val colorBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = radius
                setColor(bmiLevel.cardBgColor) // 动态颜色
            }
            binding.statusContainer.background = colorBg
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

        // 1. 体重文本（单位保留英文）
        val weightText = when (state.weightUnit) {
            WeightUnit.KG.name -> String.format("%.2f kg", state.weightInput)
            WeightUnit.LB.name -> String.format("%.2f lb", state.weightInput)
            else -> String.format("%.2f kg", state.weightInput)
        }

        // 2. 身高文本（单位保留英文）
        val heightText = when (state.heightUnit) {
            HeightUnit.CM.name -> String.format("%.1f cm", state.heightCm)
            HeightUnit.FT_IN.name -> "${state.feet} ft ${state.inches} in"
            else -> String.format("%.1f cm", state.heightCm)
        }

        // 3. 性别（从资源读取）
        val genderText = when (state.gender) {
            Gender.MALE.name -> getString(R.string.gender_male)
            Gender.FEMALE.name -> getString(R.string.gender_female)
            else -> getString(R.string.gender_male)
        }

        // 4. 年龄（从资源读取，带占位符）
        val ageText = getString(R.string.age_years_old, state.age)

        // 5. 组装完整信息
        binding.tvBmiInfo.text = getString(R.string.bmi_info_format, weightText, heightText, genderText, ageText)


        // ---------- 5. 双卡片 + 广告位切换 ----------
        binding.groupNoData.visibility = if (!hasHistory) View.VISIBLE else View.GONE
        binding.groupHasData.visibility = if (hasHistory) View.VISIBLE else View.GONE
        binding.llAdContainer.visibility = if (hasHistory) View.VISIBLE else View.GONE

        if (hasHistory) {
            val recommendedApps = state.recommendedApps
            if (recommendedApps.size == 3) {
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

        // ----------  根据用户的 BMI 等级、身高体重数据，生成一段健康建议文字，并同时填充到两个不同的卡片布局中（llBottomTip 和 llBottomTipHasData） ----------
        renderBottomTip(
            bmiLevel = bmiLevel,
            userWeightInput = state.weightInput,
            userWeightUnitStr = state.weightUnit,
            userHeightCm = state.heightCm,
            userHeightDisplayText = heightText,
            age = state.age,
            gender = state.gender,
            tvDesc = binding.tvTipDesc,
            tvMain = binding.tvTipMain,
            tvRange = binding.tvTipRange,
            tvDescHasData = binding.tvTipDescHasData,
            tvMainHasData = binding.tvTipMainHasData,
            tvRangeHasData = binding.tvTipRangeHasData
        )

        // ---------- 8. 图例高亮（仅在无历史记录时显示） ----------
        if (!hasHistory) {
            bindBmiLegend(bmiLevel,state.age)
        }
    }

    // ---------- 辅助方法 ----------
    private fun animateBmiNumber(targetBmi: Double) {
        bmiAnimator?.cancel()//取消旧动画
        //从0到目标bmi值
        bmiAnimator = ValueAnimator.ofFloat(0f, targetBmi.toFloat()).apply {
            duration = 800
            //在动画的每一帧被调用一次
            addUpdateListener { animation ->
                val current = animation.animatedValue as Float
                binding.tvBmiValue.text = String.format("%.1f", current)
            }
            start()
        }
    }


    private fun getStandardWeightRangeCm(heightCm: Double, age: Int, gender: String): Pair<Double, Double> {
        val h = heightCm / 100.0
        return if (age in 2..20) {
            val genderEnum = if (gender == Gender.MALE.name) Gender.MALE else Gender.FEMALE
            val (bmiLow, bmiHigh) = BmiClassifier.getNormalBmiRange(age, genderEnum)
            Pair(bmiLow * h * h, bmiHigh * h * h)
        } else {
            // 成人固定标准
            Pair(18.5 * h * h, 24.9 * h * h)
        }
    }

    private fun renderBottomTip(
        bmiLevel: BmiLevel,
        userWeightInput: Double,
        userWeightUnitStr: String,
        userHeightCm: Double,
        userHeightDisplayText: String,
        age: Int,
        gender: String,
        tvDesc: TextView,
        tvMain: TextView,
        tvRange: TextView,
        tvDescHasData: TextView,
        tvMainHasData: TextView,
        tvRangeHasData: TextView
    ) {
        // 获取正常体重值
        val (stdMinKg, stdMaxKg) = getStandardWeightRangeCm(userHeightCm, age, gender)
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

        // 描述文字
        listOf(tvDesc, tvDescHasData).forEach { it.text = descText; it.visibility = View.VISIBLE }

        if (bmiLevel == BmiLevel.NORMAL) {
            listOf(tvMain, tvMainHasData).forEach { it.visibility = View.GONE }
            listOf(tvRange, tvRangeHasData).forEach { it.visibility = View.GONE }
        } else {
            val mainText = getString(R.string.normal_weight_for_height, userHeightDisplayText)
            listOf(tvMain, tvMainHasData).forEach {//批量设置给有历史/无历史数据
                it.text = mainText
                it.visibility = View.VISIBLE
            }

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
            spannable.setSpan(//设置fullText为红色
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
    }

    private fun bindBmiLegend(currentLevel: BmiLevel, age: Int) {
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

        val isChild = age in 2..20
        val visibleIndices = if (isChild) listOf(2, 3, 4, 5) else (0..7).toList()

        legendLevels.forEachIndexed { index, level ->
            val layout = layouts[index]
            val shouldShow = visibleIndices.contains(index)

            if (!shouldShow) {
                layout.visibility = View.GONE
                return@forEachIndexed
            }

            layout.visibility = View.VISIBLE
            val levelColor = level.cardBgColor
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
    private fun showBmiLegendDialog(
        bmiLevel: BmiLevel,
        age: Int,
        gender: String
    ) {
        //获取视图对象
        val dialogBinding = DialogBmiLegendBinding.inflate(layoutInflater)

        // 使用自定义 Theme
        val dialog = BottomSheetDialog(
            this,
            R.style.Theme_BMI_BottomSheetDialog
        )

        //绑定视图到弹出层
        dialog.setContentView(dialogBinding.root)

        //按下系统返回键可以关闭
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        // 标题
        val isChild = age in 2..20
        // 标题
        dialogBinding.tvDialogTitle.text = if (isChild) {
            getString(R.string.bmi_for_teenagers)
        } else {
            getString(R.string.bmi_for_adults)
        }

        val genderText = when (gender) {
            Gender.MALE.name -> getString(R.string.gender_male)      // "Male"
            Gender.FEMALE.name -> getString(R.string.gender_female)  // "Female"
            else -> getString(R.string.gender_male)
        }

        if (isChild) {
            dialogBinding.tvAgeGender.visibility = View.VISIBLE
            dialogBinding.tvAgeGender.text = getString(R.string.age_gender_format, age, genderText)
        } else {
            dialogBinding.tvAgeGender.visibility = View.GONE
        }

        // Gauge
        val config = BmiConfigProvider.getConfig(age, gender)
        dialogBinding.bmiGaugeDialog.applyConfig(config)
        dialogBinding.bmiGaugeDialog.setShowPointer(false)//隐藏指针

        // 图例高亮
        applyLegendHighlight(dialogBinding, bmiLevel, age, gender)

        // 按钮
        dialogBinding.btnGotIt.setOnClickListener {
            dialog.dismiss()
        }

        //当调用 dialog.show() 让对话框出现在屏幕上时，这个 { } 大括号里的代码就会立刻执行。
        dialog.setOnShowListener {

            // 获取底部弹窗的可滑动父容器，若为空则安全退出
            val bottomSheet = dialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            ) ?: return@setOnShowListener

            //拿到控制权
            val behavior = BottomSheetBehavior.from(bottomSheet)

            //强制弹窗在出现时完全展开
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            //禁止用户把弹窗停留在半展开（折叠）状态
            behavior.skipCollapsed = true
            //允许隐藏。开启“用户可以通过向下滑动手势把弹窗完全拖出屏幕”的能力。
            behavior.isHideable = true
            //预览高度为 0
            behavior.peekHeight = 0
        }

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
        //获取对应配置
        val config = BmiConfigProvider.getConfig(age, gender)
        //分界点
        val splitPoints = config.splitPoints

        val radius = dpToPx(15f).toFloat()
        val whiteColor = 0xFFFFFFFF.toInt()
        val blackTextColor = 0xFF000000.toInt()

        val boldTypeface = resources.getFont(R.font.montserrat_extrabold)
        val regularTypeface = resources.getFont(R.font.montserrat_regular)

        // 所有行视图列表
        //
        val layouts = listOf(
            binding.layoutLevel0, binding.layoutLevel1, binding.layoutLevel2, binding.layoutLevel3,
            binding.layoutLevel4, binding.layoutLevel5, binding.layoutLevel6, binding.layoutLevel7
        )
        //圆点
        val dots = listOf(
            binding.dotLevel0, binding.dotLevel1, binding.dotLevel2, binding.dotLevel3,
            binding.dotLevel4, binding.dotLevel5, binding.dotLevel6, binding.dotLevel7
        )
        //等级名称
        val nameTvs = listOf(
            binding.tvLevelName0, binding.tvLevelName1, binding.tvLevelName2, binding.tvLevelName3,
            binding.tvLevelName4, binding.tvLevelName5, binding.tvLevelName6, binding.tvLevelName7
        )
        //bmi值
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

        // 生成bmi范围文本
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

            //返回该索引是否包含
            val shouldShow = visibleIndices.contains(index)

            if (shouldShow) {
                layout.visibility = View.VISIBLE//  如果应该显示，就把这行设为“可见”
            } else {
                layout.visibility = View.GONE//  如果应该隐藏，就把这行设为“不可见”
                return@forEachIndexed           //  并且立即跳过本轮循环剩余的代码
            }

            // 设置名称
            nameTv.text = getString(level.statusTextRes)

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
                // 成人使用原有硬编码范围
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
                    //创建彩色圆角矩形背景
                    //设置为矩形
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = radius
                    setColor(color)
                }
                layout.background = bg
                //获取左侧小圆点的背景（dot.background），强制转换为 GradientDrawable，然后将其颜色设置为白色。
                (dot.background as GradientDrawable).setColor(whiteColor)
                //字体样式
                nameTv.typeface = boldTypeface
                rangeTv.typeface = boldTypeface
                //字体颜色
                nameTv.setTextColor(whiteColor)
                rangeTv.setTextColor(whiteColor)
            } else {
                //不设置背景
                layout.background = null
                (dot.background as GradientDrawable).setColor(color)
                //字体样式
                nameTv.typeface = regularTypeface
                rangeTv.typeface = regularTypeface
                //字体颜色
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
    val dialog = Dialog(this).apply {
        setContentView(dialogBinding.root)
        window?.apply {
            //将对话框的背景设置为完全透明
            setBackgroundDrawableResource(android.R.color.transparent)
            //将对话框的显示位置设置为屏幕中央
            setGravity(Gravity.CENTER)
            //手动设置对话框的宽度为 301dp，高度为 154dp
            setLayout(dpToPx(301f), dpToPx(154f))
        }
    }

    dialogBinding.tvCancel.setOnClickListener { dialog.dismiss() }
    dialogBinding.tvDelete.setOnClickListener {
        //不进行删除，因为未保存
        dialog.dismiss()
        //  保存删除成功标志到 SharedPreferences
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("show_delete_success", true).apply()
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
            val url = "https://play.google.com/store/apps/details?id=${app.packageName}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)  // 使浏览器在新任务中打开
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        bmiAnimator?.cancel()
        bmiAnimator = null
        super.onDestroy()
    }
}