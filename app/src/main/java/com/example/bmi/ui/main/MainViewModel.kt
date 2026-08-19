package com.example.bmi.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bmi.data.repository.BmiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: BmiRepository
) : ViewModel() {

    private val _isBottomBarVisible = MutableStateFlow(false)
    val isBottomBarVisible: StateFlow<Boolean> = _isBottomBarVisible.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeLatestRecord()
                .collect { record ->
                    _isBottomBarVisible.value = record != null
                }
        }
    }
}