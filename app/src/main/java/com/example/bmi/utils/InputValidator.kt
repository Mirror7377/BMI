package com.example.bmi.utils

import com.example.bmi.ui.home.enums.WeightUnit
import kotlin.math.pow
import kotlin.math.roundToInt

object InputValidator {
    // 范围常量（针对不同单位）
    private val KG_RANGE = 1.0..250.0
    private val LB_RANGE = 2.0..551.0
    private val CM_RANGE = 1.0..250.0
    private val FEET_RANGE = 1..8
    private val INCHES_RANGE = 0..11

    // 默认值（均为用户单位下的值）
    const val DEFAULT_KG = 65.0
    const val DEFAULT_LB = 140.0
    const val DEFAULT_CM = 170.0
    const val DEFAULT_FEET = 5
    const val DEFAULT_INCHES = 7

    /**
     * 验证体重（返回修正后的体重值，单位与输入一致）
     */
    fun validateWeight(input: String, unit: WeightUnit): Triple<Double, Boolean, String?> {
        val (value, needToast, msg) = when {
            input.isBlank() -> {
                val default = if (unit == WeightUnit.KG) DEFAULT_KG else DEFAULT_LB
                Triple(default, true, getWeightRangeMessage(unit))
            }
            else -> {
                val parsed = input.toDoubleOrNull()
                if (parsed == null) {
                    val default = if (unit == WeightUnit.KG) DEFAULT_KG else DEFAULT_LB
                    Triple(default, true, getWeightRangeMessage(unit))
                } else {
                    val range = if (unit == WeightUnit.KG) KG_RANGE else LB_RANGE
                    val clamped = when {
                        parsed < range.start -> range.start
                        parsed > range.endInclusive -> range.endInclusive
                        else -> parsed
                    }
                    val needToast = clamped != parsed
                    val msg = if (needToast) getWeightRangeMessage(unit) else null
                    Triple(clamped, needToast, msg)
                }
            }
        }
        return Triple(value, needToast, msg)
    }

    /**
     * 验证身高（cm模式，返回修正后的cm值）
     */
    fun validateHeightCm(input: String): Triple<Double, Boolean, String?> {
        val (value, needToast, msg) = when {
            input.isBlank() -> Triple(DEFAULT_CM, true, "Please input a valid height (1 - 250 cm)")
            else -> {
                val parsed = input.toDoubleOrNull()
                if (parsed == null) Triple(DEFAULT_CM, true, "Please input a valid height (1 - 250 cm)")
                else {
                    val clamped = when {
                        parsed < CM_RANGE.start -> CM_RANGE.start
                        parsed > CM_RANGE.endInclusive -> CM_RANGE.endInclusive
                        else -> parsed
                    }
                    val needToast = clamped != parsed
                    val msg = if (needToast) "Please input a valid height (1 - 250 cm)" else null
                    Triple(clamped, needToast, msg)
                }
            }
        }
        return Triple(value, needToast, msg)
    }

    fun validateFeet(input: String): Pair<Int, Boolean> {
        if (input.isBlank()) return Pair(DEFAULT_FEET, true)
        val parsed = input.toIntOrNull()
        if (parsed == null) return Pair(DEFAULT_FEET, true)
        val clamped = when {
            parsed < FEET_RANGE.start -> FEET_RANGE.start
            parsed > FEET_RANGE.endInclusive -> FEET_RANGE.endInclusive
            else -> parsed
        }
        return Pair(clamped, clamped != parsed)
    }

    fun validateInches(input: String): Pair<Int, Boolean> {
        if (input.isBlank()) return Pair(DEFAULT_INCHES, true)
        val parsed = input.toIntOrNull()
        if (parsed == null) return Pair(DEFAULT_INCHES, true)
        val clamped = when {
            parsed < INCHES_RANGE.start -> INCHES_RANGE.start
            parsed > INCHES_RANGE.endInclusive -> INCHES_RANGE.endInclusive
            else -> parsed
        }
        return Pair(clamped, clamped != parsed)
    }

    private fun getWeightRangeMessage(unit: WeightUnit): String {
        val range = if (unit == WeightUnit.KG) KG_RANGE else LB_RANGE
        return "Please input a valid weight (${range.start} - ${range.endInclusive} ${unit.name}) to calculate your BMI accurately"
    }

    // 格式化显示（保留两位/一位小数）
    fun formatWeight(value: Double, unit: WeightUnit): String {
        return String.format("%.2f", value)
    }
    fun formatHeightCm(value: Double): String = String.format("%.1f", value)
}