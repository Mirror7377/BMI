package com.example.bmi.data.database

data class RecommendApp(
    val id: Int,
    val name: String,
    val category: String,
    val rating: Double,
    val iconResId: Int,
    val packageName: String
)