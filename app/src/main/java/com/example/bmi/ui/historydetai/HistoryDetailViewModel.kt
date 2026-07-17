package com.example.bmi.ui.historydetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bmi.R
import com.example.bmi.data.database.RecommendApp
import com.example.bmi.data.repository.BmiRepository
import com.example.bmi.ui.home.enums.Gender
import com.example.bmi.ui.bmigauge.BmiClassifier
import com.example.bmi.ui.bmigauge.BmiLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    private val repository: BmiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryDetailState())
    val state: StateFlow<HistoryDetailState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<HistoryDetailEffect>()
    val effect: SharedFlow<HistoryDetailEffect> = _effect.asSharedFlow()

    fun handleIntent(intent: HistoryDetailIntent) {
        when (intent) {
            is HistoryDetailIntent.LoadRecord -> loadRecord(intent.id)
            HistoryDetailIntent.DeleteRecord -> deleteRecord()
            HistoryDetailIntent.BackPressed -> navigateBack()
        }
    }

    private fun loadRecord(id: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val record = repository.getRecordById(id)
                if (record != null) {
                    val level = if (record.age > 20) {
                        BmiClassifier.classifyAdult(record.bmi)
                    } else {
                        BmiClassifier.classifyChild(record.age, record.gender, record.bmi)
                    }
                    val apps = getRecommendedApps(level, record.gender)
                    _state.update {
                        it.copy(
                            recordId = id,
                            bmi = record.bmi,
                            weightInput = record.weightInput,
                            weightUnit = record.weightUnit,
                            heightInput = record.heightInput,
                            heightUnit = record.heightUnit,
                            feet = record.feetInput ?: 0,
                            inches = record.inchesInput ?: 0,
                            age = record.age,
                            gender = record.gender,
                            heightCm = record.heightCm,
                            bmiLevel = level,
                            recommendedApps = apps,
                            timestamp = record.timestamp,
                            timeOfDay = record.timeOfDay,
                            isLoading = false
                        )
                    }
                } else {
                    _effect.emit(HistoryDetailEffect.ShowError("Record not found"))
                    _state.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _effect.emit(HistoryDetailEffect.ShowError(e.message ?: "Unknown error"))
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun deleteRecord() {
        viewModelScope.launch {
            val id = _state.value.recordId
            if (id != 0L) {
                // 1. 执行删除
                repository.deleteRecord(id)

                // 2. 查询是否还有剩余记录
                val remainingCount = repository.getRecordCount()

                // 3. 根据结果发送不同 Effect
                if (remainingCount == 0) {
                    _effect.emit(HistoryDetailEffect.NavigateToHome)
                } else {
                    _effect.emit(HistoryDetailEffect.NavigateBack)
                }
            } else {
                _effect.emit(HistoryDetailEffect.ShowError("No record to delete"))
            }
        }
    }

    private fun navigateBack() {
        viewModelScope.launch {
            _effect.emit(HistoryDetailEffect.NavigateBack)
        }
    }

    // ---------- 推荐算法（与 ResultViewModel 完全一致） ----------
    private val allApps = listOf(
        RecommendApp(
            id = 1,
            name = "Lose Weight App for Women - Workout at Home",
            category = "Home Workout for Men & Women",
            rating = 4.9,
            iconResId = R.drawable.ic_app_1,
            packageName = "loseweightapp.loseweightappforwomen.womenworkoutathome"
        ),
        RecommendApp(
            id = 2,
            name = "Lose Weight App for Men - Weight Loss in 30 Days",
            category = "Bodyweight Fitness ＆ Training",
            rating = 5.0,
            iconResId = R.drawable.ic_app_2,
            packageName = "menloseweight.loseweightappformen.weightlossformen"
        ),
        RecommendApp(
            id = 3,
            name = "Lose Weight at Home - Home Workout in 30 Days",
            category = "Home Workout for Weight Loss",
            rating = 4.9,
            iconResId = R.drawable.ic_app_3,
            packageName = "loseweight.weightloss.workout.fitness"
        ),
        RecommendApp(
            id = 4,
            name = "Fasting App - Fasting Tracker & Intermittent Fast",
            category = "Home Workout for Women, Fit",
            rating = 4.9,
            iconResId = R.drawable.ic_app_4,
            packageName = "bodyfast.zero.fastingtracker.weightloss"
        ),
        RecommendApp(
            id = 5,
            name = "Walking App - Walking for Weight Loss",
            category = "Weight Loss, Lose Belly Fat",
            rating = 4.6,
            iconResId = R.drawable.ic_app_5,
            packageName = "walking.weightloss.walk.tracker"
        ),
        RecommendApp(
            id = 6,
            name = "Home Workout - No Equipment",
            category = "Simple Fast Lose Weight Diet",
            rating = 4.9,
            iconResId = R.drawable.ic_app_6,
            packageName = "homeworkout.homeworkouts.noequipment"
        ),
        RecommendApp(
            id = 7,
            name = "30 Day Fitness Challenge - Workout at Home",
            category = "Walking Tracker & Pedometer",
            rating = 4.7,
            iconResId = R.drawable.ic_app_7,
            packageName = "com.popularapp.thirtydayfitnesschallenge"
        ),
        RecommendApp(
            id = 8,
            name = "Six Pack in 30 Days - Abs Workout",
            category = "6 Pack, Core, Abs Exercise",
            rating = 5.0,
            iconResId = R.drawable.ic_app_8,
            packageName = "sixpack.sixpackabs.absworkout"
        ),
        RecommendApp(
            id = 9,
            name = "Step Tracker - Pedometer Free & Calorie Tracker",
            category = "Step Counter, Weight Loss",
            rating = 4.9,
            iconResId = R.drawable.ic_app_9,
            packageName = "steptracker.healthandfitness.walkingtracker.pedometer"
        ),
        RecommendApp(
            id = 10,
            name = "Blood Pressure Monitor - Blood Pressure App",
            category = "Bodyweight Fitness ＆ Training",
            rating = 4.7,
            iconResId = R.drawable.ic_app_10,
            packageName = "bloodpressuremonitor.bloodpressureapp.bpmonitor"
        )
    )

    private fun getRecommendedApps(bmiLevel: BmiLevel, gender: String): List<RecommendApp> {
        val isUnderNormal = bmiLevel.ordinal < BmiLevel.NORMAL.ordinal
        val ids = if (isUnderNormal) {
            val pool12 = listOf(6, 7, 8).shuffled().take(2)
            val pool3 = listOf(5, 9, 10).shuffled().first()
            pool12 + listOf(pool3)
        } else {
            val pool12 = if (gender == Gender.MALE.name) {
                listOf(2, 3, 6, 7, 8).shuffled().take(2)
            } else {
                listOf(1, 3, 6, 7, 8).shuffled().take(2)
            }
            val pool3 = listOf(4, 5, 9, 10).shuffled().first()
            pool12 + listOf(pool3)
        }
        return ids.mapNotNull { id -> allApps.find { it.id == id } }
    }
}