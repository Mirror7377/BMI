package com.example.bmi.ui.home

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.bmi.databinding.FragmentHomeBinding
import com.example.bmi.ui.adapt.AgeAdapter
import com.example.bmi.ui.home.enums.Gender
import com.example.bmi.ui.home.enums.HeightUnit
import com.example.bmi.ui.home.enums.TimeOfDay
import com.example.bmi.ui.home.enums.WeightUnit
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    // 从 arguments 获取是否为特殊首页（无数据）
    //控制calculate按钮的显示位置和底部导航栏是否显示
    private var isEmptyMode: Boolean = false

    private lateinit var ageAdapter: AgeAdapter
    private val placeholderCount = 2  // 首尾占位数，保证边界居中

    companion object {
        private const val ARG_IS_EMPTY = "isEmptyMode"

        fun newInstance(isEmptyMode: Boolean): HomeFragment {
            return HomeFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_IS_EMPTY, isEmptyMode)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isEmptyMode = arguments?.getBoolean(ARG_IS_EMPTY) ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 根布局设置为可点击、可获取焦点
        binding.root.apply {
            isClickable = true
            isFocusableInTouchMode = true
            setOnClickListener { view ->
                // 清除所有子 View 的焦点
                view.clearFocus()
                // 隐藏软键盘
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }

        // 根据 isEmptyMode 控制 Calculate 按钮的显隐。
        if (isEmptyMode) {
            binding.btnCalculateNoNav.visibility = View.VISIBLE
            binding.btnCalculateWithNav.visibility = View.GONE
        } else {
            binding.btnCalculateNoNav.visibility = View.GONE
            binding.btnCalculateWithNav.visibility = View.VISIBLE
        }

        // 观察状态变化
        observeState()

        // 设置事件监听
        setupListeners()

        // 初始化年龄横向滚轮
        setupAgeRecyclerView()

        // 触发初始化
        viewModel.sendIntent(HomeIntent.Init)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            // 当生命周期进入 STARTED 时启动，进入 STOPPED 时自动取消
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collectLatest { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: HomeState) {
        // 体重单位背景起始边距
        val weightMarginStart = when (state.weightUnit) {
            WeightUnit.KG -> 70   // 95 - 25 = 70
            WeightUnit.LB -> 0    // 25 - 25 = 0
        }
        //获取 selectedUnitBg 视图的布局参数
        val weightParams = binding.selectedUnitBg.layoutParams as ConstraintLayout.LayoutParams//强转
        weightParams.marginStart = dpToPx(weightMarginStart)//marginStart的属性单位为px
        binding.selectedUnitBg.layoutParams = weightParams

        // 身高单位背景起始边距
        val heightMarginStart = when (state.heightUnit) {
            HeightUnit.CM -> 70   // 95 - 25 = 70
            HeightUnit.FT_IN -> 0 // 25 - 25 = 0
        }
        val heightParams = binding.selectedUnitBgHeight.layoutParams as? ConstraintLayout.LayoutParams
        heightParams?.marginStart = dpToPx(heightMarginStart)
        binding.selectedUnitBgHeight.layoutParams = heightParams
        // 体重显示：直接使用 state.weightDisplay（由 ViewModel 格式化）
        binding.etWeightValue.setText(state.weightDisplay)


        // 身高显示
        when (state.heightUnit) {
            HeightUnit.CM -> {
                binding.heightFtInGroup.visibility = View.GONE
                binding.heightCmGroup.visibility = View.VISIBLE
                binding.etCmValue.setText(state.heightDisplay)
            }
            HeightUnit.FT_IN -> {
                binding.heightFtInGroup.visibility = View.VISIBLE
                binding.heightCmGroup.visibility = View.GONE
                // 直接使用 state.feetInput 和 state.inchesInput（未经换算的原始值）
                binding.etFtValue.setText(state.feetInput.toString())
                binding.etInValue.setText(state.inchesInput.toString())
            }
        }

        // 更新日期显示（格式：July 30, 2021）
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        binding.tvDateDisplay.text = dateFormat.format(Date(state.timestamp))

        // 更新时段显示
        binding.tvTimeOfDayDisplay.text = state.timeOfDay.displayName // 若枚举未修改，则用 toDisplayName() 扩展函数

        binding.genderCheck1.visibility = if (state.gender == Gender.MALE) View.VISIBLE else View.GONE
        binding.genderCheck2.visibility = if (state.gender == Gender.FEMALE) View.VISIBLE else View.GONE
    }

    private fun setupListeners() {
        // ---------- 体重单位切换 ----------
        binding.tvUnitKg.setOnClickListener {
            //会递归清除所有子 View 的焦点
            binding.root.clearFocus()
            viewModel.sendIntent(HomeIntent.WeightUnitChanged(WeightUnit.KG))
        }
        binding.tvUnitLb.setOnClickListener {
            binding.root.clearFocus()
            viewModel.sendIntent(HomeIntent.WeightUnitChanged(WeightUnit.LB))
        }

        // ---------- 身高单位切换 ----------
        binding.tvUnitCm.setOnClickListener {
            binding.root.clearFocus()
            viewModel.sendIntent(HomeIntent.HeightUnitChanged(HeightUnit.CM))
        }
        binding.tvUnitFtIn.setOnClickListener {
            binding.root.clearFocus()
            viewModel.sendIntent(HomeIntent.HeightUnitChanged(HeightUnit.FT_IN))
        }

        // ---------- 日期选择容器 ----------
        binding.datePickerContainer.setOnClickListener {
            showDatePicker()
        }

        // ---------- 时段选择容器 ----------
        binding.timeOfDayPickerContainer.setOnClickListener {
            showTimeOfDayPicker()
        }

        // ---------- 性别选择 ----------
        binding.genderContainer1.setOnClickListener {
            binding.genderCheck1.visibility = View.VISIBLE
            binding.genderCheck2.visibility = View.GONE
            viewModel.sendIntent(HomeIntent.GenderSelected(Gender.MALE))
        }
        binding.genderContainer2.setOnClickListener {
            binding.genderCheck1.visibility = View.GONE
            binding.genderCheck2.visibility = View.VISIBLE
            viewModel.sendIntent(HomeIntent.GenderSelected(Gender.FEMALE))
        }

        // ---------- 体重输入框焦点监听 ----------
        binding.etWeightValue.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {//失去焦点校验函数
                validateAndFormatWeight(binding.etWeightValue, viewModel.state.value.weightUnit)
            }
        }
        // ---------- 身高 cm 输入框 ----------
        binding.etCmValue.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateAndFormatHeightCm(binding.etCmValue)
            }
        }

        // ---------- ft 输入框 ----------
        binding.etFtValue.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateAndFormatFeet(binding.etFtValue)
            }
        }

        // ---------- in 输入框 ----------
        binding.etInValue.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateAndFormatInches(binding.etInValue)
            }
        }


        // ---------- Calculate 按钮 ----------
        binding.btnCalculateNoNav.setOnClickListener {
            viewModel.sendIntent(HomeIntent.Calculate)
        }
        binding.btnCalculateWithNav.setOnClickListener {
            viewModel.sendIntent(HomeIntent.Calculate)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    // dp 转 px 辅助函数
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun validateAndFormatWeight(editText: EditText, unit: WeightUnit) {
        val raw = editText.text.toString().trim()
        val (min, max) = when (unit) {
            WeightUnit.LB -> 2.0 to 551.0
            WeightUnit.KG -> 1.0 to 250.0
        }

        when {
            raw.isEmpty() -> {
                // 为空时设为默认值
                val default = when (unit) {
                    WeightUnit.LB -> 140.00
                    WeightUnit.KG -> 65.00
                }
                editText.setText(String.format("%.2f", default))
                showToast("Please input a valid weight (${String.format("%.0f", min)}-${String.format("%.0f", max)}) to calculate your BMI accurately")
                viewModel.sendIntent(HomeIntent.WeightChanged(default))
            }
            raw.toDoubleOrNull() == null -> {
                // 非法字符，重置为当前有效值（从 ViewModel 获取）
                editText.setText(viewModel.state.value.weightDisplay)
                showToast("Please input a valid weight (${String.format("%.0f", min)}-${String.format("%.0f", max)}) to calculate your BMI accurately")
            }
            else -> {
                val value = raw.toDouble()
                val clamped = when {
                    value < min -> min
                    value > max -> max
                    else -> value
                }
                // 补全小数点
                val formatted = if (raw.contains('.')) {
                    String.format("%.2f", clamped)  // 确保两位小数
                } else {
                    String.format("%.2f", clamped)  // 没输入小数点，补上 .00
                }
                editText.setText(formatted)
                if (value != clamped || raw != formatted) {
                    showToast("Please input a valid weight (${String.format("%.0f", min)}-${String.format("%.0f", max)}) to calculate your BMI accurately")
                }
                viewModel.sendIntent(HomeIntent.WeightChanged(clamped))
            }
        }
    }
    private fun validateAndFormatHeightCm(editText: EditText) {
        val raw = editText.text.toString().trim()
        val min = 1.0
        val max = 250.0

        when {
            raw.isEmpty() -> {
                editText.setText("170.0")
                showToast("Please input a valid height (${String.format("%.0f", min)}-${String.format("%.0f", max)}) to calculate your BMI accurately")
                viewModel.sendIntent(HomeIntent.HeightChanged(170.0))
            }
            raw.toDoubleOrNull() == null -> {
                editText.setText(viewModel.state.value.heightDisplay)
                showToast("Please input a valid height (${String.format("%.0f", min)}-${String.format("%.0f", max)}) to calculate your BMI accurately")
            }
            else -> {
                val value = raw.toDouble()
                val clamped = value.coerceIn(min, max)
                val formatted = if (raw.contains('.')) {
                    String.format("%.1f", clamped)  // 确保一位小数
                } else {
                    String.format("%.1f", clamped)
                }
                editText.setText(formatted)
                if (value != clamped || raw != formatted) {
                    showToast("Please input a valid height (${String.format("%.0f", min)}-${String.format("%.0f", max)}) to calculate your BMI accurately")
                }
                viewModel.sendIntent(HomeIntent.HeightChanged(clamped))
            }
        }
    }

    private fun validateAndFormatFeet(editText: EditText) {
        val raw = editText.text.toString().trim()
        val min = 1
        val max = 8

        when {
            raw.isEmpty() -> {
                editText.setText("5")
                showToast("Please input a valid height (${min}' - ${max}'2'') to calculate your BMI accurately")
                viewModel.sendIntent(HomeIntent.FeetChanged(5))
            }
            raw.toIntOrNull() == null -> {
                editText.setText(viewModel.state.value.feetInput.toString())
                showToast("Please input a valid height (${min}' - ${max}'2'') to calculate your BMI accurately")
            }
            else -> {
                val value = raw.toInt()
                val clamped = value.coerceIn(min, max)
                editText.setText(clamped.toString())
                if (value != clamped) {
                    showToast("Please input a valid height (${min}' - ${max}'2'') to calculate your BMI accurately")
                }
                viewModel.sendIntent(HomeIntent.FeetChanged(clamped))
            }
        }
    }

    private fun validateAndFormatInches(editText: EditText) {
        val raw = editText.text.toString().trim()
        val min = 0
        val max = 11

        when {
            raw.isEmpty() -> {
                editText.setText("0")  // in 为空自动变 0，不弹 Toast
                viewModel.sendIntent(HomeIntent.InchesChanged(0))
            }
            raw.toIntOrNull() == null -> {
                editText.setText(viewModel.state.value.inchesInput.toString())
                showToast("Please input a valid height (1' - 8'2'') to calculate your BMI accurately")
            }
            else -> {
                val value = raw.toInt()
                val clamped = value.coerceIn(min, max)
                editText.setText(clamped.toString())
                if (value != clamped) {
                    showToast("Please input a valid height (1' - 8'2'') to calculate your BMI accurately")
                }
                viewModel.sendIntent(HomeIntent.InchesChanged(clamped))
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun setupAgeRecyclerView() {
        val realAges = (2..99).toList()
        val placeholders = List(placeholderCount) { AgeItem.Placeholder }
        val allItems = placeholders + realAges.map { AgeItem.RealAge(it) } + placeholders

        ageAdapter = AgeAdapter(allItems) { selectedAge ->
            viewModel.sendIntent(HomeIntent.AgeChanged(selectedAge))
        }

        binding.rvAgePicker.adapter = ageAdapter

        // 系统吸附
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(binding.rvAgePicker)

        // 滚动停止时更新选中年龄
        binding.rvAgePicker.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val centerView = snapHelper.findSnapView(recyclerView.layoutManager)
                    centerView?.let {
                        val position = recyclerView.getChildAdapterPosition(it)
                        if (position != RecyclerView.NO_POSITION) {
                            val item = allItems[position]
                            if (item is AgeItem.RealAge) {
                                viewModel.sendIntent(HomeIntent.AgeChanged(item.age))
                            }
                        }
                    }
                }
            }
        })

        // 初始定位到默认年龄 25
        binding.rvAgePicker.post {
            val defaultAge = viewModel.state.value.age
            val index = defaultAge - 2 + placeholderCount   // 真实年龄索引 + 前面占位个数
            // 因为内容区宽度刚好是 item 宽度，offset 固定为 0 即可对齐
            val offset = 0
            (binding.rvAgePicker.layoutManager as LinearLayoutManager)
                .scrollToPositionWithOffset(index, offset)
        }
    }

    // 日期选择（模拟弹框，使用 DatePickerDialog）
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = viewModel.state.value.timestamp
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog =
            DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                val newCalendar = Calendar.getInstance()
                newCalendar.set(selectedYear, selectedMonth, selectedDay)
                val newTimestamp = newCalendar.timeInMillis
                // 保留原有时段，只更新日期
                viewModel.sendIntent(
                    HomeIntent.TimeChanged(
                        newTimestamp,
                        viewModel.state.value.timeOfDay
                    )
                )
            }, year, month, day)

        // 仅可选择今天及以前的日期
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    // 时段选择（简单列表弹框）
    private fun showTimeOfDayPicker() {
        val items = arrayOf("Morning", "Afternoon", "Evening", "Night")
        val current = viewModel.state.value.timeOfDay
        val checkedItem = when (current) {
            TimeOfDay.MORNING -> 0
            TimeOfDay.AFTERNOON -> 1
            TimeOfDay.EVENING -> 2
            TimeOfDay.NIGHT -> 3
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Select Time of Day")
            .setSingleChoiceItems(items, checkedItem) { dialog, which ->
                val timeOfDay = when (which) {
                    0 -> TimeOfDay.MORNING
                    1 -> TimeOfDay.AFTERNOON
                    2 -> TimeOfDay.EVENING
                    3 -> TimeOfDay.NIGHT
                    else -> TimeOfDay.MORNING
                }
                // 保留原有日期，只更新时段
                viewModel.sendIntent(HomeIntent.TimeChanged(viewModel.state.value.timestamp, timeOfDay))
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


}

