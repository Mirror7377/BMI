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

    /**
     * 加载指定年月的每天最新 BMI 数据
     */
    fun loadMonthData(year: Int, month: Int) {
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

    /**
     * 构建该月每天的数据（1 号 ~ 最后一天）
     */
    private fun buildDayData(year: Int, month: Int, records: List<BmiRecord>): List<DayBmiData> {
        // 获取该月天数
        val calendar = Calendar.getInstance().apply {
            set(year, month, 1)
        }
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // 按天分组，取每天最新的一条记录
        val latestByDay = records.groupBy { record ->
            val cal = Calendar.getInstance().apply { timeInMillis = record.timestamp }
            cal.get(Calendar.DAY_OF_MONTH)
        }.mapValues { entry ->
            // 按时间排序，取最新的一条
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