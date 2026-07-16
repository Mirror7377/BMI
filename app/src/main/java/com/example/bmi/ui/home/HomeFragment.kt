package com.example.bmi.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.bmi.data.database.BmiRecord
import com.example.bmi.databinding.FragmentHomeBinding
import com.example.bmi.ui.adapt.AgeAdapter
import com.example.bmi.ui.adapt.DatePickerAdapter
import com.example.bmi.ui.adapt.DatePickerItem
import com.example.bmi.ui.adapt.DatePickerType
import com.example.bmi.ui.adapt.TimeOfDayPickerAdapter
import com.example.bmi.ui.adapt.TimePickerItem
import com.example.bmi.ui.home.enums.Gender
import com.example.bmi.ui.home.enums.HeightUnit
import com.example.bmi.ui.home.enums.TimeOfDay
import com.example.bmi.ui.home.enums.WeightUnit
import com.example.bmi.ui.result.ResultActivity
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

    // 控制计算按钮显示位置，由 MainActivity 动态更新
    private var isEmptyMode: Boolean = false

    private lateinit var ageAdapter: AgeAdapter
    private val placeholderCount = 2

    // 日期滚轮成员变量
    private lateinit var monthSnapHelper: LinearSnapHelper
    private lateinit var daySnapHelper: LinearSnapHelper
    private lateinit var yearSnapHelper: LinearSnapHelper
    private val datePlaceholderCount = 3
    private var currentSelectedYear: Int = 0
    private var currentSelectedMonth: Int = 0
    private var currentSelectedDay: Int = 0
    private val todayCalendar = Calendar.getInstance()
    private val maxYear = todayCalendar.get(Calendar.YEAR)
    private val maxMonth = todayCalendar.get(Calendar.MONTH) + 1
    private val maxDay = todayCalendar.get(Calendar.DAY_OF_MONTH)
    private lateinit var monthItems: List<DatePickerItem>
    private lateinit var dayItems: List<DatePickerItem>
    private lateinit var yearItems: List<DatePickerItem>

    // 时段滚轮
    private lateinit var timeSnapHelper: LinearSnapHelper
    private val timePlaceholderCount = 2
    private var currentSelectedTimeIndex: Int = 0
    private val timeOptions = listOf("Morning", "Afternoon", "Evening", "Night")

    companion object {
        fun newInstance(): HomeFragment {
            return HomeFragment().apply {
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 默认为 false 不显示
        isEmptyMode = false
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

        // 根布局点击隐藏键盘
        binding.root.apply {
            isClickable = true
            isFocusableInTouchMode = true
            setOnClickListener { root ->
                root.clearFocus()
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(root.windowToken, 0)
            }
        }

        // 根据当前 isEmptyMode 更新按钮
        updateButtonVisibility()

        observeState()
        setupListeners()
        setupAgeRecyclerView()
        setupDatePickers()
        setupTimePicker()
        observeEffect()

        viewModel.sendIntent(HomeIntent.Init)
    }

    /**
     * 供 MainActivity 调用，动态更新空状态
     */
    fun setEmptyMode(isEmpty: Boolean) {
        if (this.isEmptyMode != isEmpty) {
            this.isEmptyMode = isEmpty
            if (_binding != null) {
                updateButtonVisibility()
            }
        }
    }

    /**
     * 根据 isEmptyMode 切换按钮显隐
     */
    private fun updateButtonVisibility() {
        if (isEmptyMode) {
            binding.btnCalculateNoNav.visibility = View.VISIBLE
            binding.btnCalculateWithNav.visibility = View.GONE
        } else {
            binding.btnCalculateNoNav.visibility = View.GONE
            binding.btnCalculateWithNav.visibility = View.VISIBLE
        }
    }

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
        val heightParams = binding.selectedUnitBgHeight.layoutParams as? ConstraintLayout.LayoutParams
        heightParams?.marginStart = dpToPx(heightMarginStart)
        binding.selectedUnitBgHeight.layoutParams = heightParams

        binding.etWeightValue.setText(state.weightDisplay)

        when (state.heightUnit) {
            HeightUnit.CM -> {
                binding.heightFtInGroup.visibility = View.GONE
                binding.heightCmGroup.visibility = View.VISIBLE
                binding.etCmValue.setText(state.heightDisplay)
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
        binding.tvTimeOfDayDisplay.text = state.timeOfDay.displayName

        binding.genderCheck1.visibility = if (state.gender == Gender.MALE) View.VISIBLE else View.GONE
        binding.genderCheck2.visibility = if (state.gender == Gender.FEMALE) View.VISIBLE else View.GONE
    }

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
            showDatePicker()
        }
        binding.timeOfDayPickerContainer.setOnClickListener {
            showTimeOfDayPicker()
        }
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

        binding.etWeightValue.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateAndFormatWeight(binding.etWeightValue, viewModel.state.value.weightUnit)
            }
        }
        binding.etCmValue.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateAndFormatHeightCm(binding.etCmValue)
            }
        }
        binding.etFtValue.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateAndFormatFeet(binding.etFtValue)
            }
        }
        binding.etInValue.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateAndFormatInches(binding.etInValue)
            }
        }

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
                val default = when (unit) {
                    WeightUnit.LB -> 140.00
                    WeightUnit.KG -> 65.00
                }
                editText.setText(String.format("%.2f", default))
                showToast("Please input a valid weight (${String.format("%.0f", min)}-${String.format("%.0f", max)}) to calculate your BMI accurately")
                viewModel.sendIntent(HomeIntent.WeightChanged(default))
            }
            raw.toDoubleOrNull() == null -> {
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
                val formatted = if (raw.contains('.')) {
                    String.format("%.2f", clamped)
                } else {
                    String.format("%.2f", clamped)
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
                    String.format("%.1f", clamped)
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
        val feetRaw = binding.etFtValue.text.toString().trim()
        val feet = feetRaw.toIntOrNull() ?: 5
        val min = 0
        val max = if (feet >= 8) 2 else 11
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
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(binding.rvAgePicker)
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
        binding.rvAgePicker.post {
            val defaultAge = viewModel.state.value.age
            val index = defaultAge - 2 + placeholderCount
            val offset = 0
            (binding.rvAgePicker.layoutManager as LinearLayoutManager)
                .scrollToPositionWithOffset(index, offset)
        }
    }

    private fun showDatePicker() {
        binding.datePickerMask.visibility = View.VISIBLE
        binding.datePickerBottomSheet.visibility = View.VISIBLE
        binding.datePickerMask.setOnClickListener { dismissDatePicker() }
        binding.btnDateCancel.setOnClickListener { dismissDatePicker() }
        binding.btnDateDone.setOnClickListener {
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

    private fun setupDatePickers() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = viewModel.state.value.timestamp
        if (calendar.after(todayCalendar)) {
            calendar.time = todayCalendar.time
        }
        currentSelectedYear = calendar.get(Calendar.YEAR)
        currentSelectedMonth = calendar.get(Calendar.MONTH) + 1
        currentSelectedDay = calendar.get(Calendar.DAY_OF_MONTH)

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
                            updateDayList()
                        }
                    }
                }
            }
        })

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
                                updateMonthList()
                            }
                        }
                    }
                }
            }
        })
        scrollDateToCurrent()
    }

    private fun showTimeOfDayPicker() {
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
        val bundle = Bundle().apply {
            putDouble("KEY_BMI", record.bmi)
            putDouble("KEY_WEIGHT_INPUT", record.weightInput)
            putString("KEY_WEIGHT_UNIT", record.weightUnit)
            putDouble("KEY_HEIGHT_INPUT", record.heightInput)
            putString("KEY_HEIGHT_UNIT", record.heightUnit)
            putInt("KEY_FEET", record.feetInput ?: 0)
            putInt("KEY_INCHES", record.inchesInput ?: 0)
            putInt("KEY_AGE", record.age)
            putString("KEY_GENDER", record.gender)
            putDouble("KEY_HEIGHT_CM", record.heightCm)
        }
        val intent = Intent(requireContext(), ResultActivity::class.java).apply {
            putExtras(bundle)
        }
        startActivity(intent)
    }

    private fun buildMonthList(): List<DatePickerItem> {
        val realData = (1..12).map { DatePickerItem.RealValue(it) }
        val placeholders = List(datePlaceholderCount) { DatePickerItem.Placeholder }
        return placeholders + realData + placeholders
    }

    private fun buildYearList(): List<DatePickerItem> {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val realData = (1900..currentYear).map { DatePickerItem.RealValue(it) }
        val placeholders = List(datePlaceholderCount) { DatePickerItem.Placeholder }
        return placeholders + realData + placeholders
    }

    private fun getDaysInMonth(year: Int, month: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1)
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    private fun scrollDateToCurrent() {
        val monthPos = currentSelectedMonth - 1 + datePlaceholderCount
        (binding.rvMonthPicker.layoutManager as LinearLayoutManager)
            .scrollToPositionWithOffset(monthPos, 0)
        val yearPos = currentSelectedYear - 1900 + datePlaceholderCount
        (binding.rvYearPicker.layoutManager as LinearLayoutManager)
            .scrollToPositionWithOffset(yearPos, 0)
        val dayPos = currentSelectedDay - 1 + datePlaceholderCount
        (binding.rvDayPicker.layoutManager as LinearLayoutManager)
            .scrollToPositionWithOffset(dayPos, 0)
    }

    private fun getSelectedYear(): Int = currentSelectedYear
    private fun getSelectedMonth(): Int = currentSelectedMonth
    private fun getSelectedDay(): Int = currentSelectedDay

    private fun buildMonthList(year: Int): List<DatePickerItem> {
        val maxMonthOfYear = if (year == maxYear) maxMonth else 12
        val realData = (1..maxMonthOfYear).map { DatePickerItem.RealValue(it) }
        val placeholders = List(datePlaceholderCount) { DatePickerItem.Placeholder }
        return placeholders + realData + placeholders
    }

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

    private fun updateMonthList() {
        val newMonthItems = buildMonthList(currentSelectedYear)
        monthItems = newMonthItems
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