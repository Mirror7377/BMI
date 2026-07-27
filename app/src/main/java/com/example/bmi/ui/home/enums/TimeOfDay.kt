package com.example.bmi.ui.home.enums

import com.example.bmi.R
import java.util.Calendar

enum class TimeOfDay(val displayName: Int) {
    MORNING(R.string.time_morning),
    AFTERNOON(R.string.time_afternoon),
    EVENING(R.string.time_evening),
    NIGHT(R.string.time_night);

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