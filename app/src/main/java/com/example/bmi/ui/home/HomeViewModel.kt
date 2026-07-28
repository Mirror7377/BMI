package com.example.bmi.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bmi.data.database.BmiRecord
import com.example.bmi.data.enums.Gender
import com.example.bmi.data.enums.HeightUnit
import com.example.bmi.data.enums.TimeOfDay
import com.example.bmi.data.enums.WeightUnit
import com.example.bmi.data.repository.BmiRepository
import com.example.bmi.ui.bmigauge.BmiClassifier
import com.example.bmi.utils.UnitConverter
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
class HomeViewModel @Inject constructor(
    private val repository: BmiRepository
) : ViewModel() {

    // ---------- State ----------
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    // ---------- 副作用（导航事件） ----------
    private val _effect = MutableSharedFlow<HomeEffect>()
    val effect: SharedFlow<HomeEffect> = _effect.asSharedFlow()

    // ---------- Intent 处理 ----------
    fun sendIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.WeightChanged -> onWeightChanged(intent.value)
            is HomeIntent.WeightUnitChanged -> onWeightUnitChanged(intent.unit)
            is HomeIntent.HeightCmChanged -> onHeightCmChanged(intent.value)
            is HomeIntent.HeightUnitChanged -> onHeightUnitChanged(intent.unit)
            is HomeIntent.FeetChanged -> onFeetChanged(intent.feet)
            is HomeIntent.InchesChanged -> onInchesChanged(intent.inches)
            is HomeIntent.AgeChanged -> onAgeChanged(intent.age)
            is HomeIntent.GenderSelected -> onGenderSelected(intent.gender)
            is HomeIntent.TimeChanged -> onTimeChanged(intent.timestamp, intent.timeOfDay)
            HomeIntent.Calculate -> calculate()
        }
    }


    private fun onWeightChanged(value: Double) {
        val (min, max) = when (_state.value.weightUnit) {
            WeightUnit.KG -> 1.0 to 250.0
            WeightUnit.LB -> 2.0 to 551.0
        }
        val clamped = value.coerceIn(min, max)
        val kgValue = if (_state.value.weightUnit == WeightUnit.KG) {
            clamped
        } else {
            UnitConverter.lbToKg(clamped)
        }
        updateState {
            copy(
                weightInput = clamped,
                weightKg = kgValue
            )
        }
    }

    private fun onWeightUnitChanged(unit: WeightUnit) {
        val state = _state.value
        if (state.weightUnit == unit) return

        // 将当前 weightInput 从旧单位换算到新单位（保留原始输入精度）
        val newWeightInput = if (unit == WeightUnit.KG) {
            UnitConverter.lbToKg(state.weightInput).coerceIn(1.0, 250.0)
        } else {
            UnitConverter.kgToLb(state.weightInput).coerceIn(2.0, 551.0)
        }
        // 换算后的值用于 详细页图标的显示
        val newWeightKg = if (unit == WeightUnit.KG) newWeightInput else UnitConverter.lbToKg(newWeightInput)

        updateState {
            copy(
                weightUnit = unit,
                weightInput = newWeightInput,
                weightKg = newWeightKg
            )
        }
    }

    private fun onHeightCmChanged(value: Double) {
        val clamped = value.coerceIn(1.0, 250.0)
        updateState {
            copy(
                heightCm = clamped,
                feetInput = UnitConverter.cmToFeet(clamped),
                inchesInput = UnitConverter.cmToInches(clamped)
            )
        }
    }

    private fun onHeightUnitChanged(unit: HeightUnit) {
        val currentCm = _state.value.heightCm
        val feetInput = _state.value.feetInput
        val inchesInput = _state.value.inchesInput
        if (unit == HeightUnit.CM) {
            var heightCm = UnitConverter.feetInchToCm(feetInput, inchesInput)
            updateState {
                copy(heightUnit = unit,
                    heightCm = heightCm
                ) }
        } else { // FT_IN
            val rawFeet = UnitConverter.cmToFeet(currentCm)
            val rawInches = UnitConverter.cmToInches(currentCm)

            val feet = rawFeet.coerceIn(1, 8) //限制为1-8
            //限制英寸
            val inches = when {
                rawFeet < 1 -> 0                 // 不足 1 英尺，强制 1'0"
                feet == 8  -> rawInches.coerceIn(0, 2)  // 8 英尺时英寸上限 2
                else       -> rawInches.coerceIn(0, 11)
            }

            updateState {
                copy(
                    heightUnit = unit,
                    feetInput = feet,
                    inchesInput = inches
                )
            }
        }
    }

    private fun onFeetChanged(feet: Int) {
        val clamped = feet.coerceIn(1, 8)
        var currentInches = _state.value.inchesInput
        // 若英尺为8且英寸超过2，自动修正为2
        if (clamped == 8 && currentInches > 2) {
            currentInches = 2
        }
        val cm = UnitConverter.feetInchToCm(clamped, currentInches)
        updateState {
            copy(
                feetInput = clamped,
                inchesInput = currentInches,
                heightCm = cm
            )
        }
    }

    private fun onInchesChanged(inches: Int) {
        val currentFeet = _state.value.feetInput
        val maxInches = if (currentFeet == 8) 2 else 11
        val clamped = inches.coerceIn(0, maxInches)
        val cm = UnitConverter.feetInchToCm(currentFeet, clamped)
        updateState {
            copy(
                inchesInput = clamped,
                feetInput = currentFeet,
                heightCm = cm
            )
        }
    }

    private fun onAgeChanged(age: Int) {
        val clamped = age.coerceIn(2, 99)
        updateState { copy(age = clamped) }
    }

    private fun onGenderSelected(gender: Gender) {
        updateState { copy(gender = gender) }
    }

    private fun onTimeChanged(timestamp: Long, timeOfDay: TimeOfDay) {
        updateState {
            copy(
                timestamp = timestamp,
                timeOfDay = timeOfDay,
            )
        }
    }

    private fun calculate() {
        val state = _state.value

        // 计算BMI
        val bmi = UnitConverter.calculateBmi(
            heightUnit = state.heightUnit,
            feetInput = state.feetInput,
            inchesInput = state.inchesInput,
            heightCm = state.heightCm,
            weightUnit = state.weightUnit,
            weightInput = state.weightInput,
            weightKg = state.weightKg
        )

        // 根据年龄分类
        val isAdult = state.age >= 18
        val category = if (isAdult) {
            BmiClassifier.classifyAdult(bmi)
        } else {
            BmiClassifier.classifyChild(state.age, state.gender.name, bmi)
        }

        val record = BmiRecord(
            weightInput = state.weightInput,
            weightUnit = state.weightUnit.name,
            heightUnit = state.heightUnit.name,
            feetInput = if (state.heightUnit == HeightUnit.FT_IN) state.feetInput else null,
            inchesInput = if (state.heightUnit == HeightUnit.FT_IN) state.inchesInput else null,
            weightKg = state.weightKg,
            heightCm = state.heightCm,
            timestamp = state.timestamp,
            timeOfDay = state.timeOfDay.name,
            age = state.age,
            gender = state.gender.name,
            bmi = bmi,
            category = category.toString(),
            createTime = System.currentTimeMillis()
        )



        viewModelScope.launch {
            _effect.emit(HomeEffect.NavigateToResult(record))
        }
    }


    private inline fun updateState(block: HomeState.() -> HomeState) {
        _state.update { it.block() }
    }
}
