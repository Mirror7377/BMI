package com.example.bmi.ui.result

import android.os.Bundle
import androidx.core.os.BundleCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bmi.data.database.BmiRecord
import com.example.bmi.data.repository.BmiRepository
import com.example.bmi.ui.bmigauge.BmiClassifier
import com.example.bmi.utils.RecommendationUtils
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
class ResultViewModel @Inject constructor(
    private val repository: BmiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ResultState())
    val state: StateFlow<ResultState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<ResultEffect>()
    val effect: SharedFlow<ResultEffect> = _effect.asSharedFlow()

    fun handleIntent(intent: ResultIntent) {
        when (intent) {
            is ResultIntent.Init -> initData(intent.bundle)
            is ResultIntent.SaveRecord -> saveRecord()
        }
    }

    private fun initData(bundle: Bundle?) {
        if (bundle == null) return

        val record = BundleCompat.getParcelable(bundle, "BMI_RECORD", BmiRecord::class.java) ?: return

        val bmiLevel = if (record.age > 20) {
            BmiClassifier.classifyAdult(record.bmi)
        } else {
            BmiClassifier.classifyChild(record.age, record.gender, record.bmi)
        }

        val recommendedApps = RecommendationUtils.getRecommendedApps(bmiLevel, record.gender)

        _state.update {
            it.copy(
                record = record,
                bmiLevel = bmiLevel,
                recommendedApps = recommendedApps
            )
        }

        viewModelScope.launch {
            val hasSaved = repository.hasAnyRecord()
            _state.update { it.copy(hasSavedRecord = hasSaved) }
        }
    }

    private fun saveRecord() {
        val currentState = _state.value
        val record = currentState.record ?: return

        viewModelScope.launch {
            val countBefore = repository.getRecordCount()
            val isFirstSave = countBefore == 0

            repository.saveRecord(record)
            _effect.emit(ResultEffect.NavigateToHome(isFirstSave))
        }
    }
}