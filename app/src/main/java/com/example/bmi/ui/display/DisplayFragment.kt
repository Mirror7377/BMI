package com.example.bmi.ui.display

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.bmi.R
import com.example.bmi.data.database.BmiRecord
import com.example.bmi.databinding.FragmentDisplayBinding
import com.example.bmi.ui.home.enums.Gender
import com.example.bmi.ui.home.enums.HeightUnit
import com.example.bmi.ui.home.enums.WeightUnit
import com.example.bmi.ui.recent.RecentActivity
import com.example.bmi.ui.bmigauge.BmiClassifier
import com.example.bmi.ui.bmigauge.BmiLevel
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
        fun newInstance(): DisplayFragment {
            val frag = DisplayFragment()
            return frag
        }
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

        // Recent 按钮点击事件
        binding.tvRecent.setOnClickListener {
            val intent = Intent(requireContext(), RecentActivity::class.java)
            startActivity(intent)
        }

        // 观察最新记录
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.latestRecord.collect { record ->
                    if (record != null) {
                        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                        binding.tvDate.text = dateFormat.format(record.timestamp)

                        // 显示详细内容
                        binding.scrollViewContent.visibility = View.VISIBLE

                        // 绑定详细数据
                        bindDetailData(record)
                    }
                }
            }
        }
    }

    private fun bindDetailData(record: BmiRecord) {
        // 解析 BmiLevel 和颜色
        val bmiLevel = BmiClassifier.classifyAdult(record.bmi)

        // 仪表盘
        binding.bmiGauge.setBmi(record.bmi.toFloat(),false)

        //bmi数值
        binding.tvBmiValueLarge.text = String.format("%.1f", record.bmi)


        // 状态标签
        binding.tvBmiStatus.text = bmiLevel.statusText
        val radius = dpToPx(19.75f).toFloat()
        val colorBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(bmiLevel.cardBgColor)
        }
        binding.statusContainer.background = colorBg
        binding.statusIcon.visibility = View.GONE // 不显问号图标

        // 个人信息
        val weightText = when (record.weightUnit) {
            WeightUnit.KG.name -> String.format("%.2f kg", record.weightInput)
            WeightUnit.LB.name -> String.format("%.2f lb", record.weightInput)
            else -> String.format("%.2f kg", record.weightInput)
        }

        val heightText = when (record.heightUnit) {
            HeightUnit.CM.name -> String.format("%.1f cm", record.heightInput)
            HeightUnit.FT_IN.name -> "${record.feetInput ?: 0} ft ${record.inchesInput ?: 0} in"
            else -> String.format("%.1f cm", record.heightInput)
        }

        val genderText = when (record.gender) {
            Gender.MALE.name -> "Male"
            Gender.FEMALE.name -> "Female"
            else -> "Male"
        }

        binding.tvBmiInfo.text = "$weightText | $heightText | $genderText | ${record.age} years old"

        // 图例高亮
        bindBmiLegend(bmiLevel)
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

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}