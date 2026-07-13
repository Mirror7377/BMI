package com.example.bmi.utils

import com.example.bmi.ui.home.enums.Gender

enum class AdultCategory {
    VERY_SEVERELY_UNDERWEIGHT,
    SEVERELY_UNDERWEIGHT,
    UNDERWEIGHT,
    NORMAL,
    OVERWEIGHT,
    OBESE_CLASS_I,
    OBESE_CLASS_II,
    OBESE_CLASS_III
}

enum class ChildCategory {
    UNDERWEIGHT,
    NORMAL,
    OVERWEIGHT,
    OBESE_CLASS_I
}

object BmiClassifier {
    fun classifyAdult(bmi: Double): AdultCategory {
        return when {
            bmi < 16.0 -> AdultCategory.VERY_SEVERELY_UNDERWEIGHT
            bmi < 17.0 -> AdultCategory.SEVERELY_UNDERWEIGHT
            bmi < 18.5 -> AdultCategory.UNDERWEIGHT
            bmi < 25.0 -> AdultCategory.NORMAL
            bmi < 30.0 -> AdultCategory.OVERWEIGHT
            bmi < 35.0 -> AdultCategory.OBESE_CLASS_I
            bmi < 40.0 -> AdultCategory.OBESE_CLASS_II
            else -> AdultCategory.OBESE_CLASS_III
        }
    }

    fun classifyChild(age: Int, gender: Gender, bmi: Double): ChildCategory {
        val row = getChildRow(age, gender)
        return when {
            bmi < row.underweight -> ChildCategory.UNDERWEIGHT
            bmi < row.normal -> ChildCategory.NORMAL
            bmi < row.overweight -> ChildCategory.OVERWEIGHT
            else -> ChildCategory.OBESE_CLASS_I
        }
    }

    fun getDialRange(age: Int, gender: Gender): Pair<Double, Double> {
        return if (age in 2..20) {
            val row = getChildRow(age, gender)
            Pair(row.dialLow, row.dialHigh)
        } else {
            Pair(15.0, 41.0)  // 成人
        }
    }

    private fun getChildRow(age: Int, gender: Gender): ChildRow {
        val table = if (gender == Gender.MALE) maleTable else femaleTable
        return table[age] ?: table[age.coerceIn(2, 20)] ?: error("Age out of range")
    }

    data class ChildRow(
        val underweight: Double,
        val normal: Double,
        val overweight: Double,
        val obeseClassI: Double,
        val dialLow: Double,
        val dialHigh: Double
    )

    // 女童表（年龄2-20）
    private val femaleTable = mapOf(
        2 to ChildRow(14.4, 17.9, 19.0, 19.1, 13.0, 20.0),
        3 to ChildRow(14.0, 17.1, 18.2, 18.3, 13.0, 19.0),
        4 to ChildRow(13.7, 16.7, 17.9, 18.0, 13.0, 19.0),
        5 to ChildRow(13.5, 16.7, 18.2, 18.3, 13.0, 19.0),
        6 to ChildRow(13.4, 17.1, 18.7, 18.8, 13.0, 20.0),
        7 to ChildRow(13.5, 17.5, 19.5, 19.6, 13.0, 21.0),
        8 to ChildRow(13.6, 18.3, 20.5, 20.6, 13.0, 22.0),
        9 to ChildRow(13.8, 19.1, 21.7, 21.8, 13.0, 23.0),
        10 to ChildRow(14.0, 19.9, 22.9, 23.0, 13.0, 24.0),
        11 to ChildRow(14.8, 21.6, 25.1, 25.2, 14.0, 26.0),
        12 to ChildRow(14.8, 21.6, 25.1, 25.2, 14.0, 26.0),
        13 to ChildRow(15.4, 22.5, 26.3, 26.4, 15.0, 27.0),
        14 to ChildRow(15.8, 23.3, 27.1, 27.2, 15.0, 28.0),
        15 to ChildRow(16.4, 24.0, 28.0, 28.1, 16.0, 29.0),
        16 to ChildRow(16.8, 24.5, 28.8, 28.9, 16.0, 30.0),
        17 to ChildRow(17.2, 25.1, 29.5, 29.6, 16.0, 31.0),
        18 to ChildRow(17.6, 25.5, 30.3, 30.4, 17.0, 31.0),
        19 to ChildRow(17.8, 26.1, 30.9, 31.0, 17.0, 32.0),
        20 to ChildRow(17.9, 26.4, 31.6, 31.7, 17.0, 33.0)
    )

    // 男童表
    private val maleTable = mapOf(
        2 to ChildRow(14.8, 18.1, 19.2, 19.3, 14.0, 20.0),
        3 to ChildRow(14.4, 17.3, 18.2, 18.3, 13.0, 19.0),
        4 to ChildRow(14.0, 16.8, 17.9, 18.0, 13.0, 19.0),
        5 to ChildRow(13.8, 16.7, 18.0, 18.1, 13.0, 19.0),
        6 to ChildRow(13.7, 16.9, 18.5, 18.6, 13.0, 20.0),
        7 to ChildRow(13.6, 17.3, 19.1, 19.2, 13.0, 20.0),
        8 to ChildRow(13.7, 17.7, 19.9, 20.0, 13.0, 21.0),
        9 to ChildRow(14.0, 18.5, 21.0, 21.1, 13.0, 22.0),
        10 to ChildRow(14.2, 19.3, 22.1, 22.2, 13.0, 23.0),
        11 to ChildRow(14.5, 19.9, 23.1, 23.2, 13.0, 24.0),
        12 to ChildRow(15.0, 20.9, 24.1, 24.2, 14.0, 25.0),
        13 to ChildRow(15.5, 21.7, 25.3, 25.4, 14.0, 26.0),
        14 to ChildRow(16.0, 22.5, 25.9, 26.0, 15.0, 27.0),
        15 to ChildRow(16.5, 23.4, 26.7, 26.8, 15.0, 28.0),
        16 to ChildRow(17.1, 24.1, 27.6, 27.7, 16.0, 29.0),
        17 to ChildRow(17.6, 24.7, 28.2, 28.3, 17.0, 29.0),
        18 to ChildRow(18.3, 25.5, 28.9, 29.0, 17.0, 30.0),
        19 to ChildRow(18.5, 26.3, 29.7, 29.8, 17.0, 31.0),
        20 to ChildRow(18.5, 27.1, 30.6, 30.7, 17.0, 32.0)
    )
}