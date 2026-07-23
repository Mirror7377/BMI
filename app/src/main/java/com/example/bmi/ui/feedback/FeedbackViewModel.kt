package com.example.bmi.ui.feedback

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class FeedbackViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(FeedbackState())
    val state: StateFlow<FeedbackState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<FeedbackEffect>()
    val effect: SharedFlow<FeedbackEffect> = _effect.asSharedFlow()

    fun handleIntent(intent: FeedbackIntent) {
        when (intent) {
            is FeedbackIntent.Init -> init()
            else -> {}
        }
    }

    private fun init() {
        // 暂不实现
    }
}