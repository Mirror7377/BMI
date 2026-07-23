package com.example.bmi.ui.result

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bmi.R
import com.example.bmi.data.database.BmiRecord
import com.example.bmi.data.database.RecommendApp
import com.example.bmi.data.repository.BmiRepository
import com.example.bmi.ui.home.enums.Gender
import com.example.bmi.ui.home.enums.WeightUnit
import com.example.bmi.ui.bmigauge.BmiClassifier
import com.example.bmi.ui.bmigauge.BmiLevel
import com.example.bmi.utils.UnitConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val repository: BmiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ResultState())
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<ResultEffect>()
    val effect = _effect.asSharedFlow()

    // ====================== 入口 从 Intent 加载（计算后跳转） ======================
    fun initData(bundle: Bundle) {
        loadFromArguments(bundle)

        // 异步查询数据库是否有历史记录
        viewModelScope.launch {
            val hasRecord = repository.hasAnyRecord()
            _state.update { it.copy(hasSavedRecord = hasRecord) }
        }
    }


    // ====================== 保存记录 ======================
    fun saveRecord() {
        viewModelScope.launch {
            // 先获取当前记录数，判断是否第一次保存
            val countBefore = repository.getRecordCount()
            val isFirstSave = countBefore == 0

            val currentState = _state.value
            val finalWeightKg = if (currentState.weightUnit == WeightUnit.KG.name) {
                currentState.weightInput
            } else {
                UnitConverter.lbToKg(currentState.weightInput)
            }
            val bmiLevel = BmiClassifier.classifyAdult(currentState.bmi)
            val currentTs = currentState.timestamp
            val timeOfDay = currentState.timeOfDay

            val record = BmiRecord(
                weightInput = currentState.weightInput,
                weightUnit = currentState.weightUnit,
                heightInput = currentState.heightInput,
                heightUnit = currentState.heightUnit,
                feetInput = currentState.feet,
                inchesInput = currentState.inches,
                weightKg = finalWeightKg,
                heightCm = currentState.heightCm,
                timestamp = currentTs,
                timeOfDay = timeOfDay,
                age = currentState.age,
                gender = currentState.gender,
                bmi = currentState.bmi,
                category = bmiLevel.statusText,
                createTime = System.currentTimeMillis()
            )
            repository.saveRecord(record)
            _effect.emit(ResultEffect.NavigateToHome(isFirstSave))
        }
    }

    // ====================== 内部方法 ======================
    private fun loadFromArguments(args: Bundle) {
        val record = args.getParcelable("BMI_RECORD", BmiRecord::class.java) ?: return

        _state.update {
            it.copy(
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
                timestamp = record.timestamp,
                timeOfDay = record.timeOfDay
            )
        }
        updateDerivedState()
    }

    private fun updateDerivedState() {
        val currentState = _state.value
        val level = if (currentState.age > 20) {
            BmiClassifier.classifyAdult(currentState.bmi)
        } else {
            // 直接传入 String 性别（state.gender 就是 String）
            BmiClassifier.classifyChild(currentState.age, currentState.gender, currentState.bmi)
        }
        val recommendedApps = getRecommendedApps(level, currentState.gender)
        _state.update {
            it.copy(
                bmiLevel = level,
                recommendedApps = recommendedApps
            )
        }
    }

    // ---------- 10 个 App 数据 ----------
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

    // ---------- 推荐算法 ----------
    fun getRecommendedApps(bmiLevel: BmiLevel, gender: String): List<RecommendApp> {
        // 判断是否为 Normal 以下（体重过轻）
        // BmiLevel 的 ordinal 顺序: VERY_SEVERELY_UNDERWEIGHT(0), SEVERELY_UNDERWEIGHT(1), UNDERWEIGHT(2), NORMAL(3), ...
        val isUnderNormal = bmiLevel.ordinal < BmiLevel.NORMAL.ordinal

        val ids = if (isUnderNormal) {
            // A. 结论为 normal 以下（体重过轻）
            val pool12 = listOf(6, 7, 8).shuffled().take(2)
            val pool3 = listOf(5, 9, 10).shuffled().first()
            pool12 + listOf(pool3)
        } else {
            // B. 结论为 normal 及以上
            val pool12 = if (gender == Gender.MALE.name) {
                listOf(2, 3, 6, 7, 8).shuffled().take(2)
            } else {
                listOf(1, 3, 6, 7, 8).shuffled().take(2)
            }
            val pool3 = listOf(4, 5, 9, 10).shuffled().first()
            pool12 + listOf(pool3)
        }

        // 根据 id 查找对应的 RecommendApp
        return ids.mapNotNull { id -> allApps.find { it.id == id } }
    }
}