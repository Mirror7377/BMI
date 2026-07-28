package com.example.bmi.ui.home

import com.example.bmi.data.database.BmiRecord

sealed class HomeEffect {
    data class NavigateToResult(val record: BmiRecord) : HomeEffect()
}