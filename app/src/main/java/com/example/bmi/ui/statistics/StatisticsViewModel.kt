package com.example.bmi.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bmi.data.database.BmiRecord
import com.example.bmi.data.repository.BmiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: BmiRepository
) : ViewModel() {

    // ========== Day 模式数据 ==========
    private val _monthData = MutableStateFlow<List<DayBmiData>>(emptyList())
    val monthData: StateFlow<List<DayBmiData>> = _monthData.asStateFlow()

    private val _weightData = MutableStateFlow<List<DayWeightData>>(emptyList())
    val weightData: StateFlow<List<DayWeightData>> = _weightData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isWeightLoading = MutableStateFlow(false)
    val isWeightLoading: StateFlow<Boolean> = _isWeightLoading.asStateFlow()

    private val _monthBmiData = MutableStateFlow<List<DayBmiData>>(emptyList())
    val monthBmiData: StateFlow<List<DayBmiData>> = _monthBmiData.asStateFlow()
    private val _monthWeightData = MutableStateFlow<List<DayWeightData>>(emptyList())
    val monthWeightData: StateFlow<List<DayWeightData>> = _monthWeightData.asStateFlow()

    fun getCurrentMonthBmiData(): List<DayBmiData> = _monthBmiData.value
    fun getCurrentMonthWeightData(): List<DayWeightData> = _monthWeightData.value

    // 当前加载的年月（Day 模式）
    private var currentYear: Int = 0
    private var currentMonth: Int = 0
    private var currentWeightYear: Int = 0
    private var currentWeightMonth: Int = 0

    // ========== Week 模式数据 ==========
    private val _weekBmiData = MutableStateFlow<List<DayBmiData>>(emptyList())
    val weekBmiData: StateFlow<List<DayBmiData>> = _weekBmiData.asStateFlow()

    private val _weekWeightData = MutableStateFlow<List<DayWeightData>>(emptyList())
    val weekWeightData: StateFlow<List<DayWeightData>> = _weekWeightData.asStateFlow()

    // ========== 缓存访问接口（新增） ==========
    fun getCurrentDayBmiData(): List<DayBmiData> = _monthData.value
    fun getCurrentDayWeightData(): List<DayWeightData> = _weightData.value
    fun getCurrentWeekBmiData(): List<DayBmiData> = _weekBmiData.value
    fun getCurrentWeekWeightData(): List<DayWeightData> = _weekWeightData.value

    init {
        // 监听数据库变化，自动重新加载当前模式数据
        viewModelScope.launch {
            repository.observeLatestRecord()
                .collect { _ ->
                    if (currentYear != 0 || currentMonth != 0) {
                        loadMonthData(currentYear, currentMonth)
                        loadWeightMonthData(currentYear, currentMonth)
                    }
                }
        }
        viewModelScope.launch {
            repository.observeLatestRecord()
                .collect { _ ->
                    // Week 模式暂不自动刷新，可后续优化（也可以刷新，但需要判断当前模式）
                    // 若需要，可调用 loadWeekData()
                }
        }
    }

    // ========== Day 模式加载方法 ==========
    fun loadMonthData(year: Int, month: Int) {
        this.currentYear = year
        this.currentMonth = month
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val records = repository.getMonthLatestRecords(year, month)
                val data = buildDayData(year, month, records)
                _monthData.value = data
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadWeightMonthData(year: Int, month: Int) {
        this.currentWeightYear = year
        this.currentWeightMonth = month
        viewModelScope.launch {
            _isWeightLoading.value = true
            try {
                val records = repository.getMonthLatestRecords(year, month)
                val data = buildWeightDayData(year, month, records)
                _weightData.value = data
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isWeightLoading.value = false
            }
        }
    }

    private fun buildDayData(year: Int, month: Int, records: List<BmiRecord>): List<DayBmiData> {
        val calendar = Calendar.getInstance().apply {
            set(year, month, 1)
        }
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val latestByDay = records.groupBy { record ->
            val cal = Calendar.getInstance().apply { timeInMillis = record.timestamp }
            cal.get(Calendar.DAY_OF_MONTH)
        }.mapValues { entry ->
            entry.value.maxByOrNull { it.timestamp }
        }

        val result = mutableListOf<DayBmiData>()
        for (day in 1..daysInMonth) {
            val date = Calendar.getInstance().apply {
                set(year, month, day, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val record = latestByDay[day]
            val bmi = record?.bmi?.toFloat()
            result.add(DayBmiData(date, bmi))
        }
        return result
    }

    private fun buildWeightDayData(year: Int, month: Int, records: List<BmiRecord>): List<DayWeightData> {
        val calendar = Calendar.getInstance().apply {
            set(year, month, 1)
        }
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val latestByDay = records.groupBy { record ->
            val cal = Calendar.getInstance().apply { timeInMillis = record.timestamp }
            cal.get(Calendar.DAY_OF_MONTH)
        }.mapValues { entry ->
            entry.value.maxByOrNull { it.timestamp }
        }

        val result = mutableListOf<DayWeightData>()
        for (day in 1..daysInMonth) {
            val date = Calendar.getInstance().apply {
                set(year, month, day, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val record = latestByDay[day]
            val weight = record?.weightInput?.toFloat()
            result.add(DayWeightData(date, weight))
        }
        return result
    }

    // ========== Week 模式加载方法 ==========
    fun loadWeekData() {
        viewModelScope.launch {
            try {
                val today = Calendar.getInstance()
                val thisWeekMonday = getWeekStart(today)

                val mondays = mutableListOf<Calendar>()
                for (i in 52 downTo 0) {
                    val monday = thisWeekMonday.clone() as Calendar
                    monday.add(Calendar.DAY_OF_YEAR, -i * 7)
                    mondays.add(monday)
                }
                val nextMonday = thisWeekMonday.clone() as Calendar
                nextMonday.add(Calendar.DAY_OF_YEAR, 7)
                mondays.add(nextMonday)

                val startTime = mondays.first().timeInMillis
                val lastMonday = mondays[mondays.size - 2]
                val endTime = lastMonday.clone() as Calendar
                endTime.add(Calendar.DAY_OF_YEAR, 6)
                endTime.set(Calendar.HOUR_OF_DAY, 23)
                endTime.set(Calendar.MINUTE, 59)
                endTime.set(Calendar.SECOND, 59)
                endTime.set(Calendar.MILLISECOND, 999)
                val allRecords = repository.getRecordsBetween(startTime, endTime.timeInMillis)

                val dayMap = allRecords.groupBy { record ->
                    val cal = Calendar.getInstance().apply { timeInMillis = record.timestamp }
                    cal.get(Calendar.YEAR) to cal.get(Calendar.DAY_OF_YEAR)
                }.mapValues { (_, records) ->
                    records.maxByOrNull { it.timestamp }
                }

                val weekBmiList = mutableListOf<DayBmiData>()
                val weekWeightList = mutableListOf<DayWeightData>()

                for (i in mondays.indices) {
                    val monday = mondays[i]
                    val isPlaceholder = (i == mondays.size - 1)

                    if (isPlaceholder) {
                        weekBmiList.add(DayBmiData(monday.clone() as Calendar, null))
                        weekWeightList.add(DayWeightData(monday.clone() as Calendar, null))
                        continue
                    }

                    val weekEnd = monday.clone() as Calendar
                    weekEnd.add(Calendar.DAY_OF_YEAR, 6)

                    var validDays = 0
                    var sumBmi = 0f
                    var sumWeight = 0f

                    var currentDay = monday.clone() as Calendar
                    while (currentDay <= weekEnd) {
                        val key = currentDay.get(Calendar.YEAR) to currentDay.get(Calendar.DAY_OF_YEAR)
                        val record = dayMap[key]
                        if (record != null) {
                            record.bmi?.toFloat()?.let {
                                sumBmi += it
                                validDays++
                            }
                            record.weightInput?.toFloat()?.let {
                                sumWeight += it
                            }
                        }
                        currentDay.add(Calendar.DAY_OF_YEAR, 1)
                    }

                    val avgBmi = if (validDays > 0) sumBmi / validDays else null

                    var weightValidDays = 0
                    var weightSum = 0f
                    currentDay = monday.clone() as Calendar
                    while (currentDay <= weekEnd) {
                        val key = currentDay.get(Calendar.YEAR) to currentDay.get(Calendar.DAY_OF_YEAR)
                        val record = dayMap[key]
                        if (record?.weightInput != null) {
                            weightSum += record.weightInput.toFloat()
                            weightValidDays++
                        }
                        currentDay.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    val avgWeight = if (weightValidDays > 0) weightSum / weightValidDays else null

                    weekBmiList.add(DayBmiData(monday.clone() as Calendar, avgBmi))
                    weekWeightList.add(DayWeightData(monday.clone() as Calendar, avgWeight))
                }

                _weekBmiData.value = weekBmiList
                _weekWeightData.value = weekWeightList

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getWeekStart(calendar: Calendar): Calendar {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val diff = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        cal.add(Calendar.DAY_OF_YEAR, -diff)
        return cal
    }

    fun loadMonthStatistics() {
        viewModelScope.launch {
            try {
                // 1. 确定日期范围：当前月+1 往前推5年
                val today = Calendar.getInstance()
                val endDate = today.clone() as Calendar
                endDate.add(Calendar.MONTH, 1) // 当前月+1
                endDate.set(Calendar.DAY_OF_MONTH, 1)
                endDate.set(Calendar.HOUR_OF_DAY, 0)
                endDate.set(Calendar.MINUTE, 0)
                endDate.set(Calendar.SECOND, 0)
                endDate.set(Calendar.MILLISECOND, 0)

                val startDate = endDate.clone() as Calendar
                startDate.add(Calendar.YEAR, -5) // 往前推5年

                // 2. 获取该时间范围内的所有记录
                val records = repository.getRecordsBetween(
                    startDate.timeInMillis,
                    endDate.timeInMillis
                )

                // 3. 按天分组取最后一条
                val latestPerDay = records.groupBy { record ->
                    val cal = Calendar.getInstance().apply { timeInMillis = record.timestamp }
                    cal.get(Calendar.YEAR) to cal.get(Calendar.DAY_OF_YEAR)
                }.mapValues { (_, list) ->
                    list.maxByOrNull { it.timestamp }
                }

                // 4. 按月分组，计算平均BMI（只使用每天最后一条）
                val monthMap = mutableMapOf<Pair<Int, Int>, MutableList<Float>>()
                latestPerDay.values.forEach { record ->
                    record?.let {
                        val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        val key = cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
                        monthMap.getOrPut(key) { mutableListOf() }.add(it.bmi.toFloat())
                    }
                }

                // 5. 生成61个月的数据（从 startDate 到 endDate 的每个月）
                val result = mutableListOf<DayBmiData>()
                var current = startDate.clone() as Calendar
                while (current <= endDate) {
                    val year = current.get(Calendar.YEAR)
                    val month = current.get(Calendar.MONTH)
                    val bmiList = monthMap[year to month]
                    val avgBmi = if (bmiList.isNullOrEmpty()) null else bmiList.average().toFloat()
                    val date = current.clone() as Calendar
                    date.set(Calendar.DAY_OF_MONTH, 1) // 统一为每月1号
                    result.add(DayBmiData(date, avgBmi))
                    current.add(Calendar.MONTH, 1)
                }

                _monthBmiData.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadMonthWeightStatistics() {
        viewModelScope.launch {
            try {
                val today = Calendar.getInstance()
                val endDate = today.clone() as Calendar
                endDate.set(Calendar.DAY_OF_MONTH, 1)
                endDate.add(Calendar.MONTH, 1)
                endDate.set(Calendar.HOUR_OF_DAY, 0)
                endDate.set(Calendar.MINUTE, 0)
                endDate.set(Calendar.SECOND, 0)
                endDate.set(Calendar.MILLISECOND, 0)

                val startDate = endDate.clone() as Calendar
                startDate.add(Calendar.YEAR, -5)

                val records = repository.getRecordsBetween(
                    startDate.timeInMillis,
                    endDate.timeInMillis
                )

                // 按天分组取最后一条体重
                val latestPerDay = records.groupBy { record ->
                    val cal = Calendar.getInstance().apply { timeInMillis = record.timestamp }
                    cal.get(Calendar.YEAR) to cal.get(Calendar.DAY_OF_YEAR)
                }.mapValues { (_, list) ->
                    list.maxByOrNull { it.timestamp }
                }

                val monthMap = mutableMapOf<Pair<Int, Int>, MutableList<Float>>()
                latestPerDay.values.forEach { record ->
                    record?.let {
                        val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        val key = cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
                        it.weightInput?.toFloat()?.let { weight ->
                            monthMap.getOrPut(key) { mutableListOf() }.add(weight)
                        }
                    }
                }

                val result = mutableListOf<DayWeightData>()
                var current = startDate.clone() as Calendar
                while (current <= endDate) {
                    val year = current.get(Calendar.YEAR)
                    val month = current.get(Calendar.MONTH)
                    val weightList = monthMap[year to month]
                    val avgWeight = if (weightList.isNullOrEmpty()) null else weightList.average().toFloat()
                    val date = current.clone() as Calendar
                    date.set(Calendar.DAY_OF_MONTH, 1)
                    result.add(DayWeightData(date, avgWeight))
                    current.add(Calendar.MONTH, 1)
                }

                _monthWeightData.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}