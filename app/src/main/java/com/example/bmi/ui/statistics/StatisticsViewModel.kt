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

    private val _monthData = MutableStateFlow<List<DayBmiData>>(emptyList())
    val monthData: StateFlow<List<DayBmiData>> = _monthData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 当前加载的年月
    private var currentYear: Int = 0
    private var currentMonth: Int = 0

    init {
        // 监听数据库变化，自动重新加载数据
        viewModelScope.launch {
            repository.observeLatestRecord()
                .collect { _ ->
                    // 只要有新增/修改/删除记录，就重新加载当前月份数据
                    if (currentYear != 0 || currentMonth != 0) {
                        loadMonthData(currentYear, currentMonth)
                    }
                }
        }
    }

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
}