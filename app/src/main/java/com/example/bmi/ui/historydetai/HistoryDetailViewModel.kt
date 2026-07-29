package com.example.bmi.ui.historydetai

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bmi.data.repository.BmiRepository
import com.example.bmi.ui.bmigauge.BmiClassifier
import com.example.bmi.utils.RecommendationUtils
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
            try {
                val record = repository.getRecordById(id)
                if (record != null) {
                    val level = if (record.age > 20) {
                        BmiClassifier.classifyAdult(record.bmi)
                    } else {
                        BmiClassifier.classifyChild(record.age, record.gender, record.bmi)
                    }
                    val apps = RecommendationUtils.getRecommendedApps(level, record.gender)
                    _state.update {
                        it.copy(
                            recordId = id,
                            bmi = record.bmi,
                            weightInput = record.weightInput,
                            weightUnit = record.weightUnit,
                            heightUnit = record.heightUnit,
                            feet = record.feetInput ?: 0,
                            inches = record.inchesInput ?: 0,
                            age = record.age,
                            gender = record.gender,
                            heightCm = record.heightCm,
                            bmiLevel = level,
                            recommendedApps = apps,
                            timestamp = record.timestamp,
                            timeOfDay = record.timeOfDay
                        )
                    }
                } else {
                    Log.w("HistoryDetailViewModel", "Record not found")
                }
            } catch (e: Exception) {
                Log.e("HistoryDetailViewModel", "Error loading detail: ${e.message}")
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
                Log.w("HistoryDetailViewModel", "No record to delete")
            }
        }
    }

    private fun navigateBack() {
        viewModelScope.launch {
            _effect.emit(HistoryDetailEffect.NavigateBack)
        }
    }
}