package com.example.bmi.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bmi.data.database.BmiRecord
import com.example.bmi.data.repository.BmiRepository
import com.example.bmi.ui.home.enums.*
import com.example.bmi.utils.BmiClassifier
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
import java.text.SimpleDateFormat
import java.util.*
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
            HomeIntent.Init -> init()
            is HomeIntent.WeightChanged -> onWeightChanged(intent.value)
            is HomeIntent.WeightUnitChanged -> onWeightUnitChanged(intent.unit)
            is HomeIntent.HeightChanged -> onHeightChanged(intent.value)
            is HomeIntent.HeightUnitChanged -> onHeightUnitChanged(intent.unit)
            is HomeIntent.FeetChanged -> onFeetChanged(intent.feet)
            is HomeIntent.InchesChanged -> onInchesChanged(intent.inches)
            is HomeIntent.AgeChanged -> onAgeChanged(intent.age)
            is HomeIntent.GenderSelected -> onGenderSelected(intent.gender)
            is HomeIntent.TimeChanged -> onTimeChanged(intent.timestamp, intent.timeOfDay)
            HomeIntent.Calculate -> calculate()
        }
    }

    // ---------- 各处理函数 ----------
    private fun init() {
        // 初始化时自动设置当前时间
        val now = System.currentTimeMillis()
        val timeOfDay = TimeOfDay.fromSystemTime()
        updateState {
            copy(
                timestamp = now,
                timeOfDay = timeOfDay,
                timeDisplay = formatTime(now, timeOfDay)
            )
        }
        // 同步更新派生字段（体重/身高显示）
        refreshDisplayValues()
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
                weightKg = kgValue,
                weightDisplay = String.format("%.2f", clamped)
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
        // 换算后的值用于显示，同时更新 weightKg 和 weightInput
        val newWeightKg = if (unit == WeightUnit.KG) newWeightInput else UnitConverter.lbToKg(newWeightInput)

        updateState {
            copy(
                weightUnit = unit,
                weightInput = newWeightInput,
                weightKg = newWeightKg,
                weightDisplay = String.format("%.2f", newWeightInput)
            )
        }
    }

    private fun onHeightChanged(value: Double) {
        //只限定cm
        val clamped = value.coerceIn(1.0, 250.0)
        updateState {
            copy(
                heightCm = clamped,
                heightInput = clamped
            )
        }
        refreshDisplayValues()
    }

    private fun onHeightUnitChanged(unit: HeightUnit) {
        val currentCm = _state.value.heightCm
        if (unit == HeightUnit.CM) {
            updateState { copy(heightUnit = unit, heightInput = currentCm) }
        } else { // FT_IN
            val rawFeet = UnitConverter.cmToFeet(currentCm)
            val rawInches = UnitConverter.cmToInches(currentCm)

            val feet = rawFeet.coerceIn(1, 8)
            val inches = when {
                rawFeet < 1 -> 0                 // 不足 1 英尺，强制 1'0"
                feet == 8  -> rawInches.coerceIn(0, 2)  // 8 英尺时英寸上限 2
                else       -> rawInches.coerceIn(0, 11)
            }

            val newHeightCm = UnitConverter.feetInchToCm(feet, inches)
            updateState {
                copy(
                    heightUnit = unit,
                    feetInput = feet,
                    inchesInput = inches,
                    heightInput = (feet * 12 + inches).toDouble(),
                    heightCm = newHeightCm
                )
            }
        }
        refreshDisplayValues()
    }

    private fun onFeetChanged(feet: Int) {
        val clamped = feet.coerceIn(1, 8)
        var currentInches = _state.value.inchesInput
        // 若英尺为8且英寸超过2，自动修正为2
        if (clamped == 8 && currentInches > 2) {
            currentInches = 2
        }
        val cm = UnitConverter.feetInchToCm(clamped, currentInches)
        val totalInches = (clamped * 12 + currentInches).toDouble()
        updateState {
            copy(
                feetInput = clamped,
                inchesInput = currentInches,
                heightCm = cm,
                heightInput = totalInches
            )
        }
    }

    private fun onInchesChanged(inches: Int) {
        val currentFeet = _state.value.feetInput
        val maxInches = if (currentFeet >= 8) 2 else 11
        val clamped = inches.coerceIn(0, maxInches)
        val cm = UnitConverter.feetInchToCm(currentFeet, clamped)
        val totalInches = (currentFeet * 12 + clamped).toDouble()
        updateState {
            copy(
                inchesInput = clamped,
                heightCm = cm,
                heightInput = totalInches
            )
        }
    }

    private fun onAgeChanged(age: Int) {
        val clamped = age.coerceIn(2, 100)
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
                timeDisplay = formatTime(timestamp, timeOfDay)
            )
        }
    }

    private fun calculate() {
        val state = _state.value
        val bmi: Double

        when {
            // ① cm + kg：BMI = weightKg / (heightM)^2
            state.heightUnit == HeightUnit.CM && state.weightUnit == WeightUnit.KG -> {
                val heightM = state.heightCm / 100.0
                bmi = state.weightKg / (heightM * heightM)
            }
            // ② ft-in + lb：BMI = weightLb / (totalInches)^2 * 703
            state.heightUnit == HeightUnit.FT_IN && state.weightUnit == WeightUnit.LB -> {
                val totalInches = state.feetInput * 12.0 + state.inchesInput
                bmi = state.weightInput / (totalInches * totalInches) * 703.0
            }
            // ③ ft-in + kg：BMI = weightKg / (ft*0.3048 + in*0.0254)^2
            state.heightUnit == HeightUnit.FT_IN && state.weightUnit == WeightUnit.KG -> {
                val heightM = state.feetInput * 0.3048 + state.inchesInput * 0.0254
                bmi = state.weightKg / (heightM * heightM)
            }
            // ④ cm + lb：BMI = (weightLb * 0.45359237) / (heightM)^2
            state.heightUnit == HeightUnit.CM && state.weightUnit == WeightUnit.LB -> {
                val weightKg = state.weightInput * 0.45359237
                val heightM = state.heightCm / 100.0
                bmi = weightKg / (heightM * heightM)
            }
            else -> {
                // 兜底：使用标准值计算
                val heightM = state.heightCm / 100.0
                bmi = state.weightKg / (heightM * heightM)
            }
        }

        // 根据年龄分类
        val isAdult = state.age >= 18
        val category = if (isAdult) {
            BmiClassifier.classifyAdult(bmi)
        } else {
            BmiClassifier.classifyChild(state.age, state.gender, bmi)
        }

        val record = BmiRecord(
            weightInput = state.weightInput,
            weightUnit = state.weightUnit.name,
            heightInput = state.heightInput,
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
            category = category.toString()
        )

        viewModelScope.launch {
            repository.saveRecord(record)
            _effect.emit(HomeEffect.NavigateToResult(record))
        }
    }

    // ---------- 辅助函数，格式化为ui需要的格式 ----------
    private fun refreshDisplayValues() {
        val state = _state.value

        // 体重显示：直接使用 weightInput（当前单位值）
        val weightDisplay = String.format("%.2f", state.weightInput)

        // 身高显示
        val heightDisplay = if (state.heightUnit == HeightUnit.CM) {
            String.format("%.1f", state.heightInput)  // heightInput 已是 cm 值
        } else {
            // ft-in 模式：使用 feetInput 和 inchesInput
            "${state.feetInput}' ${state.inchesInput}\""
        }


        // 更新时间显示
        val timeDisplay = formatTime(state.timestamp, state.timeOfDay)

        updateState {
            copy(
                weightDisplay = weightDisplay,
                heightDisplay = heightDisplay,
                timeDisplay = timeDisplay
            )
        }
    }


    private fun formatTime(timestamp: Long, timeOfDay: TimeOfDay): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateStr = sdf.format(Date(timestamp))
        return "$dateStr ${timeOfDay.displayName}"
    }

    private inline fun updateState(block: HomeState.() -> HomeState) {
        _state.update { it.block() }
    }
}
sealed class HomeEffect {
    data class NavigateToResult(val record: com.example.bmi.data.database.BmiRecord) : HomeEffect()
    data class ShowError(val message: String) : HomeEffect()
}
