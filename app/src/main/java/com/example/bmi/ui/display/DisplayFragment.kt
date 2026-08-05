package com.example.bmi.ui.display

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.bmi.MainActivity
import com.example.bmi.R
import com.example.bmi.data.database.BmiRecord
import com.example.bmi.databinding.FragmentDisplayBinding
import com.example.bmi.data.enums.Gender
import com.example.bmi.data.enums.HeightUnit
import com.example.bmi.data.enums.WeightUnit
import com.example.bmi.ui.recent.RecentActivity
import com.example.bmi.ui.bmigauge.BmiClassifier
import com.example.bmi.ui.bmigauge.BmiConfigProvider
import com.example.bmi.ui.bmigauge.BmiLevel
import com.example.bmi.ui.recent.RecentIntent
import com.example.bmi.utils.CommonBanner
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class DisplayFragment : Fragment() {

    private var _binding: FragmentDisplayBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DisplayViewModel by viewModels()

    companion object {
        fun newInstance(): DisplayFragment = DisplayFragment()
    }

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDisplayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        setupObservers()
        viewModel.handleIntent(DisplayIntent.LoadLatest)
    }

    // ========== 设置监听器 ==========
    private fun setupListeners() {
        binding.tvRecent.setOnClickListener {
            viewModel.handleIntent(DisplayIntent.NavigateTo(DisplayIntent.Destination.RECENT))
        }
    }

    // ========== 统一观察状态和副作用 ==========
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 收集状态（StateFlow）
                launch {
                    viewModel.state.collect { state ->
                        renderState(state)
                    }
                }
                // 收集副作用（SharedFlow）
                launch {
                    viewModel.effect.collect { effect ->
                        when (effect) {
                            is DisplayEffect.NavigateTo -> handleNavigation(effect)
                        }
                    }
                }
            }
        }
    }

    // ========== 导航处理 ==========
    private fun handleNavigation(effect: DisplayEffect.NavigateTo) {
        when (effect.destination) {
            DisplayIntent.Destination.HOME -> {
                (requireActivity() as? MainActivity)?.goToHome()
            }
            DisplayIntent.Destination.RECENT -> {
                startActivity(Intent(requireContext(), RecentActivity::class.java))
            }
        }
    }

    // ========== 渲染状态 ==========
    private fun renderState(state: DisplayState) {
        val record = state.record ?: return
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        //左上角日期
        binding.tvDate.text = dateFormat.format(record.timestamp)

        bindDetailData(record)
    }

    // ========== 绑定数据详情 ==========
    private fun bindDetailData(record: BmiRecord) {
        val age = record.age
        val gender = when (record.gender) {
            Gender.MALE.name -> Gender.MALE
            Gender.FEMALE.name -> Gender.FEMALE
            else -> Gender.MALE
        }

        val bmiLevel = if (age <= 20) {
            BmiConfigProvider.classifyChild(age, gender.name, record.bmi)
        } else {
            BmiClassifier.classifyAdult(record.bmi)
        }

        val config = BmiConfigProvider.getConfig(age, gender.name)
        binding.bmiGauge.applyConfig(config)
        binding.bmiGauge.setBmi(record.bmi.toFloat(), false)

        binding.tvBmiValueLarge.text = String.format("%.1f", record.bmi)

        binding.tvBmiStatus.text = getString(bmiLevel.statusTextRes)
        val radius = dpToPx(19.75f).toFloat()
        val colorBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(bmiLevel.cardBgColor)
        }
        binding.statusContainer.background = colorBg
        binding.statusIcon.visibility = View.GONE

        val weightText = when (record.weightUnit) {
            WeightUnit.KG.name -> String.format("%.2f kg", record.weightInput)
            WeightUnit.LB.name -> String.format("%.2f lb", record.weightInput)
            else -> String.format("%.2f kg", record.weightInput)
        }

        val heightText = when (record.heightUnit) {
            HeightUnit.CM.name -> String.format("%.1f cm", record.heightCm)
            HeightUnit.FT_IN.name -> "${record.feetInput ?: 0} ft ${record.inchesInput ?: 0} in"
            else -> String.format("%.1f cm", record.heightCm)
        }

        val genderText = when (record.gender) {
            Gender.MALE.name -> getString(R.string.gender_male)
            Gender.FEMALE.name -> getString(R.string.gender_female)
            else -> getString(R.string.gender_male)
        }

        val ageText = getString(R.string.age_years_old, age)

        binding.tvBmiInfo.text = getString(
            R.string.bmi_info_format,
            weightText,
            heightText,
            genderText,
            ageText
        )

        bindBmiLegend(bmiLevel, age, gender.name)
    }

    // ========== 图例绑定 ==========
    private fun bindBmiLegend(currentLevel: BmiLevel, age: Int, gender: String) {
        val isChild = age <= 20
        val config = BmiConfigProvider.getConfig(age, gender)
        val splitPoints = config.splitPoints
        val colors = config.colors

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

        val visibleIndices = if (isChild) listOf(2, 3, 4, 5) else (0..7).toList()

        fun getLevelColor(index: Int): Int {
            return if (isChild) {
                when (index) {
                    2 -> BmiLevel.UNDERWEIGHT.cardBgColor
                    3 -> BmiLevel.NORMAL.cardBgColor
                    4 -> BmiLevel.OVERWEIGHT.cardBgColor
                    5 -> BmiLevel.OBESE_CLASS_I.cardBgColor
                    //getOrElse当索引越界时，不会抛出异常，而是执行一个 lambda 函数，返回一个“兜底值”。
                    else -> colors.getOrElse(index) { 0xFF000000.toInt() }
                }
            } else {
                //返回 colors 列表里对应位置的那个颜色,无效索引为0xFF000000
                colors.getOrElse(index) { 0xFF000000.toInt() }
            }
        }

        fun getRangeText(index: Int): String {
            if (isChild && splitPoints.size >= 3) {
                val u = splitPoints[0]
                val n = splitPoints[1]
                val o = splitPoints[2]
                return when (index) {
                    2 -> "＜${u}"
                    3 -> "${u} - ${n}"
                    4 -> "${n} - ${o}"
                    5 -> "≥${o}"
                    else -> ""
                }
            }
            return when (index) {
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

            nameTv.text = getString(level.statusTextRes)
            rangeTv.text = getRangeText(index)

            val color = getLevelColor(index)
            val isHighlighted = (level == currentLevel)

            if (isHighlighted) {
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
        }
    }

    // ========== 工具方法 ==========
    private fun dpToPx(dp: Float): Int = (dp * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}