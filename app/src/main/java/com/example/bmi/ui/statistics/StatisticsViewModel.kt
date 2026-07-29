// StatisticsViewModel.kt
package com.example.bmi.ui.statistics

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bmi.data.database.BmiRecord
import com.example.bmi.data.enums.ChartMode
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

    //在 StatisticsViewModel 对象被创建的那一瞬间
    //(也就是类构造函数的最后一步)立即执行，早于 Fragment 的任何生命周期。
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
        //缓存存在
        if (cachedBmi.isNotEmpty() || cachedWeight.isNotEmpty()) {
            _state.update {
                it.copy(
                    mode = ChartMode.DAY,
                    bmiData = cachedBmi,
                    weightData = cachedWeight
                )
            }
        }

        viewModelScope.launch {
            try {
                val endDate = Calendar.getInstance()//当前时间
                val startDate = endDate.clone() as Calendar//复制endDate
                startDate.add(Calendar.DAY_OF_YEAR, -60)//往前推60天

                val records = repository.getRecordsBetween(startDate.timeInMillis, endDate.timeInMillis)
                val bmiData = buildDayRangeData(startDate, endDate, records)
                val weightData = buildWeightDayRangeData(startDate, endDate, records)

                _state.update {
                    it.copy(
                        mode = ChartMode.DAY,
                        bmiData = bmiData,
                        weightData = weightData,
                        dayBmiCache = bmiData,
                        dayWeightCache = weightData
                    )
                }
            } catch (e: Exception) {
                Log.e("StatisticsViewModel", "loadDay error", e)
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
                    weightData = cachedWeight
                )
            }
        }

        viewModelScope.launch {
            try {
                val today = Calendar.getInstance()
                val thisWeekMonday = getWeekStart(today)//返回的是本周一的00:00:00 2026-07-27 00:00:00

                val mondays = mutableListOf<Calendar>()
                //循环从 i = 52（一年前）开始
                for (i in 52 downTo 0) {
                    val monday = thisWeekMonday.clone() as Calendar
                    //              1 月 2 日 → DAY_OF_YEAR = 2
                    monday.add(Calendar.DAY_OF_YEAR, -i * 7)//把数据往回拨动一周
                    //最终包含 53 个周一
                    mondays.add(monday)
                }
                //d当前时间的下周一的占位符,不显示数据
                val nextMonday = thisWeekMonday.clone() as Calendar
                nextMonday.add(Calendar.DAY_OF_YEAR, 7)
                mondays.add(nextMonday)

                //取第一个数据，也就是52周前
                val startTime = mondays.first().timeInMillis
                //本周一
                val lastMonday = mondays[mondays.size - 2]
                val endTime = lastMonday.clone() as Calendar
                //把 endTime 从“本周一的 00:00:00.000”，改造成“本周日的 23:59:59.999”。
                endTime.add(Calendar.DAY_OF_YEAR, 6)
                endTime.set(Calendar.HOUR_OF_DAY, 23)
                endTime.set(Calendar.MINUTE, 59)
                endTime.set(Calendar.SECOND, 59)
                endTime.set(Calendar.MILLISECOND, 999)

                val allRecords = repository.getRecordsBetween(startTime, endTime.timeInMillis)

                //按天分组，记录
                val dayMap = allRecords.groupBy { record ->
                    val cal = Calendar.getInstance().apply { timeInMillis = record.timestamp }
                    cal.get(Calendar.YEAR) to cal.get(Calendar.DAY_OF_YEAR)
                }.mapValues { (_, records) -> records.maxByOrNull { it.timestamp } }

                val weekBmiList = mutableListOf<DayBmiData>()
                val weekWeightList = mutableListOf<DayWeightData>()

                //按周计算平均值
                //遍历 mondays 列表的所有索引
                for (i in mondays.indices) {
                    //取出的是 本周一（2026-07-27）
                    val monday = mondays[i]
                    //是否是最后一周   判断当前是不是最后一个元素
                    val isPlaceholder = (i == mondays.size - 1)

                    if (isPlaceholder) {
                        //添加一个 BMI,体重 为 null 的数据
                        weekBmiList.add(DayBmiData(monday.clone() as Calendar, null))
                        weekWeightList.add(DayWeightData(monday.clone() as Calendar, null))
                        continue
                    }

                    //克隆一份当前周一
                    val weekEnd = monday.clone() as Calendar
                    //在克隆的副本上，加 6 天。
                    weekEnd.add(Calendar.DAY_OF_YEAR, 6)

                    //记录这一周里有 BMI 记录的天数。
                    var validDays = 0
                    //用于累加这一周所有天的 BMI 值总和。
                    var sumBmi = 0f
                    //用于累加这一周所有天的体重值总和。
                    var sumWeight = 0f
                    //记录这一周里有体重记录的天数
                    var weightValidDays = 0

                    //再克隆一个“本周一”作为游标（指针），用来逐天遍历。
                    val currentDay = monday.clone() as Calendar
                    while (currentDay <= weekEnd) {
                        //从当前的 currentDay 中提取“年份”和“一年中的第几天”
                        val key = currentDay.get(Calendar.YEAR) to currentDay.get(Calendar.DAY_OF_YEAR)
                        //查找这个 Key 对应的记录。
                        val record = dayMap[key]
                        if (record != null) {
                            record.bmi.toFloat().let {
                                sumBmi += it
                                validDays++
                            }
                            record.weightKg.toFloat().let {
                                sumWeight += it
                                weightValidDays++
                            }
                        }
                        //把游标 currentDay 往后推 1 天
                        currentDay.add(Calendar.DAY_OF_YEAR, 1)
                    }

                    //计算周平均
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
                        weekWeightCache = weekWeightList
                    )
                }
            } catch (e: Exception) {
                Log.e("StatisticsViewModel", "loadWeek error", e)
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
                    weightData = cachedWeight
                )
            }
        }

        viewModelScope.launch {
            try {
                //获取当前系统时间（精确到毫秒）
                val today = Calendar.getInstance()
                //克隆一份 today
                val endDate = today.clone() as Calendar
                //变为本月1.1时分秒毫秒都为0
                endDate.set(Calendar.DAY_OF_MONTH, 1)
                endDate.add(Calendar.MONTH, 1)
                endDate.set(Calendar.HOUR_OF_DAY, 0)
                endDate.set(Calendar.MINUTE, 0)
                endDate.set(Calendar.SECOND, 0)
                endDate.set(Calendar.MILLISECOND, 0)

                //以 endDate 为基准，往前推 5 年，得到 startDate。
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
                        //从日历中提取 年份 和 月份,把所有同一年同一月的记录归到同一个“桶”里。
                        val key = cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
                        it.bmi.toFloat().let { bmi ->
                            bmiMonthMap.getOrPut(key) { mutableListOf() }.add(bmi)
                        }
                        it.weightKg.toFloat().let { weight ->
                            weightMonthMap.getOrPut(key) { mutableListOf() }.add(weight)
                        }
                    }
                }

                val bmiResult = mutableListOf<DayBmiData>()
                val weightResult = mutableListOf<DayWeightData>()

                val current = startDate.clone() as Calendar
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
                        monthWeightCache = weightResult
                    )
                }
            } catch (e: Exception) {
                Log.e("StatisticsViewModel", "loadMonth error", e)
            }
        }
    }


    private fun buildDayRangeData(startDate: Calendar, endDate: Calendar, records: List<BmiRecord>): List<DayBmiData> {

        //groupBy 将 records 列表按照“年份 + 一年中的第几天”分组。
        val latestPerDay = records.groupBy { record ->
            //先拿到毫秒数据
            val cal = Calendar.getInstance().apply { timeInMillis = record.timestamp }
            //按天分配： 2026 to 210天
            cal.get(Calendar.YEAR) to cal.get(Calendar.DAY_OF_YEAR)
        }.mapValues { (_, list) -> list.maxByOrNull { it.timestamp } }//找到分组中每一组按timestamp数据最大的那一条，没有数据则返回null

        //创建一个可变列表
        val result = mutableListOf<DayBmiData>()
        val current = startDate.clone() as Calendar//复制一份开始日期
        while (current <= endDate) {
            //从当前的 current 日期中，提取“年份”和“这一年的第几天”。
            val key = current.get(Calendar.YEAR) to current.get(Calendar.DAY_OF_YEAR)
            //拿着刚才生成的 key，去 latestPerDay 这个 Map 里查找。
            val record = latestPerDay[key]
            val bmi = record?.bmi?.toFloat()
            //创建一个 DayBmiData 对象，日期是 current 的克隆，BMI 值是刚取出的 bmi
            result.add(DayBmiData(current.clone() as Calendar, bmi))
            current.add(Calendar.DAY_OF_YEAR, 1)//加一天
        }
        return result
    }

    private fun buildWeightDayRangeData(startDate: Calendar, endDate: Calendar, records: List<BmiRecord>): List<DayWeightData> {
        val latestPerDay = records.groupBy { record ->
            val cal = Calendar.getInstance().apply { timeInMillis = record.timestamp }
            cal.get(Calendar.YEAR) to cal.get(Calendar.DAY_OF_YEAR)
        }.mapValues { (_, list) -> list.maxByOrNull { it.timestamp } }

        val result = mutableListOf<DayWeightData>()
        val current = startDate.clone() as Calendar
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
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)//返回数字 1 ~ 7对应周1-周日
        val diff = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY//计算差值
        cal.add(Calendar.DAY_OF_YEAR, -diff)//找到周1
        return cal
    }
}