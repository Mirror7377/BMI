package com.example.bmi.ui.home

import android.animation.ArgbEvaluator
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.bmi.R
import com.example.bmi.databinding.FragmentHomeBinding
import com.example.bmi.ui.adapt.AgeAdapter
import com.example.bmi.ui.adapt.AgeItemDecoration
import com.example.bmi.ui.home.enums.Gender
import com.example.bmi.ui.home.enums.HeightUnit
import com.example.bmi.ui.home.enums.WeightUnit
import com.example.bmi.utils.DatePickerHelper
import com.example.bmi.ui.profile.ProfileActivity
import com.example.bmi.ui.result.ResultActivity
import com.example.bmi.utils.CommonBanner
import com.example.bmi.utils.TimePickerHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    //获取一个与当前 Fragment 生命周期绑定的 ViewModel 实例，并确保它在屏幕旋转等配置变更时不会被销毁重建。
    private val viewModel: HomeViewModel by viewModels()

    private lateinit var ageAdapter: AgeAdapter
    private lateinit var snapHelper: LinearSnapHelper

    companion object {
        fun newInstance() = HomeFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 点击空白处收起键盘
        binding.root.apply {
            isClickable = true
            isFocusableInTouchMode = true
            setOnClickListener { root ->
                root.clearFocus()
                //系统键盘
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(root.windowToken, 0)
            }
        }

        observeState()
        setupListeners()
        setupAgeRecyclerView()
        observeEffect()
    }

    // ---- 状态观察 ----
    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collectLatest { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: HomeState) {
        //获取当前依附的activity实例
        val context = requireContext()
        val selectedColor = ContextCompat.getColor(context, R.color.text_black)
        val unselectedColor = ContextCompat.getColor(context, R.color.bg_text)

        // 单位切换
        binding.tvUnitKg.setTextColor(if (state.weightUnit == WeightUnit.KG) selectedColor else unselectedColor)
        binding.tvUnitLb.setTextColor(if (state.weightUnit == WeightUnit.LB) selectedColor else unselectedColor)
        binding.tvUnitCm.setTextColor(if (state.heightUnit == HeightUnit.CM) selectedColor else unselectedColor)
        binding.tvUnitFtIn.setTextColor(if (state.heightUnit == HeightUnit.FT_IN) selectedColor else unselectedColor)

        // 体重单位背景偏移
        val weightMarginStart = when (state.weightUnit) {
            WeightUnit.KG -> 70
            WeightUnit.LB -> 0
        }
        val weightParams = binding.selectedUnitBg.layoutParams as ConstraintLayout.LayoutParams
        weightParams.marginStart = dpToPx(weightMarginStart)
        binding.selectedUnitBg.layoutParams = weightParams
        // 身高单位背景偏移
        val heightMarginStart = when (state.heightUnit) {
            HeightUnit.CM -> 70
            HeightUnit.FT_IN -> 0
        }
        val heightParams = binding.selectedUnitBgHeight.layoutParams as ConstraintLayout.LayoutParams
        heightParams.marginStart = dpToPx(heightMarginStart)
        binding.selectedUnitBgHeight.layoutParams = heightParams

        binding.etWeightValue.setText(String.format("%.2f", state.weightInput))
        when (state.heightUnit) {
            HeightUnit.CM -> {
                binding.heightFtInGroup.visibility = View.GONE
                binding.heightCmGroup.visibility = View.VISIBLE
                binding.etCmValue.setText(state.heightCm.toString())
            }
            HeightUnit.FT_IN -> {
                binding.heightFtInGroup.visibility = View.VISIBLE
                binding.heightCmGroup.visibility = View.GONE
                binding.etFtValue.setText(state.feetInput.toString())
                binding.etInValue.setText(state.inchesInput.toString())
            }
        }

        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        binding.tvDateDisplay.text = dateFormat.format(Date(state.timestamp))
        binding.tvTimeOfDayDisplay.setText(state.timeOfDay.displayName)
        updateGenderUI(state.gender)
    }

    // ---- 事件监听 ----
    private fun setupListeners() {
        binding.tvUnitKg.setOnClickListener {
            binding.root.clearFocus()
            viewModel.sendIntent(HomeIntent.WeightUnitChanged(WeightUnit.KG))
        }
        binding.tvUnitLb.setOnClickListener {
            binding.root.clearFocus()
            viewModel.sendIntent(HomeIntent.WeightUnitChanged(WeightUnit.LB))
        }
        binding.tvUnitCm.setOnClickListener {
            binding.root.clearFocus()
            viewModel.sendIntent(HomeIntent.HeightUnitChanged(HeightUnit.CM))
        }
        binding.tvUnitFtIn.setOnClickListener {
            binding.root.clearFocus()
            viewModel.sendIntent(HomeIntent.HeightUnitChanged(HeightUnit.FT_IN))
        }
        binding.datePickerContainer.setOnClickListener {
            binding.root.clearFocus()
            showDatePicker()
        }
        binding.timeOfDayPickerContainer.setOnClickListener {
            binding.root.clearFocus()
            showTimePicker()
        }
        binding.genderContainer1.setOnClickListener {
            binding.root.clearFocus()
            binding.genderCheck1.visibility = View.VISIBLE
            binding.genderCheck2.visibility = View.GONE
            viewModel.sendIntent(HomeIntent.GenderSelected(Gender.MALE))
        }
        binding.genderContainer2.setOnClickListener {
            binding.root.clearFocus()
            binding.genderCheck1.visibility = View.GONE
            binding.genderCheck2.visibility = View.VISIBLE
            viewModel.sendIntent(HomeIntent.GenderSelected(Gender.FEMALE))
        }

        // 输入框焦点校验
        binding.etWeightValue.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateAndFormatWeight(binding.etWeightValue, viewModel.state.value.weightUnit)
        }
        binding.etCmValue.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateAndFormatHeightCm(binding.etCmValue)
        }
        binding.etFtValue.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateAndFormatFeet(binding.etFtValue)
        }
        binding.etInValue.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateAndFormatInches(binding.etInValue)
        }

        binding.btnCalculate.setOnClickListener {
            binding.root.clearFocus()
            viewModel.sendIntent(HomeIntent.Calculate)
        }
        binding.ivPerson.setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java))
        }
    }

    // ---- 日期/时间选择器调用 ----
    private fun showDatePicker() {
        DatePickerHelper(
            context = requireContext(),
            currentTimestamp = viewModel.state.value.timestamp,
            onDateSelected = { newTimestamp ->
                viewModel.sendIntent(HomeIntent.TimeChanged(newTimestamp, viewModel.state.value.timeOfDay))
            }
        ).show()
    }

    private fun showTimePicker() {
        TimePickerHelper(
            context = requireContext(),
            currentTimeOfDay = viewModel.state.value.timeOfDay,
            onTimeSelected = { newTimeOfDay ->
                viewModel.sendIntent(
                    HomeIntent.TimeChanged(
                        viewModel.state.value.timestamp,
                        newTimeOfDay
                    )
                )
            }
        ).show()
    }

    // ---- 年龄滚轮 ----
    private fun setupAgeRecyclerView() {
        val ages = (2..99).toList()
        ageAdapter = AgeAdapter(ages) { selectedAge ->
            viewModel.sendIntent(HomeIntent.AgeChanged(selectedAge))
            scrollAgeToCenter(selectedAge)
        }
        binding.rvAgePicker.adapter = ageAdapter

        if (binding.rvAgePicker.itemDecorationCount == 0) {
            binding.rvAgePicker.addItemDecoration(AgeItemDecoration(resources.getDimensionPixelSize(R.dimen.age_item_space)))
        }
        snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(binding.rvAgePicker)

        binding.rvAgePicker.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    snapHelper.findSnapView(recyclerView.layoutManager)?.let {
                        val pos = recyclerView.getChildAdapterPosition(it)
                        if (pos != RecyclerView.NO_POSITION) viewModel.sendIntent(HomeIntent.AgeChanged(ages[pos]))
                    }
                }
            }
        })

        binding.rvAgePicker.post {
            val sidePadding = (binding.rvAgePicker.width - resources.getDimensionPixelSize(R.dimen.age_item_width)) / 2
            binding.rvAgePicker.setPadding(sidePadding, 0, sidePadding, 0)
            val index = viewModel.state.value.age - 2
            (binding.rvAgePicker.layoutManager as LinearLayoutManager).scrollToPosition(index)
            binding.rvAgePicker.post {
                snapHelper.findSnapView(binding.rvAgePicker.layoutManager)?.let { view ->
                    binding.rvAgePicker.layoutManager?.let { snapHelper.calculateDistanceToFinalSnap(it, view) }
                        ?.let {
                        binding.rvAgePicker.scrollBy(it[0], it[1])
                    }
                }
                updateAgePickerEffects()
            }
        }

        binding.rvAgePicker.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateAgePickerEffects()
            }
        })
    }

    private fun updateAgePickerEffects() {
        val recycler = binding.rvAgePicker
        if (recycler.childCount == 0) return

        val itemWidthPx = resources.getDimensionPixelSize(R.dimen.age_item_width)
        val spacePx = resources.getDimensionPixelSize(R.dimen.age_item_space)
        val unitPx = itemWidthPx + spacePx
        val maxDistance = 2.5f * unitPx
        val centerX = recycler.width / 2f
        val argbEvaluator = ArgbEvaluator()
        val startColor = 0xFF000000.toInt()
        val endColor = 0xFFBBBBBB.toInt()

        for (i in 0 until recycler.childCount) {
            val child = recycler.getChildAt(i)
            val tv = child.findViewById<TextView>(R.id.tvAgeItem) ?: continue
            val distance = abs(child.left + child.width / 2f - centerX)
            val ratio = (distance / maxDistance).coerceIn(0f, 1f)
            tv.alpha = 1f - ratio * 0.75f
            tv.setTextColor(argbEvaluator.evaluate(ratio, startColor, endColor) as Int)
        }
    }

    private fun scrollAgeToCenter(age: Int) {
        val target = age - 2
        val current = snapHelper.findSnapView(binding.rvAgePicker.layoutManager)?.let {
            binding.rvAgePicker.getChildAdapterPosition(it)
        } ?: -1
        if (current == target) return
        binding.rvAgePicker.smoothScrollToPosition(target)
        binding.rvAgePicker.post {
            snapHelper.findSnapView(binding.rvAgePicker.layoutManager)?.let { view ->
                binding.rvAgePicker.layoutManager?.let { snapHelper.calculateDistanceToFinalSnap(it, view) }
                    ?.let {
                    binding.rvAgePicker.smoothScrollBy(it[0], it[1])
                }
            }
        }
    }

    private fun validateAndFormatWeight(editText: EditText, unit: WeightUnit) {
        val raw = editText.text.toString().trim()
        val (min, max) = when (unit) {
            WeightUnit.LB -> 2.0 to 551.0
            WeightUnit.KG -> 1.0 to 250.0
        }
        val errorMsg = "Please input a valid weight (${min.toInt()}-${max.toInt()}) to calculate your BMI accurately"

        when {
            raw.isEmpty() -> {
                val default = when (unit) {
                    WeightUnit.LB -> 140.00
                    WeightUnit.KG -> 65.00
                }
                editText.setText(String.format("%.2f", default))
                CommonBanner.show(requireActivity(), R.drawable.warning, errorMsg)
                viewModel.sendIntent(HomeIntent.WeightChanged(default))
            }
            raw.toDoubleOrNull() == null -> {
                editText.setText(String.format("%.2f", viewModel.state.value.weightInput))
                CommonBanner.show(requireActivity(), R.drawable.warning, errorMsg)
            }
            else -> {
                val value = raw.toDouble()
                val clamped = value.coerceIn(min, max)
                val formatted = String.format("%.2f", clamped)
                editText.setText(formatted)
                if (value != clamped) {
                    CommonBanner.show(requireActivity(), R.drawable.warning, errorMsg)
                }
                viewModel.sendIntent(HomeIntent.WeightChanged(clamped))
            }
        }
    }

    private fun validateAndFormatHeightCm(editText: EditText) {
        val raw = editText.text.toString().trim()
        val errorMsg = getString(R.string.error_height_invalid)
        when {
            raw.isEmpty() -> {
                editText.setText("170.0")
                CommonBanner.show(requireActivity(), R.drawable.warning, errorMsg)
                viewModel.sendIntent(HomeIntent.HeightCmChanged(170.0))
            }
            else -> {
                val value = raw.toDouble()
                val clamped = value.coerceIn(1.0, 250.0)
                val formatted = String.format("%.1f", clamped)
                editText.setText(formatted)
                if (value != clamped) {
                    CommonBanner.show(requireActivity(), R.drawable.warning, errorMsg)
                }
                viewModel.sendIntent(HomeIntent.HeightCmChanged(clamped))
            }
        }
    }

    private fun validateAndFormatFeet(editText: EditText) {
        val raw = editText.text.toString().trim()
        val errorMsg = getString(R.string.error_height_ft_invalid)

        when {
            raw.isEmpty() -> {
                editText.setText("5")
                CommonBanner.show(requireActivity(), R.drawable.warning, errorMsg)
                viewModel.sendIntent(HomeIntent.FeetChanged(5))
            }
            else -> {
                val value = raw.toInt()
                val clamped = value.coerceIn(1, 8)
                editText.setText(clamped.toString())
                if (value != clamped) {
                    CommonBanner.show(requireActivity(), R.drawable.warning, errorMsg)
                }
                viewModel.sendIntent(HomeIntent.FeetChanged(clamped))
            }
        }
    }

    private fun validateAndFormatInches(editText: EditText) {
        val raw = editText.text.toString().trim()
        val feetRaw = binding.etFtValue.text.toString().trim()
        val feet = feetRaw.toIntOrNull() ?: 5
        val min = 0
        val max = if (feet >= 8) 2 else 11

        // 定义两种错误信息
        val errorMsgInches = getString(R.string.error_height_inches_invalid)
        val errorMsgFull = getString(R.string.error_height_full_invalid)

        when {
            raw.isEmpty() -> {
                editText.setText("0")
                viewModel.sendIntent(HomeIntent.InchesChanged(0))
            }
            raw.toIntOrNull() == null -> {
                editText.setText(viewModel.state.value.inchesInput.toString())
                CommonBanner.show(requireActivity(), R.drawable.warning, errorMsgInches)
            }
            else -> {
                val value = raw.toInt()
                val clamped = value.coerceIn(min, max)
                editText.setText(clamped.toString())

                if (value != clamped) {
                    val msg = if (feet == 8 && value > 2) errorMsgFull else errorMsgInches
                    CommonBanner.show(requireActivity(), R.drawable.warning, msg)
                }
                viewModel.sendIntent(HomeIntent.InchesChanged(clamped))
            }
        }
    }

    // ---- 效果观察 ----
    private fun observeEffect() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        is HomeEffect.NavigateToResult -> {
                            startActivity(Intent(requireContext(), ResultActivity::class.java).apply {
                                putExtra("BMI_RECORD", effect.record)
                            })
                        }
                    }
                }
            }
        }
    }

    // ---- 性别 UI ----
    private fun updateGenderUI(gender: Gender) {
        val selectedColor = ContextCompat.getColor(requireContext(), R.color.white)
        val unSelectedColor = ContextCompat.getColor(requireContext(), R.color.gender)
        val maleSelected = gender == Gender.MALE
        binding.genderCheck1.visibility = if (maleSelected) View.VISIBLE else View.GONE
        binding.genderCheck2.visibility = if (!maleSelected) View.VISIBLE else View.GONE
        binding.genderContainer1.setCardBackgroundColor(if (maleSelected) selectedColor else unSelectedColor)
        binding.genderContainer2.setCardBackgroundColor(if (maleSelected) unSelectedColor else selectedColor)
    }

    override fun onResume() {
        super.onResume()
        val prefs = requireContext().getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("show_delete_success", false)) {
            prefs.edit().remove("show_delete_success").apply()
            CommonBanner.show(requireActivity(), R.drawable.check_circle, "Deleted successfully.")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}