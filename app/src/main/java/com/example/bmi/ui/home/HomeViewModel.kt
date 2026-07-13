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
        // 用户输入的 value 是当前单位下的值
        val clamped = value.coerceIn(1.0, 250.0)
        // 计算对应的 kg 值（用于存储和计算）
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
        val clamped = value.coerceIn(1.0, 250.0)
        updateState { copy(heightCm = clamped) }
        refreshDisplayValues()
    }

    private fun onHeightUnitChanged(unit: HeightUnit) {
        val currentCm = _state.value.heightCm
        val newHeightInput: Double
        val newFeet: Int
        val newInches: Int

        if (unit == HeightUnit.CM) {
            newHeightInput = currentCm
            newFeet = _state.value.feetInput   // 保留原值，但不会显示，可忽略
            newInches = _state.value.inchesInput
        } else { // FT_IN
            newFeet = UnitConverter.cmToFeet(currentCm)
            newInches = UnitConverter.cmToInches(currentCm)
            newHeightInput = (newFeet * 12 + newInches).toDouble()
        }

        updateState {
            copy(
                heightUnit = unit,
                heightInput = newHeightInput,
                feetInput = newFeet,
                inchesInput = newInches
            )
        }
        refreshDisplayValues()
    }

    private fun onFeetChanged(feet: Int) {
        val clamped = feet.coerceIn(1, 8)
        val currentInches = _state.value.inchesInput
        val cm = UnitConverter.feetInchToCm(clamped, currentInches)
        // 将 totalInches 转为 Double
        val totalInches = (clamped * 12 + currentInches).toDouble()
        updateState {
            copy(
                feetInput = clamped,
                heightCm = cm,
                heightInput = totalInches   // 现在类型匹配
            )
        }
    }

    private fun onInchesChanged(inches: Int) {
        val clamped = inches.coerceIn(0, 11)
        val currentFeet = _state.value.feetInput
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
        val heightM = state.heightCm / 100
        val bmi = state.weightKg / (heightM * heightM)

        // 根据年龄分类
        val isAdult = state.age >= 18
        val category = if (isAdult) {
            BmiClassifier.classifyAdult(bmi)
        } else {
            BmiClassifier.classifyChild(state.age, state.gender, bmi)
        }

        val record = BmiRecord(
            // 原始输入值
            weightInput = state.weightInput,
            weightUnit = state.weightUnit.name,
            heightInput = state.heightInput,
            heightUnit = state.heightUnit.name,
            feetInput = if (state.heightUnit == HeightUnit.FT_IN) state.feetInput else null,
            inchesInput = if (state.heightUnit == HeightUnit.FT_IN) state.inchesInput else null,
            // 标准换算值
            weightKg = state.weightKg,
            heightCm = state.heightCm,
            // 其他字段
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
