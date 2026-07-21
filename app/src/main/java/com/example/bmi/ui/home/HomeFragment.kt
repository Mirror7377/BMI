package com.example.bmi.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.Toast
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
import com.example.bmi.data.database.BmiRecord
import com.example.bmi.databinding.FragmentHomeBinding
import com.example.bmi.ui.adapt.AgeAdapter
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

    // 时段滚轮
    private lateinit var numberPicker: NumberPicker
    private val timeOptions = arrayOf("Morning", "Afternoon", "Evening", "Night")
    private var currentSelectedTimeIndex: Int = 0

    companion object {
        fun newInstance(): HomeFragment {
            val frag = HomeFragment()
            return frag
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
            isClickable = true//根布局可点击
            isFocusableInTouchMode = true//可获取焦点
            setOnClickListener { root ->
                root.clearFocus()
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(root.windowToken, 0)//隐藏键盘
            }
        }

        // 根据当前 isEmptyMode 更新calculate按钮位置
        updateButtonVisibility()

        observeState()
        setupListeners()
        setupAgeRecyclerView()
        initDatePicker()
        initTimePicker()
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
        weightParams.marginStart = dpToPx(weightMarginStart)//布局参数需要使用像素单位
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
        //性别选择
        updateGenderUI(state.gender)
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
            binding.root.clearFocus()
            viewModel.sendIntent(HomeIntent.Calculate)
        }
        binding.btnCalculateWithNav.setOnClickListener {
            binding.root.clearFocus()
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
                //todo 当用户把输入框的数据都删除时，还原为删除前的数据
                editText.setText(viewModel.state.value.weightDisplay)
                showToast("Please input a valid weight (${String.format("%.0f", min)}-${String.format("%.0f", max)}) to calculate your BMI accurately")
            }
            else -> {
                val value = raw.toDouble()
                val clamped = value.coerceIn(min, max)
                val formatted = String.format("%.2f", clamped)
                editText.setText(formatted)
                if (value != clamped || raw != formatted) {
                    //todo 修改标签
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
        // 先同步当前状态的时间到 DatePicker
        val timestamp = viewModel.state.value.timestamp
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        if (calendar.after(today)) {
            calendar.time = today.time
        }
        binding.datePicker.init(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
            null
        )

        binding.datePickerMask.visibility = View.VISIBLE
        binding.datePickerBottomSheet.visibility = View.VISIBLE
        binding.datePickerMask.setOnClickListener { dismissDatePicker() }
        binding.btnDateCancel.setOnClickListener { dismissDatePicker() }
        binding.btnDateDone.setOnClickListener {
            val year = binding.datePicker.year
            val month = binding.datePicker.month // 0-based
            val day = binding.datePicker.dayOfMonth
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(year, month, day)
            // 保留当前时段
            viewModel.sendIntent(
                HomeIntent.TimeChanged(
                    selectedCalendar.timeInMillis,
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

    private fun initDatePicker() {
        val timestamp = viewModel.state.value.timestamp
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        // 限制不能超过今天（与原来逻辑一致）
        val today = Calendar.getInstance()
        if (calendar.after(today)) {
            calendar.time = today.time
        }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) // 注意：DatePicker 的 month 是从0开始的
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        binding.datePicker.init(year, month, day, null)
    }

    private fun showTimeOfDayPicker() {
        // 同步当前选中值
        currentSelectedTimeIndex = when (viewModel.state.value.timeOfDay) {
            TimeOfDay.MORNING -> 0
            TimeOfDay.AFTERNOON -> 1
            TimeOfDay.EVENING -> 2
            TimeOfDay.NIGHT -> 3
            else -> 0
        }
        numberPicker.value = currentSelectedTimeIndex

        binding.timePickerMask.visibility = View.VISIBLE
        binding.timePickerBottomSheet.visibility = View.VISIBLE
        binding.timePickerMask.setOnClickListener { dismissTimePicker() }
        binding.btnTimeCancel.setOnClickListener { dismissTimePicker() }
        binding.btnTimeDone.setOnClickListener {
            val selectedIndex = numberPicker.value
            val timeOfDay = when (selectedIndex) {
                0 -> TimeOfDay.MORNING
                1 -> TimeOfDay.AFTERNOON
                2 -> TimeOfDay.EVENING
                3 -> TimeOfDay.NIGHT
                else -> TimeOfDay.MORNING
            }
            println(timeOfDay)
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
                        //todo 未使用
                        is HomeEffect.ShowError -> {
                            showToast(effect.message)
                        }
                    }
                }
            }
        }
    }

    private fun navigateToDisplay(record: BmiRecord) {
        val intent = Intent(requireContext(), ResultActivity::class.java).apply {
            putExtra("BMI_RECORD", record) // 直接塞整个对象
        }
        startActivity(intent)
    }

    private fun initTimePicker() {
        numberPicker = binding.npTimePicker
        numberPicker.minValue = 0
        numberPicker.maxValue = timeOptions.size - 1
        numberPicker.displayedValues = timeOptions//timeOptions 数组直接绑定到滚轮控件上
        numberPicker.wrapSelectorWheel = false
        // 同步当前选中值
        currentSelectedTimeIndex = when (viewModel.state.value.timeOfDay) {

            TimeOfDay.MORNING -> 0
            TimeOfDay.AFTERNOON -> 1
            TimeOfDay.EVENING -> 2
            TimeOfDay.NIGHT -> 3
            else -> 0
        }
        numberPicker.value = currentSelectedTimeIndex

    }

    private fun updateGenderUI(gender: Gender) {
        val selectedColor = ContextCompat.getColor(requireContext(), R.color.white)
        val unSelectedColor = ContextCompat.getColor(requireContext(), R.color.gender)

        val maleSelected = gender == Gender.MALE

        binding.genderCheck1.visibility =
            if (maleSelected) View.VISIBLE else View.GONE
        binding.genderCheck2.visibility =
            if (!maleSelected) View.VISIBLE else View.GONE

        binding.genderContainer1.setCardBackgroundColor(
            if (maleSelected) selectedColor else unSelectedColor
        )

        binding.genderContainer2.setCardBackgroundColor(
            if (maleSelected) unSelectedColor else selectedColor
        )
    }

}