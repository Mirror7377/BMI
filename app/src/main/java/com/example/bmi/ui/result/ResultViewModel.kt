package com.example.bmi.ui.result

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bmi.data.database.BmiRecord
import com.example.bmi.data.repository.BmiRepository
import com.example.bmi.ui.home.enums.Gender
import com.example.bmi.ui.home.enums.HeightUnit
import com.example.bmi.ui.home.enums.WeightUnit
import com.example.bmi.utils.BmiClassifier
import com.example.bmi.utils.BmiLevel
import com.example.bmi.utils.UnitConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val repository: BmiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ResultState())
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<ResultEffect>()
    val effect = _effect.asSharedFlow()

    // 从 Intent 加载数据（计算后跳转）
    fun initData(bundle: Bundle) {
        loadFromArguments(bundle)
    }


    fun saveRecord() {
        viewModelScope.launch {
            val currentState = _state.value
            val finalWeightKg = if (currentState.weightUnit == WeightUnit.KG.name) {
                currentState.weightInput
            } else {
                UnitConverter.lbToKg(currentState.weightInput)
            }
            val bmiLevel = BmiClassifier.classifyAdult(currentState.bmi)
            val currentTs = System.currentTimeMillis()
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val timeOfDay = when (hour) {
                in 6..11 -> "Morning"
                in 12..17 -> "Afternoon"
                else -> "Evening"
            }

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
                category = bmiLevel.statusText
            )
            repository.saveRecord(record)
            _effect.emit(ResultEffect.NavigateToHome)
        }
    }

    // 内部方法
    private fun loadFromArguments(args: Bundle) {
        val bmi = args.getDouble("KEY_BMI", 0.0)
        val weightInput = args.getDouble("KEY_WEIGHT_INPUT", 0.0)
        val weightUnit = args.getString("KEY_WEIGHT_UNIT") ?: WeightUnit.KG.name
        val heightInput = args.getDouble("KEY_HEIGHT_INPUT", 0.0)
        val heightUnit = args.getString("KEY_HEIGHT_UNIT") ?: HeightUnit.CM.name
        val feet = args.getInt("KEY_FEET", 0)
        val inches = args.getInt("KEY_INCHES", 0)
        val age = args.getInt("KEY_AGE", 0)
        val gender = args.getString("KEY_GENDER") ?: Gender.MALE.name
        val heightCm = args.getDouble("KEY_HEIGHT_CM", 0.0)

        _state.update {
            it.copy(
                bmi = bmi,
                weightInput = weightInput,
                weightUnit = weightUnit,
                heightInput = heightInput,
                heightUnit = heightUnit,
                feet = feet,
                inches = inches,
                age = age,
                gender = gender,
                heightCm = heightCm
            )
        }
        updateDerivedState()
    }

    private fun updateDerivedState() {
        val currentState = _state.value
        val level = BmiClassifier.classifyAdult(currentState.bmi)
        _state.update { it.copy(bmiLevel = level) }
    }
}