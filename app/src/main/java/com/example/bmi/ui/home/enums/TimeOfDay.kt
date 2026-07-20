package com.example.bmi.ui.home.enums


import java.util.Calendar

enum class TimeOfDay(val displayName: String) {
    MORNING("Morning"),//displayName属性
    AFTERNOON("Afternoon"),
    EVENING("Evening"),
    NIGHT("Night");

    companion object {
        fun fromSystemTime(): TimeOfDay {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return when (hour) {
                in 8..13 -> MORNING
                in 14..18 -> AFTERNOON
                in 19..22 -> EVENING
                else -> NIGHT
            }
        }
    }
}