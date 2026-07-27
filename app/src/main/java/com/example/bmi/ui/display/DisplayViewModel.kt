package com.example.bmi.ui.display

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bmi.data.repository.BmiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DisplayViewModel @Inject constructor(
    private val repository: BmiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DisplayState())
    val state: StateFlow<DisplayState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<DisplayEffect>()
    val effect: SharedFlow<DisplayEffect> = _effect.asSharedFlow()

    fun handleIntent(intent: DisplayIntent) {
        when (intent) {
            DisplayIntent.LoadLatest -> loadLatestRecord()
            is DisplayIntent.NavigateTo -> navigateTo(intent.destination)
        }
    }

    private fun loadLatestRecord() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.observeLatestRecord()
                .collect { record ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            record = record,
                            error = null
                        )
                    }
                }
        }
    }

    private fun navigateTo(destination: DisplayIntent.Destination) {
        viewModelScope.launch {
            _effect.emit(DisplayEffect.NavigateTo(destination))
        }
    }

    init {
        handleIntent(DisplayIntent.LoadLatest)
    }
}