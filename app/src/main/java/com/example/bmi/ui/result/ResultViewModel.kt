package com.example.bmi.ui.result

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bmi.data.database.BmiRecord
import com.example.bmi.data.enums.WeightUnit
import com.example.bmi.data.repository.BmiRepository
import com.example.bmi.ui.bmigauge.BmiClassifier
import com.example.bmi.utils.RecommendationUtils
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
            val bmiLevel = if (currentState.age > 20) {
                BmiClassifier.classifyAdult(currentState.bmi)
            } else {
                // 直接传入 String 性别（state.gender 就是 String）
                BmiClassifier.classifyChild(currentState.age, currentState.gender, currentState.bmi)
            }
            val currentTs = currentState.timestamp
            val timeOfDay = currentState.timeOfDay

            val record = BmiRecord(
                weightInput = currentState.weightInput,
                weightUnit = currentState.weightUnit,
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
                category = bmiLevel.name,
                createTime = System.currentTimeMillis()
            )
            repository.saveRecord(record)
            _effect.emit(ResultEffect.NavigateToHome(isFirstSave))
        }
    }

    private fun loadFromArguments(args: Bundle) {
        val record = args.getParcelable("BMI_RECORD", BmiRecord::class.java) ?: return

        _state.update {
            it.copy(
                bmi = record.bmi,
                weightInput = record.weightInput,
                weightUnit = record.weightUnit,
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
        val recommendedApps = RecommendationUtils.getRecommendedApps(level, currentState.gender)
        _state.update {
            it.copy(
                bmiLevel = level,
                recommendedApps = recommendedApps
            )
        }
    }
}