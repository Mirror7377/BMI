package com.example.bmi.ui.home

import android.animation.ArgbEvaluator
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.TextView
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
import com.example.bmi.MainActivity
import com.example.bmi.R
import com.example.bmi.data.database.BmiRecord
import com.example.bmi.databinding.FragmentHomeBinding
import com.example.bmi.ui.adapt.AgeAdapter
import com.example.bmi.ui.adapt.AgeItemDecoration
import com.example.bmi.ui.home.enums.Gender
import com.example.bmi.ui.home.enums.HeightUnit
import com.example.bmi.ui.home.enums.TimeOfDay
import com.example.bmi.ui.home.enums.WeightUnit
import com.example.bmi.ui.profile.ProfileActivity
import com.example.bmi.ui.result.ResultActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    // 控制计算按钮显示位置，由 MainActivity 动态更新
    private var isEmptyMode: Boolean = false

    private lateinit var ageAdapter: AgeAdapter
    private lateinit var snapHelper: LinearSnapHelper

    private val timeOptions = listOf("Morning", "Afternoon", "Evening", "Night")
    private var selectedTimeIndex = 0
    private var currentSelectedTimeIndex: Int = 0

    // 保存日期滚轮的当前选中值（用于确认时读取）
    private var selectedMonth = 0   // 0-11
    private var selectedDay = 0     // 0-30
    private var selectedYear = 0    // 相对于 1900 的偏移

    // 当前时间的年月日缓存，避免重复计算
    private val nowCalendar by lazy { Calendar.getInstance() }
    private val currentYear by lazy { nowCalendar.get(Calendar.YEAR) }
    private val currentMonth by lazy { nowCalendar.get(Calendar.MONTH) + 1 }
    private val currentDay by lazy { nowCalendar.get(Calendar.DAY_OF_MONTH) }

    companion object {
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        binding.root.apply {
            isClickable = true
            isFocusableInTouchMode = true
            setOnClickListener { root ->
                root.clearFocus()
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(root.windowToken, 0)
            }
        }

        updateButtonVisibility()

        observeState()
        setupListeners()
        setupAgeRecyclerView()
        initWheelDatePickers()          // 初始化三个日期滚轮
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

    private fun updateButtonVisibility() {
        val params = binding.btnCalculate.layoutParams as ConstraintLayout.LayoutParams
        val margin20 = dpToPx(20)
        if (isEmptyMode) {
            params.bottomMargin = margin20
        } else {
            val navHeight = (activity as? MainActivity)?.getBottomNavHeight() ?: 0
            params.bottomMargin = navHeight + margin20
        }
        binding.btnCalculate.layoutParams = params
    }

    // ---------- State Observation ----------
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
        updateGenderUI(state.gender)
    }

    // ---------- Listeners ----------
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

        binding.btnCalculate.setOnClickListener {
            binding.root.clearFocus()
            viewModel.sendIntent(HomeIntent.Calculate)
        }

        binding.ivPerson.setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ---------- Helper: dp to px ----------
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    // ---------- Validation methods (unchanged) ----------
    private fun validateAndFormatWeight(editText: EditText, unit: WeightUnit) {
        // ... 保持原样，略 ...
    }

    private fun validateAndFormatHeightCm(editText: EditText) {
        // ... 保持原样，略 ...
    }

    private fun validateAndFormatFeet(editText: EditText) {
        // ... 保持原样，略 ...
    }

    private fun validateAndFormatInches(editText: EditText) {
        // ... 保持原样，略 ...
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    // ---------- Age RecyclerView (unchanged) ----------
    private fun setupAgeRecyclerView() {
        val ages = (2..99).toList()
        ageAdapter = AgeAdapter(ages) { selectedAge ->
            viewModel.sendIntent(HomeIntent.AgeChanged(selectedAge))
            scrollAgeToCenter(selectedAge)
        }
        binding.rvAgePicker.adapter = ageAdapter

        if (binding.rvAgePicker.itemDecorationCount == 0) {
            val space = resources.getDimensionPixelSize(R.dimen.age_item_space)
            binding.rvAgePicker.addItemDecoration(AgeItemDecoration(space))
        }
        snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(binding.rvAgePicker)

        binding.rvAgePicker.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    snapHelper.findSnapView(recyclerView.layoutManager)?.let {
                        val position = recyclerView.getChildAdapterPosition(it)
                        if (position != RecyclerView.NO_POSITION) {
                            val age = ages[position]
                            viewModel.sendIntent(HomeIntent.AgeChanged(age))
                        }
                    }
                }
            }
        })

        binding.rvAgePicker.post {
            val recyclerWidth = binding.rvAgePicker.width
            val itemWidth = resources.getDimensionPixelSize(R.dimen.age_item_width)
            val sidePadding = (recyclerWidth - itemWidth) / 2
            binding.rvAgePicker.setPadding(sidePadding, 0, sidePadding, 0)

            val defaultAge = viewModel.state.value.age
            val index = defaultAge - 2
            val layoutManager = binding.rvAgePicker.layoutManager as LinearLayoutManager
            layoutManager.scrollToPosition(index)

            binding.rvAgePicker.post {
                snapHelper.findSnapView(layoutManager)?.let { view ->
                    val distance = snapHelper.calculateDistanceToFinalSnap(layoutManager, view)
                    if (distance != null) {
                        binding.rvAgePicker.scrollBy(distance[0], distance[1])
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
        // ... 保持原样，略（已有实现） ...
    }

    private fun scrollAgeToCenter(age: Int) {
        // ... 保持原样，略 ...
    }

    // ============================================================
    //  NEW: Custom Date Pickers (Month, Day, Year)
    // ============================================================

    /**
     * 初始化三个日期滚轮（月、日、年）
     */
    private fun initWheelDatePickers() {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = viewModel.state.value.timestamp.coerceAtMost(System.currentTimeMillis())
        }

        // --- 年份滚轮：只到当前年 ---
        val years = (1900..currentYear).map { it.toString() }
        initWheelPicker(
            recyclerView = binding.rvYear,
            data = years,
            defaultPosition = calendar.get(Calendar.YEAR) - 1900,
            onItemSelected = { position ->
                selectedYear = position
                // 年份变化时更新月份和日期
                updateMonthAndDayPickers()
            }
        )

        // --- 月份滚轮：动态生成，根据当前选中年份 ---
        val initialMonths = getMonthNamesForYear(calendar.get(Calendar.YEAR))
        initWheelPicker(
            recyclerView = binding.rvMonth,
            data = initialMonths,
            defaultPosition = calendar.get(Calendar.MONTH).coerceAtMost(initialMonths.size - 1),
            onItemSelected = { position ->
                selectedMonth = position
                // 月份变化时更新日期
                updateDayPickerForMonth(selectedYear + 1900, selectedMonth + 1)
            }
        )

        // --- 日期滚轮：动态生成，根据当前选中的年和月 ---
        val initialDays = getDaysForMonth(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
        initWheelPicker(
            recyclerView = binding.rvDay,
            data = initialDays,
            defaultPosition = (calendar.get(Calendar.DAY_OF_MONTH) - 1).coerceAtMost(initialDays.size - 1),
            onItemSelected = { position ->
                selectedDay = position
            }
        )
    }

    /**
     * 通用滚轮初始化方法
     */
    private fun initWheelPicker(
        recyclerView: RecyclerView,
        data: List<String>,
        defaultPosition: Int,
        onItemSelected: (Int) -> Unit
    ) {
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        recyclerView.layoutManager = layoutManager

        val adapter = WheelAdapter(data) { position ->
            onItemSelected(position)
        }
        recyclerView.adapter = adapter

        // 设置上下边距使中间项居中
        val itemHeight = dpToPx(45)
        val totalHeight = dpToPx(315)   // 与布局中高度一致
        val padding = (totalHeight - itemHeight) / 2
        recyclerView.setPadding(0, padding, 0, padding)
        recyclerView.clipToPadding = false

        // 添加双横线装饰器
        recyclerView.addItemDecoration(WheelDividerDecoration(requireContext()))

        // 添加 SnapHelper
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)

        // 滚动到默认位置
        recyclerView.post {
            layoutManager.scrollToPosition(defaultPosition)
            // 微调对齐
            snapHelper.findSnapView(layoutManager)?.let { view ->
                val distance = snapHelper.calculateDistanceToFinalSnap(layoutManager, view)
                if (distance != null) {
                    recyclerView.scrollBy(distance[0], distance[1])
                }
            }
            updateWheelEffects(recyclerView)
        }

        // 滚动监听更新透明度
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateWheelEffects(recyclerView)
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    snapHelper.findSnapView(layoutManager)?.let { view ->
                        val position = recyclerView.getChildAdapterPosition(view)
                        if (position != RecyclerView.NO_POSITION) {
                            onItemSelected(position)
                        }
                    }
                }
            }
        })
    }

    /**
     * 更新滚轮中每个 item 的透明度和颜色（中间黑，向外渐变灰）
     */
    private fun updateWheelEffects(recyclerView: RecyclerView) {
        if (recyclerView.childCount == 0) return

        val centerY = recyclerView.height / 2f
        val itemHeight = dpToPx(45).toFloat()
        val maxDistance = itemHeight * 2.5f

        val argbEvaluator = ArgbEvaluator()
        val startColor = 0xFF000000.toInt()   // 黑色
        val endColor = 0xFFBBBBBB.toInt()     // 浅灰

        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            val tv = child.findViewById<TextView>(R.id.tvWheelItem)
            if (tv == null) continue

            val childCenterY = child.top + child.height / 2f
            val distance = abs(childCenterY - centerY)
            val ratio = (distance / maxDistance).coerceIn(0f, 1f)

            // Alpha: 中心 1.0，边缘 0.25
            tv.alpha = 1f - ratio * 0.75f

            // 颜色插值
            val color = argbEvaluator.evaluate(ratio, startColor, endColor) as Int
            tv.setTextColor(color)
        }
    }

    /**
     * 根据年份和月份更新日期滚轮的数据（联动）
     */
    private fun updateDayPickerForMonth(year: Int, month: Int) {
        val newDays = getDaysForMonth(year, month)
        val dayAdapter = binding.rvDay.adapter as? WheelAdapter
        dayAdapter?.updateData(newDays)

        // 修正选中日期
        if (selectedDay >= newDays.size) {
            selectedDay = newDays.size - 1
            binding.rvDay.layoutManager?.scrollToPosition(selectedDay)
        }
    }

    // ---------- Inner Adapter for Wheel ----------
    inner class WheelAdapter(
        private var data: List<String>,
        private val onItemSelected: (Int) -> Unit
    ) : RecyclerView.Adapter<WheelAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_wheel_picker, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.tv.text = data[position]
        }

        override fun getItemCount(): Int = data.size

        fun updateData(newData: List<String>) {
            data = newData
            notifyDataSetChanged()
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tv: TextView = itemView.findViewById(R.id.tvWheelItem)
        }
    }

    // ---------- ItemDecoration for short lines (三段式) ----------
    // ---------- ItemDecoration for short lines (根据滚轮自动调整宽度) ----------
    inner class WheelDividerDecoration(context: Context) : RecyclerView.ItemDecoration() {
        private val density = context.resources.displayMetrics.density
        private val paint = Paint().apply {
            color = ContextCompat.getColor(context, R.color.splash_blue)
            strokeWidth = 0.5f * density
            isAntiAlias = true
        }
        // 日期滚轮宽 40dp，时段滚轮宽 100dp
        private val dateHalfWidth = 20f * density   // 40dp / 2
        private val timeHalfWidth = 50f * density   // 100dp / 2

        override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            val centerX = parent.width / 2f
            val centerY = parent.height / 2f
            val itemHeight = 45f * density

            // 根据 RecyclerView 的 id 选择宽度
            val halfWidth = if (parent.id == R.id.rvTimePicker) timeHalfWidth else dateHalfWidth

            // 上横线
            c.drawLine(
                centerX - halfWidth,
                centerY - itemHeight / 2,
                centerX + halfWidth,
                centerY - itemHeight / 2,
                paint
            )
            // 下横线
            c.drawLine(
                centerX - halfWidth,
                centerY + itemHeight / 2,
                centerX + halfWidth,
                centerY + itemHeight / 2,
                paint
            )
        }
    }

    /**
     * 显示日期选择弹窗
     */
    private fun showDatePicker() {
        (activity as? MainActivity)?.hideBottomNav()

        // 同步当前日期（确保不超过今天）
        val calendar = Calendar.getInstance().apply {
            timeInMillis = viewModel.state.value.timestamp.coerceAtMost(System.currentTimeMillis())
        }

        // 重新生成数据，确保与当前时间一致
        val years = (1900..currentYear).map { it.toString() }
        val months = getMonthNamesForYear(calendar.get(Calendar.YEAR))
        val days = getDaysForMonth(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)

        // 更新适配器数据
        (binding.rvYear.adapter as? WheelAdapter)?.updateData(years)
        (binding.rvMonth.adapter as? WheelAdapter)?.updateData(months)
        (binding.rvDay.adapter as? WheelAdapter)?.updateData(days)

        // 设置选中位置
        binding.rvYear.layoutManager?.scrollToPosition(calendar.get(Calendar.YEAR) - 1900)
        binding.rvMonth.layoutManager?.scrollToPosition(
            calendar.get(Calendar.MONTH).coerceAtMost(months.size - 1)
        )
        binding.rvDay.layoutManager?.scrollToPosition(
            (calendar.get(Calendar.DAY_OF_MONTH) - 1).coerceAtMost(days.size - 1)
        )

        // 更新成员变量
        selectedYear = calendar.get(Calendar.YEAR) - 1900
        selectedMonth = calendar.get(Calendar.MONTH).coerceAtMost(months.size - 1)
        selectedDay = (calendar.get(Calendar.DAY_OF_MONTH) - 1).coerceAtMost(days.size - 1)

        // 显示弹窗
        binding.datePickerMask.visibility = View.VISIBLE
        binding.datePickerBottomSheet.visibility = View.VISIBLE
        binding.datePickerMask.setOnClickListener { dismissDatePicker() }
        binding.btnDateCancel.setOnClickListener { dismissDatePicker() }

        binding.btnDateDone.setOnClickListener {
            val month = selectedMonth + 1
            val day = selectedDay + 1
            val year = selectedYear + 1900

            val selectedCalendar = Calendar.getInstance().apply {
                set(year, month - 1, day)
            }
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
        (activity as? MainActivity)?.showBottomNav()
    }

    // ---------- Time Picker (unchanged) ----------
    private fun showTimeOfDayPicker() {
        (activity as? MainActivity)?.hideBottomNav()

        // 同步当前选中位置
        val currentPos = when (viewModel.state.value.timeOfDay) {
            TimeOfDay.MORNING -> 0
            TimeOfDay.AFTERNOON -> 1
            TimeOfDay.EVENING -> 2
            TimeOfDay.NIGHT -> 3
            else -> 0
        }
        binding.rvTimePicker.layoutManager?.scrollToPosition(currentPos)

        binding.timePickerMask.visibility = View.VISIBLE
        binding.timePickerBottomSheet.visibility = View.VISIBLE
        binding.timePickerMask.setOnClickListener { dismissTimePicker() }
        binding.btnTimeCancel.setOnClickListener { dismissTimePicker() }

        binding.btnTimeDone.setOnClickListener {
            val timeOfDay = when (selectedTimeIndex) {
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
        (activity as? MainActivity)?.showBottomNav()
    }

    // ---------- Init Time Picker ----------
    private fun initTimePicker() {
        initWheelPicker(
            recyclerView = binding.rvTimePicker,
            data = timeOptions,
            defaultPosition = when (viewModel.state.value.timeOfDay) {
                TimeOfDay.MORNING -> 0
                TimeOfDay.AFTERNOON -> 1
                TimeOfDay.EVENING -> 2
                TimeOfDay.NIGHT -> 3
                else -> 0
            },
            onItemSelected = { position ->
                selectedTimeIndex = position
            }
        )
    }

    // ---------- Observe Effects ----------
    private fun observeEffect() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        is HomeEffect.NavigateToResult -> navigateToDisplay(effect.record)
                        is HomeEffect.ShowError -> showToast(effect.message)
                    }
                }
            }
        }
    }

    private fun navigateToDisplay(record: BmiRecord) {
        val intent = Intent(requireContext(), ResultActivity::class.java).apply {
            putExtra("BMI_RECORD", record)
        }
        startActivity(intent)
    }

    // ---------- Gender UI (unchanged) ----------
    private fun updateGenderUI(gender: Gender) {
        val selectedColor = ContextCompat.getColor(requireContext(), R.color.white)
        val unSelectedColor = ContextCompat.getColor(requireContext(), R.color.gender)
        val maleSelected = gender == Gender.MALE

        binding.genderCheck1.visibility = if (maleSelected) View.VISIBLE else View.GONE
        binding.genderCheck2.visibility = if (!maleSelected) View.VISIBLE else View.GONE
        binding.genderContainer1.setCardBackgroundColor(if (maleSelected) selectedColor else unSelectedColor)
        binding.genderContainer2.setCardBackgroundColor(if (maleSelected) unSelectedColor else selectedColor)
    }

    /**
     * 获取某一年份可用的月份名称列表（动态裁剪到当前月）
     */
    private fun getMonthNamesForYear(year: Int): List<String> {
        val allMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "June", "July", "Aug", "Sep", "Oct", "Nov", "Dec")
        return if (year == currentYear) {
            allMonths.subList(0, currentMonth)
        } else {
            allMonths
        }
    }

    /**
     * 获取某年某月的天数列表（动态裁剪到当前日）
     */
    private fun getDaysForMonth(year: Int, month: Int): List<String> {
        val cal = Calendar.getInstance().apply { set(year, month - 1, 1) }
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val limit = if (year == currentYear && month == currentMonth) {
            currentDay
        } else {
            maxDay
        }
        return (1..limit).map { String.format("%02d", it) }
    }

    /**
     * 年份或月份变化时，联动更新月份和日期
     */
    private fun updateMonthAndDayPickers() {
        val year = selectedYear + 1900

        // 1. 更新月份滚轮数据
        val newMonths = getMonthNamesForYear(year)
        val monthAdapter = binding.rvMonth.adapter as? WheelAdapter
        monthAdapter?.updateData(newMonths)

        // 修正选中的月份，如果当前选中的月份超出新范围，则选中最后一个
        if (selectedMonth >= newMonths.size) {
            selectedMonth = newMonths.size - 1
            binding.rvMonth.layoutManager?.scrollToPosition(selectedMonth)
        }

        // 2. 更新日期滚轮（基于新的月份）
        updateDayPickerForMonth(year, selectedMonth + 1)
    }
}