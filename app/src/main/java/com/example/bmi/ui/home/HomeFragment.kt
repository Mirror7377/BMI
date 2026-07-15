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
import com.example.bmi.R
import com.example.bmi.data.database.BmiRecord
import com.example.bmi.databinding.FragmentHomeBinding
import com.example.bmi.ui.adapt.AgeAdapter
import com.example.bmi.ui.adapt.DatePickerAdapter
import com.example.bmi.ui.adapt.DatePickerItem
import com.example.bmi.ui.adapt.DatePickerType
import com.example.bmi.ui.adapt.TimeOfDayPickerAdapter
import com.example.bmi.ui.adapt.TimePickerItem
import com.example.bmi.ui.display.DisplayFragment
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

    // ====================== 底部日期选择器成员变量 ======================
    private lateinit var monthSnapHelper: LinearSnapHelper
    private lateinit var daySnapHelper: LinearSnapHelper
    private lateinit var yearSnapHelper: LinearSnapHelper

    private val datePlaceholderCount = 3 // 首尾占位数量
    private var currentSelectedYear: Int = 0
    private var currentSelectedMonth: Int = 0
    private var currentSelectedDay: Int = 0

    // 日期上限：系统当天日期
    private val todayCalendar = Calendar.getInstance()
    private val maxYear = todayCalendar.get(Calendar.YEAR)
    private val maxMonth = todayCalendar.get(Calendar.MONTH) + 1 // 转为1-12月
    private val maxDay = todayCalendar.get(Calendar.DAY_OF_MONTH)

    // 滚轮数据列表（提升为成员变量，支持联动更新）
    private lateinit var monthItems: List<DatePickerItem>
    private lateinit var dayItems: List<DatePickerItem>
    private lateinit var yearItems: List<DatePickerItem>

    // ====================== 底部时段选择器成员变量 ======================
    private lateinit var timeSnapHelper: LinearSnapHelper
    private val timePlaceholderCount = 2
    private var currentSelectedTimeIndex: Int = 0
    private val timeOptions = listOf("Morning", "Afternoon", "Evening", "Night")

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
            //让根布局能够响应点击事件
            isClickable = true
            //允许在触摸模式下获取焦点
            isFocusableInTouchMode = true
            setOnClickListener { root ->
                // 清除所有子 View 的焦点
                root.clearFocus()//不会隐藏键盘
                // 隐藏键盘
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(root.windowToken, 0)
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

        // 初始化日期竖向滚轮
        setupDatePickers()

        // 初始化时段竖向滚轮
        setupTimePicker()

        //todo  Delete
        observeEffect()

        // 触发初始化
        viewModel.sendIntent(HomeIntent.Init)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            // 当生命周期进入 STARTED 时启动，进入 STOPPED 时自动取消
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                //如果新的状态值在旧的还没处理完时就来了，collectLatest 会取消旧的处理，直接处理最新的
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

        // 更新日期显示（格式：Jul 30, 2021 月份英文简写）
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
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
        // 读取当前英尺输入（可能还未提交，但 UI 上已有值）
        val feetRaw = binding.etFtValue.text.toString().trim()
        val feet = feetRaw.toIntOrNull() ?: 5   // 若无效则默认 5
        val min = 0
        val max = if (feet >= 8) 2 else 11      // 动态上限

        when {
            raw.isEmpty() -> {
                editText.setText("0")
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
        // 显示遮罩和弹窗，可添加底部进入动画
        binding.datePickerMask.visibility = View.VISIBLE
        binding.datePickerBottomSheet.visibility = View.VISIBLE


        // 点击遮罩/Cancel 关闭弹窗
        binding.datePickerMask.setOnClickListener { dismissDatePicker() }
        binding.btnDateCancel.setOnClickListener { dismissDatePicker() }

        // Done 按钮确认选择
        binding.btnDateDone.setOnClickListener {
            // 获取当前选中的年/月/日，更新到 ViewModel
            val selectedYear = getSelectedYear()
            val selectedMonth = getSelectedMonth()
            val selectedDay = getSelectedDay()

            val calendar = Calendar.getInstance()
            calendar.set(selectedYear, selectedMonth, selectedDay)
            viewModel.sendIntent(
                HomeIntent.TimeChanged(
                    calendar.timeInMillis,
                    viewModel.state.value.timeOfDay
                )
            )
            dismissDatePicker()
        }
    }

    private fun dismissDatePicker() {
        binding.datePickerMask.visibility = View.GONE
        binding.datePickerBottomSheet.visibility = View.GONE
    }

    // 滚轮初始化：使用 LinearSnapHelper 实现居中吸附，首尾添加占位 Item 保证边界居中
    private fun setupDatePickers() {
        // 初始化选中日期，强制限制不晚于今天
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = viewModel.state.value.timestamp
        if (calendar.after(todayCalendar)) {
            calendar.time = todayCalendar.time
        }
        currentSelectedYear = calendar.get(Calendar.YEAR)
        currentSelectedMonth = calendar.get(Calendar.MONTH) + 1
        currentSelectedDay = calendar.get(Calendar.DAY_OF_MONTH)

        // ====================== 1. 月份竖向滚轮 ======================
        monthItems = buildMonthList(currentSelectedYear)
        val monthAdapter = DatePickerAdapter(monthItems, 31, DatePickerType.MONTH_TEXT)
        binding.rvMonthPicker.adapter = monthAdapter
        binding.rvMonthPicker.layoutManager = LinearLayoutManager(requireContext())
        monthSnapHelper = LinearSnapHelper()
        monthSnapHelper.attachToRecyclerView(binding.rvMonthPicker)

        binding.rvMonthPicker.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val centerView = monthSnapHelper.findSnapView(recyclerView.layoutManager) ?: return
                    val position = recyclerView.getChildAdapterPosition(centerView)
                    if (position != RecyclerView.NO_POSITION) {
                        val item = monthItems[position]
                        if (item is DatePickerItem.RealValue) {
                            currentSelectedMonth = item.value
                            updateDayList() // 月份变化联动更新日期
                        }
                    }
                }
            }
        })

        // ====================== 2. 日期竖向滚轮 ======================
        dayItems = buildDayList(currentSelectedYear, currentSelectedMonth)
        val dayAdapter = DatePickerAdapter(dayItems, 14)
        binding.rvDayPicker.adapter = dayAdapter
        binding.rvDayPicker.layoutManager = LinearLayoutManager(requireContext())
        daySnapHelper = LinearSnapHelper()
        daySnapHelper.attachToRecyclerView(binding.rvDayPicker)

        binding.rvDayPicker.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val centerView = daySnapHelper.findSnapView(recyclerView.layoutManager) ?: return
                    val position = recyclerView.getChildAdapterPosition(centerView)
                    if (position != RecyclerView.NO_POSITION) {
                        val item = dayItems[position]
                        if (item is DatePickerItem.RealValue) {
                            currentSelectedDay = item.value
                        }
                    }
                }
            }
        })

        // ====================== 3. 年份竖向滚轮 ======================
        yearItems = buildYearList()
        val yearAdapter = DatePickerAdapter(yearItems, 32)
        binding.rvYearPicker.adapter = yearAdapter
        binding.rvYearPicker.layoutManager = LinearLayoutManager(requireContext())
        yearSnapHelper = LinearSnapHelper()
        yearSnapHelper.attachToRecyclerView(binding.rvYearPicker)

        binding.rvYearPicker.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val centerView = yearSnapHelper.findSnapView(recyclerView.layoutManager) ?: return
                    val position = recyclerView.getChildAdapterPosition(centerView)
                    if (position != RecyclerView.NO_POSITION) {
                        val item = yearItems[position]
                        if (item is DatePickerItem.RealValue) {
                            val newYear = item.value
                            if (newYear != currentSelectedYear) {
                                currentSelectedYear = newYear
                                updateMonthList() // 年份变化联动更新月份
                            }
                        }
                    }
                }
            }
        })

        scrollDateToCurrent()
    }

    private fun showTimeOfDayPicker() {
        // 打开弹窗时同步当前选中状态
        currentSelectedTimeIndex = when (viewModel.state.value.timeOfDay) {
            TimeOfDay.MORNING -> 0
            TimeOfDay.AFTERNOON -> 1
            TimeOfDay.EVENING -> 2
            TimeOfDay.NIGHT -> 3
        }
        scrollTimeToCurrent()

        binding.timePickerMask.visibility = View.VISIBLE
        binding.timePickerBottomSheet.visibility = View.VISIBLE

        binding.timePickerMask.setOnClickListener { dismissTimePicker() }
        binding.btnTimeCancel.setOnClickListener { dismissTimePicker() }

        binding.btnTimeDone.setOnClickListener {
            val timeOfDay = when (currentSelectedTimeIndex) {
                0 -> TimeOfDay.MORNING
                1 -> TimeOfDay.AFTERNOON
                2 -> TimeOfDay.EVENING
                3 -> TimeOfDay.NIGHT
                else -> TimeOfDay.MORNING
            }
            viewModel.sendIntent(HomeIntent.TimeChanged(viewModel.state.value.timestamp, timeOfDay))
            dismissTimePicker()
        }
    }

    private fun dismissTimePicker() {
        binding.timePickerMask.visibility = View.GONE
        binding.timePickerBottomSheet.visibility = View.GONE
    }

    private fun observeEffect() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        is HomeEffect.NavigateToResult -> {
                            // 把完整记录的原始字段全部传给详情页
                            navigateToDisplay(effect.record)
                        }
                        is HomeEffect.ShowError -> {
                            showToast(effect.message)
                        }
                    }
                }
            }
        }
    }

    private fun navigateToDisplay(record: BmiRecord) {
        val displayFragment = DisplayFragment().apply {
            arguments = Bundle().apply {
                // BMI核心值
                putDouble("KEY_BMI", record.bmi)
                // 体重原值 + 单位
                putDouble("KEY_WEIGHT_INPUT", record.weightInput)
                putString("KEY_WEIGHT_UNIT", record.weightUnit)
                // 身高原值 + 单位 + 英尺英寸
                putDouble("KEY_HEIGHT_INPUT", record.heightInput)
                putString("KEY_HEIGHT_UNIT", record.heightUnit)
                putInt("KEY_FEET", record.feetInput ?: 0)
                putInt("KEY_INCHES", record.inchesInput ?: 0)
                // 年龄、性别
                putInt("KEY_AGE", record.age)
                putString("KEY_GENDER", record.gender)
                // 标记：这是从计算页跳转来的
                putBoolean("KEY_FROM_CALCULATE", true)
            }
        }

        //跳转fragment
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, displayFragment)
            .addToBackStack(null)//把 HomeFragment 存入返回栈
            .commit()
    }


    // 构建月份列表（1-12月）
    private fun buildMonthList(): List<DatePickerItem> {
        val realData = (1..12).map { DatePickerItem.RealValue(it) }
        val placeholders = List(datePlaceholderCount) { DatePickerItem.Placeholder }
        return placeholders + realData + placeholders
    }

    // 构建年份列表（范围：1900年 ~ 今年）
    private fun buildYearList(): List<DatePickerItem> {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val realData = (1900..currentYear).map { DatePickerItem.RealValue(it) }
        val placeholders = List(datePlaceholderCount) { DatePickerItem.Placeholder }
        return placeholders + realData + placeholders
    }


    // 获取指定年月的总天数
    private fun getDaysInMonth(year: Int, month: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1)
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }


    // 将三个滚轮滚动到当前选中的日期
    private fun scrollDateToCurrent() {
        // 月份定位
        val monthPos = currentSelectedMonth - 1 + datePlaceholderCount
        (binding.rvMonthPicker.layoutManager as LinearLayoutManager)
            .scrollToPositionWithOffset(monthPos, 0)

        // 年份定位
        val yearPos = currentSelectedYear - 1900 + datePlaceholderCount
        (binding.rvYearPicker.layoutManager as LinearLayoutManager)
            .scrollToPositionWithOffset(yearPos, 0)

        // 日期定位
        val dayPos = currentSelectedDay - 1 + datePlaceholderCount
        (binding.rvDayPicker.layoutManager as LinearLayoutManager)
            .scrollToPositionWithOffset(dayPos, 0)
    }

    private fun getSelectedYear(): Int = currentSelectedYear
    private fun getSelectedMonth(): Int = currentSelectedMonth
    private fun getSelectedDay(): Int = currentSelectedDay

    // 构建月份列表：今年则截止到当月，其他年份到12月
    private fun buildMonthList(year: Int): List<DatePickerItem> {
        val maxMonthOfYear = if (year == maxYear) maxMonth else 12
        val realData = (1..maxMonthOfYear).map { DatePickerItem.RealValue(it) }
        val placeholders = List(datePlaceholderCount) { DatePickerItem.Placeholder }
        return placeholders + realData + placeholders
    }

    // 构建日期列表：今年当月则截止到今天，其他情况取当月总天数
    private fun buildDayList(year: Int, month: Int): List<DatePickerItem> {
        val maxDayOfMonth = if (year == maxYear && month == maxMonth) {
            maxDay
        } else {
            getDaysInMonth(year, month)
        }
        val realData = (1..maxDayOfMonth).map { DatePickerItem.RealValue(it) }
        val placeholders = List(datePlaceholderCount) { DatePickerItem.Placeholder }
        return placeholders + realData + placeholders
    }


    // 年份变化时更新月份列表，并自动修正超出范围的月份
    private fun updateMonthList() {
        val newMonthItems = buildMonthList(currentSelectedYear)
        monthItems = newMonthItems

        // 自动修正选中月份
        val maxMonthOfYear = if (currentSelectedYear == maxYear) maxMonth else 12
        if (currentSelectedMonth > maxMonthOfYear) {
            currentSelectedMonth = maxMonthOfYear
        }

        (binding.rvMonthPicker.adapter as? DatePickerAdapter)?.updateData(newMonthItems)
        val targetPosition = currentSelectedMonth - 1 + datePlaceholderCount
        (binding.rvMonthPicker.layoutManager as LinearLayoutManager)
            .scrollToPositionWithOffset(targetPosition, 0)

        updateDayList()
    }

    // 月份/年份变化时更新日期列表，自动修正超出范围的日期
    private fun updateDayList() {
        val maxDayOfMonth = if (currentSelectedYear == maxYear && currentSelectedMonth == maxMonth) {
            maxDay
        } else {
            getDaysInMonth(currentSelectedYear, currentSelectedMonth)
        }

        if (currentSelectedDay > maxDayOfMonth) {
            currentSelectedDay = maxDayOfMonth
        }
        val newDayItems = buildDayList(currentSelectedYear, currentSelectedMonth)
        dayItems = newDayItems

        (binding.rvDayPicker.adapter as? DatePickerAdapter)?.updateData(newDayItems)
        val targetPosition = currentSelectedDay - 1 + datePlaceholderCount
        (binding.rvDayPicker.layoutManager as LinearLayoutManager)
            .scrollToPositionWithOffset(targetPosition, 0)
    }

    private fun setupTimePicker() {
        // 初始化当前选中时段
        currentSelectedTimeIndex = when (viewModel.state.value.timeOfDay) {
            TimeOfDay.MORNING -> 0
            TimeOfDay.AFTERNOON -> 1
            TimeOfDay.EVENING -> 2
            TimeOfDay.NIGHT -> 3
        }

        val timeItems = buildTimeList()
        val timeAdapter = TimeOfDayPickerAdapter(timeItems)
        binding.rvTimePicker.adapter = timeAdapter
        binding.rvTimePicker.layoutManager = LinearLayoutManager(requireContext())

        timeSnapHelper = LinearSnapHelper()
        timeSnapHelper.attachToRecyclerView(binding.rvTimePicker)

        binding.rvTimePicker.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val centerView = timeSnapHelper.findSnapView(recyclerView.layoutManager) ?: return
                    val position = recyclerView.getChildAdapterPosition(centerView)
                    if (position != RecyclerView.NO_POSITION) {
                        val item = timeItems[position]
                        if (item is TimePickerItem.RealValue) {
                            currentSelectedTimeIndex = timeOptions.indexOf(item.text)
                        }
                    }
                }
            }
        })

        scrollTimeToCurrent()
    }

    private fun buildTimeList(): List<TimePickerItem> {
        val realData = timeOptions.map { TimePickerItem.RealValue(it) }
        val placeholders = List(timePlaceholderCount) { TimePickerItem.Placeholder }
        return placeholders + realData + placeholders
    }

    private fun scrollTimeToCurrent() {
        val targetPosition = currentSelectedTimeIndex + timePlaceholderCount
        (binding.rvTimePicker.layoutManager as LinearLayoutManager)
            .scrollToPositionWithOffset(targetPosition, 0)
    }
}

