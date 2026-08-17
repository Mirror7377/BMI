package com.example.bmi.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bmi.data.repository.BmiRepository
import com.example.bmi.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: BmiRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _currentScreen = MutableStateFlow(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

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

    fun setInitialScreen(hasData: Boolean) {
        _currentScreen.value = if (hasData) Screen.Display else Screen.Home
    }

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value == screen) return
        _currentScreen.value = screen
    }

    fun navigateToHome() {
        _currentScreen.value = Screen.Home
    }

    fun onResume() {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val target = prefs.getString("post_save_target", null) ?: return
        prefs.edit().remove("post_save_target").apply()

        val targetScreen = when (target) {
            "display" -> Screen.Display
            "statistics" -> Screen.Statistics
            else -> null
        }
        targetScreen?.let { _currentScreen.value = it }
    }
}