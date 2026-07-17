package com.example.bmi.ui.recent

import com.example.bmi.data.database.BmiRecord

data class RecentState(
    val records: List<BmiRecord> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)