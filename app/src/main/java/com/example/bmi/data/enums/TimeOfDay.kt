package com.example.bmi.data.enums

import com.example.bmi.R
import java.util.Calendar

enum class TimeOfDay(val displayName: Int) {
    Morning(R.string.time_morning),
    Afternoon(R.string.time_afternoon),
    Evening(R.string.time_evening),
    Night(R.string.time_night);

    companion object {
        fun fromSystemTime(): TimeOfDay {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return when (hour) {
                in 8..13 -> Morning
                in 14..18 -> Afternoon
                in 19..22 -> Evening
                else -> Night
            }
        }
    }
}