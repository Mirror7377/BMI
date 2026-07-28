// StatisticsViewModel.kt
package com.example.bmi.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bmi.data.database.BmiRecord
import com.example.bmi.data.repository.BmiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: BmiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _state.asStateFlow()

    // 保持原有缓存访问方法（供 Fragment 直接调用，但统一由 State 驱动后，以下方法不再需要）
    // 但为了兼容性，我们仍保留 getter，但实际不推荐直接使用，可删除。
    // 由于您要求逻辑完全不变，我们先保留这些方法，但 Fragment 会通过 State 获取数据，无需调用。
    // 如果需要，Fragment 可以直接从 state 中读取缓存。

    init {
        // 默认加载 Day 模式
        dispatch(StatisticsIntent.LoadDay)
    }

    fun dispatch(intent: StatisticsIntent) {
        when (intent) {
            is StatisticsIntent.LoadDay -> loadDay()
            is StatisticsIntent.LoadWeek -> loadWeek()
            is StatisticsIntent.LoadMonth -> loadMonth()
        }
    }

    // ========== Day 模式加载 ==========
    private fun loadDay() {
        // 先使用缓存（如果存在）
        val cachedBmi = _state.value.dayBmiCache
        val cachedWeight = _state.value.dayWeightCache
        if (cachedBmi.isNotEmpty() || cachedWeight.isNotEmpty()) {
            _state.update {
                it.copy(
                    mode = ChartMode.DAY,
                    bmiData = cachedBmi,
                    weightData = cachedWeight,
                    isLoading = false,
                    isWeightLoading = false
                )
            }
        } else {
            _state.update { it.copy(isLoading = true, isWeightLoading = true) }
        }

        viewModelScope.launch {
            try {
                // 原始逻辑：调用 loadDayRangeData(60) 和 loadWeightDayRangeData(60)
                val endDate = Calendar.getInstance()
                val startDate = endDate.clone() as Calendar
                startDate.add(Calendar.DAY_OF_YEAR, -60)

                val records = repository.getRecordsBetween(startDate.timeInMillis, endDate.timeInMillis)
                val bmiData = buildDayRangeData(startDate, endDate, records)
                val weightData = buildWeightDayRangeData(startDate, endDate, records)

                _state.update {
                    it.copy(
                        mode = ChartMode.DAY,
                        bmiData = bmiData,
                        weightData = weightData,
                        dayBmiCache = bmiData,
                        dayWeightCache = weightData,
                        isLoading = false,
                        isWeightLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        isWeightLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    // ========== Week 模式加载 ==========
    private fun loadWeek() {
        // 先使用缓存
        val cachedBmi = _state.value.weekBmiCache
        val cachedWeight = _state.value.weekWeightCache
        if (cachedBmi.isNotEmpty() || cachedWeight.isNotEmpty()) {
            _state.update {
                it.copy(
                    mode = ChartMode.WEEK,
                    bmiData = cachedBmi,
                    weightData = cachedWeight,
                    isLoading = false,
                    isWeightLoading = false
                )
            }
        } else {
            _state.update { it.copy(isLoading = true, isWeightLoading = true) }
        }

        viewModelScope.launch {
            try {
                // 调用原始的 loadWeekData() 逻辑（直接内联）
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
                }.mapValues { (_, records) -> records.maxByOrNull { it.timestamp } }

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
                    var weightValidDays = 0

                    var currentDay = monday.clone() as Calendar
                    while (currentDay <= weekEnd) {
                        val key = currentDay.get(Calendar.YEAR) to currentDay.get(Calendar.DAY_OF_YEAR)
                        val record = dayMap[key]
                        if (record != null) {
                            record.bmi?.toFloat()?.let {
                                sumBmi += it
                                validDays++
                            }
                            record.weightKg?.toFloat()?.let {
                                sumWeight += it
                                weightValidDays++
                            }
                        }
                        currentDay.add(Calendar.DAY_OF_YEAR, 1)
                    }

                    val avgBmi = if (validDays > 0) sumBmi / validDays else null
                    val avgWeight = if (weightValidDays > 0) sumWeight / weightValidDays else null

                    weekBmiList.add(DayBmiData(monday.clone() as Calendar, avgBmi))
                    weekWeightList.add(DayWeightData(monday.clone() as Calendar, avgWeight))
                }

                _state.update {
                    it.copy(
                        mode = ChartMode.WEEK,
                        bmiData = weekBmiList,
                        weightData = weekWeightList,
                        weekBmiCache = weekBmiList,
                        weekWeightCache = weekWeightList,
                        isLoading = false,
                        isWeightLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        isWeightLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    // ========== Month 模式加载 ==========
    private fun loadMonth() {
        val cachedBmi = _state.value.monthBmiCache
        val cachedWeight = _state.value.monthWeightCache
        if (cachedBmi.isNotEmpty() || cachedWeight.isNotEmpty()) {
            _state.update {
                it.copy(
                    mode = ChartMode.MONTH,
                    bmiData = cachedBmi,
                    weightData = cachedWeight,
                    isLoading = false,
                    isWeightLoading = false
                )
            }
        } else {
            _state.update { it.copy(isLoading = true, isWeightLoading = true) }
        }

        viewModelScope.launch {
            try {
                // 原始的 loadMonthStatistics() 和 loadMonthWeightStatistics() 逻辑
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

                val records = repository.getRecordsBetween(startDate.timeInMillis, endDate.timeInMillis)

                // 按天分组取最后一条
                val latestPerDay = records.groupBy { record ->
                    val cal = Calendar.getInstance().apply { timeInMillis = record.timestamp }
                    cal.get(Calendar.YEAR) to cal.get(Calendar.DAY_OF_YEAR)
                }.mapValues { (_, list) -> list.maxByOrNull { it.timestamp } }

                val bmiMonthMap = mutableMapOf<Pair<Int, Int>, MutableList<Float>>()
                val weightMonthMap = mutableMapOf<Pair<Int, Int>, MutableList<Float>>()

                latestPerDay.values.forEach { record ->
                    record?.let {
                        val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        val key = cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
                        it.bmi?.toFloat()?.let { bmi ->
                            bmiMonthMap.getOrPut(key) { mutableListOf() }.add(bmi)
                        }
                        it.weightKg?.toFloat()?.let { weight ->
                            weightMonthMap.getOrPut(key) { mutableListOf() }.add(weight)
                        }
                    }
                }

                val bmiResult = mutableListOf<DayBmiData>()
                val weightResult = mutableListOf<DayWeightData>()

                var current = startDate.clone() as Calendar
                while (current <= endDate) {
                    val year = current.get(Calendar.YEAR)
                    val month = current.get(Calendar.MONTH)
                    val key = year to month

                    val bmiList = bmiMonthMap[key]
                    val avgBmi = if (bmiList.isNullOrEmpty()) null else bmiList.average().toFloat()

                    val weightList = weightMonthMap[key]
                    val avgWeight = if (weightList.isNullOrEmpty()) null else weightList.average().toFloat()

                    val date = current.clone() as Calendar
                    date.set(Calendar.DAY_OF_MONTH, 1)

                    bmiResult.add(DayBmiData(date, avgBmi))
                    weightResult.add(DayWeightData(date, avgWeight))

                    current.add(Calendar.MONTH, 1)
                }

                _state.update {
                    it.copy(
                        mode = ChartMode.MONTH,
                        bmiData = bmiResult,
                        weightData = weightResult,
                        monthBmiCache = bmiResult,
                        monthWeightCache = weightResult,
                        isLoading = false,
                        isWeightLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        isWeightLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    // ===== 辅助方法（从原代码完整迁移） =====

    private fun buildDayRangeData(startDate: Calendar, endDate: Calendar, records: List<BmiRecord>): List<DayBmiData> {
        val latestPerDay = records.groupBy { record ->
            val cal = Calendar.getInstance().apply { timeInMillis = record.timestamp }
            cal.get(Calendar.YEAR) to cal.get(Calendar.DAY_OF_YEAR)
        }.mapValues { (_, list) -> list.maxByOrNull { it.timestamp } }

        val result = mutableListOf<DayBmiData>()
        var current = startDate.clone() as Calendar
        while (current <= endDate) {
            val key = current.get(Calendar.YEAR) to current.get(Calendar.DAY_OF_YEAR)
            val record = latestPerDay[key]
            val bmi = record?.bmi?.toFloat()
            result.add(DayBmiData(current.clone() as Calendar, bmi))
            current.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    private fun buildWeightDayRangeData(startDate: Calendar, endDate: Calendar, records: List<BmiRecord>): List<DayWeightData> {
        val latestPerDay = records.groupBy { record ->
            val cal = Calendar.getInstance().apply { timeInMillis = record.timestamp }
            cal.get(Calendar.YEAR) to cal.get(Calendar.DAY_OF_YEAR)
        }.mapValues { (_, list) -> list.maxByOrNull { it.timestamp } }

        val result = mutableListOf<DayWeightData>()
        var current = startDate.clone() as Calendar
        while (current <= endDate) {
            val key = current.get(Calendar.YEAR) to current.get(Calendar.DAY_OF_YEAR)
            val record = latestPerDay[key]
            val weight = record?.weightKg?.toFloat()
            result.add(DayWeightData(current.clone() as Calendar, weight))
            current.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
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

    // ===== 保留原有的缓存访问方法（为兼容，但实际不使用） =====
    // 以下方法在 State 驱动下不再需要，但若 Fragment 中有直接调用，可保留。
    // 为了确保 Fragment 能获取缓存数据，可直接从 state.value 中读取。
    fun getCurrentDayBmiData(): List<DayBmiData> = _state.value.dayBmiCache
    fun getCurrentDayWeightData(): List<DayWeightData> = _state.value.dayWeightCache
    fun getCurrentWeekBmiData(): List<DayBmiData> = _state.value.weekBmiCache
    fun getCurrentWeekWeightData(): List<DayWeightData> = _state.value.weekWeightCache
    fun getCurrentMonthBmiData(): List<DayBmiData> = _state.value.monthBmiCache
    fun getCurrentMonthWeightData(): List<DayWeightData> = _state.value.monthWeightCache
}