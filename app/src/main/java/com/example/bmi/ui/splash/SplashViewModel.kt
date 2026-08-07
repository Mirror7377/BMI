package com.example.bmi.ui.splash

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
class SplashViewModel @Inject constructor(
    private val repository: BmiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SplashState())
    val state: StateFlow<SplashState> = _state.asStateFlow()

    fun checkData() {
        viewModelScope.launch {
            val hasData = repository.getRecordCount() != 0
            _state.value = SplashState(hasData = hasData, isReady = true)
        }
    }
}

data class SplashState(
    val hasData: Boolean = false,
    val isReady: Boolean = false
)