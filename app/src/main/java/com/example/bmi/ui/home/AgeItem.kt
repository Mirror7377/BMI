package com.example.bmi.ui.home

sealed class AgeItem {
    object Placeholder : AgeItem()
    data class RealAge(val age: Int) : AgeItem()
}