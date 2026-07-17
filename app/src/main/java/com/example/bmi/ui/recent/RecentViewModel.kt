package com.example.bmi.ui.recent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bmi.data.repository.BmiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecentViewModel @Inject constructor(
    private val repository: BmiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RecentState())
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<RecentEffect>()
    val effect = _effect.asSharedFlow()

    fun handleIntent(intent: RecentIntent) {
        when (intent) {
            RecentIntent.LoadRecords -> loadRecords()
        }
    }


    private fun loadRecords() {
        viewModelScope.launch {
            repository.observeAllRecords().collect { recordList ->
                _state.update {
                    it.copy(records = recordList, isLoading = false)
                }
            }
        }
    }
}