package com.example.bmi.ui.historydetai

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bmi.R
import com.example.bmi.data.repository.BmiRepository
import com.example.bmi.ui.bmigauge.BmiClassifier
import com.example.bmi.utils.AppEventBus
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
    private val repository: BmiRepository,
    private val appEventBus: AppEventBus
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryDetailState())
    val state: StateFlow<HistoryDetailState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<HistoryDetailEffect>()
    val effect: SharedFlow<HistoryDetailEffect> = _effect.asSharedFlow()

    fun handleIntent(intent: HistoryDetailIntent) {
        when (intent) {
            is HistoryDetailIntent.LoadRecord -> loadRecord(intent.id)
            is HistoryDetailIntent.DeleteRecord -> deleteRecord()
            is HistoryDetailIntent.ShowBmiLegend -> showBmiLegend()
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
                    val apps = RecommendationUtils.getRecommendedApps(level, record.gender)  // 新增
                    _state.update {
                        it.copy(
                            record = record,
                            bmiLevel = level,
                            recommendedApps = apps  // 新增
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("HistoryDetailViewModel", "Error loading detail: ${e.message}")
            }
        }
    }

    fun deleteRecord() {
        viewModelScope.launch {
            val id = _state.value.record?.id ?: return@launch
            if (id != 0L) {
                repository.deleteRecord(id)
                val remainingCount = repository.getRecordCount()

                // 发送全局 Banner 事件
                appEventBus.showBanner(
                    R.drawable.check_circle,
                    "Deleted successfully."
                )

                if (remainingCount == 0) {
                    _effect.emit(HistoryDetailEffect.NavigateToHome)
                } else {
                    _effect.emit(HistoryDetailEffect.NavigateBack)
                }
            }
        }
    }

    private fun showBmiLegend() {
        viewModelScope.launch {
            _effect.emit(HistoryDetailEffect.ShowBmiLegend)
        }
    }
}