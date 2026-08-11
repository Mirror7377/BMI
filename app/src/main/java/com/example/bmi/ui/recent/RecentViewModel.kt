package com.example.bmi.ui.recent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bmi.data.repository.BmiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecentViewModel @Inject constructor(
    private val repository: BmiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RecentState())
    val state: StateFlow<RecentState> = _state.asStateFlow()

    fun handleIntent(intent: RecentIntent) {
        when (intent) {
            is RecentIntent.LoadRecords -> loadRecords()
        }
    }

    private fun loadRecords() {
        viewModelScope.launch {
            repository.observeAllRecords().collect { recordList ->
                _state.update {
                    it.copy(records = recordList)
                }
            }
        }
    }
}