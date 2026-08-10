package com.example.bmi.ui.feedback

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.content.edit

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(FeedbackState())
    val state: StateFlow<FeedbackState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<FeedbackEffect>()
    val effect: SharedFlow<FeedbackEffect> = _effect.asSharedFlow()

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun handleIntent(intent: FeedbackIntent) {
        when (intent) {
            is FeedbackIntent.UpdateFeedbackText -> updateFeedbackText(intent.text)
            is FeedbackIntent.SubmitFeedback -> submitFeedback()
            is FeedbackIntent.NavigateBack -> navigateBack()
        }
    }

    private fun updateFeedbackText(text: String) {
        _state.update {
            it.copy(
                feedbackText = text,
                isSubmitEnabled = text.isNotBlank()
            )
        }
    }

    private fun submitFeedback() {
        val currentState = _state.value
        if (!currentState.isSubmitEnabled || currentState.isLoading) return

        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                // 保存到 SharedPreferences
                prefs.edit {
                    putString("feedback_content", currentState.feedbackText)
                }

                // 发送导航效果
                _effect.emit(FeedbackEffect.NavigateBack)
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun navigateBack() {
        viewModelScope.launch {
            _effect.emit(FeedbackEffect.NavigateBack)
        }
    }
}