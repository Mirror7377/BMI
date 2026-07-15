package com.example.bmi.ui.display

import android.animation.ValueAnimator
import android.app.Dialog
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.bmi.BottomNavController
import com.example.bmi.MainActivity
import com.example.bmi.R
import com.example.bmi.databinding.DialogDiscardConfirmBinding
import com.example.bmi.databinding.FragmentDisplayBinding
import com.example.bmi.ui.home.enums.Gender
import com.example.bmi.ui.home.enums.HeightUnit
import com.example.bmi.ui.home.enums.WeightUnit
import com.example.bmi.utils.BmiClassifier
import com.example.bmi.utils.BmiLevel
import com.example.bmi.utils.UnitConverter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DisplayFragment : Fragment() {
    private var _binding: FragmentDisplayBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DisplayViewModel by viewModels()

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

    // 拦截系统返回键
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            viewModel.handleIntent(DisplayIntent.BackPressed)
        }
    }

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

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, backPressedCallback
        )

        // 如果是从计算页跳转，需要把 arguments 传递给 ViewModel（实际上已经在 init 中通过 SavedStateHandle 获取了）
        // 但为了保险，也可以再次发送 Intent，但非必须

        binding.tvDiscard.setOnClickListener {
            viewModel.handleIntent(DisplayIntent.Discard)
        }
        binding.tvSave.setOnClickListener {
            viewModel.handleIntent(DisplayIntent.SaveRecord)
        }

        // 观察 State
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    bindState(state)
                }
            }
        }

        // 观察 Effect
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        is DisplayEffect.NavigateToHome -> navigateToHome()
                        is DisplayEffect.ShowDiscardDialog -> showDiscardDialog()
                    }
                }
            }
        }
    }

    private fun bindState(state: DisplayState) {
        animateBmiNumber(state.bmi)
        binding.bmiGauge.setBmi(state.bmi.toFloat())

        val bmiLevel = state.bmiLevel
        bindStatusTag(bmiLevel)
        bindBmiLegend(bmiLevel)

        val weightText = when (state.weightUnit) {
            WeightUnit.KG.name -> String.format("%.2f kg", state.weightInput)
            WeightUnit.LB.name -> String.format("%.2f lb", state.weightInput)
            else -> String.format("%.2f kg", state.weightInput)
        }

        val heightText = when (state.heightUnit) {
            HeightUnit.CM.name -> String.format("%.1f cm", state.heightInput)
            HeightUnit.FT_IN.name -> "${state.feet} ft-${state.inches} in"
            else -> String.format("%.1f cm", state.heightInput)
        }

        val genderText = when (state.gender) {
            Gender.MALE.name -> "Male"
            Gender.FEMALE.name -> "Female"
            else -> "Male"
        }

        binding.tvBmiInfo.text = "$weightText | $heightText | $genderText | ${state.age} years old"

        setTipCardRadius()
        renderBottomTip(
            bmiLevel = bmiLevel,
            userWeightInput = state.weightInput,
            userWeightUnitStr = state.weightUnit,
            userHeightCm = state.heightCm,
            userHeightDisplayText = heightText
        )
    }

    private fun animateBmiNumber(targetBmi: Double) {
        bmiAnimator?.cancel()
        bmiAnimator = ValueAnimator.ofFloat(0f, targetBmi.toFloat()).apply {
            duration = 500
            addUpdateListener { animation ->
                val current = animation.animatedValue as Float
                binding.tvBmiValue.text = String.format("%.1f", current)
            }
            start()
        }
    }

    private fun bindStatusTag(level: BmiLevel) {
        binding.tvBmiStatus.text = level.statusText
        val radius = dpToPx(19.75f).toFloat()
        val statusBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(level.cardBgColor)
        }
        binding.tvBmiStatus.background = statusBg
    }

    private fun setTipCardRadius() {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(15f).toFloat()
            setColor(0xFFF4F4F4.toInt())
        }
        binding.llBottomTip.background = drawable
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
        userHeightDisplayText: String
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
        binding.tvTipDesc.text = bmiLevel.descText
        binding.tvTipDesc.visibility = View.VISIBLE

        if (bmiLevel == BmiLevel.NORMAL) {
            binding.tvTipMain.visibility = View.GONE
            binding.tvTipRange.visibility = View.GONE
        } else {
            binding.tvTipMain.visibility = View.VISIBLE
            binding.tvTipRange.visibility = View.VISIBLE
            binding.tvTipMain.text = "Normal weight for your height($userHeightDisplayText)"

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
            binding.tvTipRange.text = spannable
        }
        binding.tvTipDiff.visibility = View.GONE
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

    private fun showDiscardDialog() {
        val dialogBinding = DialogDiscardConfirmBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext())
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
            navigateToHome()
        }

        dialog.show()
    }

    private fun navigateToHome() {
        (requireActivity() as MainActivity).navigateToHome(isEmptyMode = false)
    }

    override fun onStart() {
        super.onStart()
        (requireActivity() as? BottomNavController)?.forceHideBottomNav()
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as? BottomNavController)?.forceHideBottomNav()
    }

    override fun onDestroyView() {
        (requireActivity() as? BottomNavController)?.restoreBottomNavAuto()
        bmiAnimator?.cancel()
        bmiAnimator = null
        super.onDestroyView()
        _binding = null
    }
}