package com.example.bmi.utils

import com.example.bmi.ui.home.enums.HeightUnit
import com.example.bmi.ui.home.enums.WeightUnit

object UnitConverter {

    fun lbToKg(lb: Double): Double = lb * 0.45359237

    fun kgToLb(kg: Double): Double = kg / 0.45359237

    // 身高
    fun cmToFeet(cm: Double): Int = (cm / 30.48).toInt()
    fun cmToInches(cm: Double): Int = ((cm % 30.48) / 2.54).toInt()
    fun feetInchToCm(feet: Int, inches: Int): Double = (feet * 12 + inches) * 2.54

    // 格式化显示
    fun formatWeight(kg: Double, unit: WeightUnit): String {
        val value = if (unit == WeightUnit.KG) kg else kgToLb(kg)
        return String.format("%.2f", value)   // 保留两位小数
    }
    fun formatHeightCm(cm: Double): String = String.format("%.1f", cm)
    fun formatHeightFtIn(cm: Double): String {
        val feet = cmToFeet(cm)
        val inches = cmToInches(cm)
        return "$feet' $inches\""
    }

    /**
     * 统一多单位组合BMI计算，完全复刻HomeViewModel四段公式逻辑
     */
    fun calculateBmi(
        heightUnit: HeightUnit,
        feetInput: Int,
        inchesInput: Int,
        heightCm: Double,
        weightUnit: WeightUnit,
        weightInput: Double,
        weightKg: Double
    ): Double {
        return when {
            // ① cm + kg
            heightUnit == HeightUnit.CM && weightUnit == WeightUnit.KG -> {
                val heightM = heightCm / 100.0
                weightKg / (heightM * heightM)
            }
            // ② ft-in + lb
            heightUnit == HeightUnit.FT_IN && weightUnit == WeightUnit.LB -> {
                val totalInches = feetInput * 12.0 + inchesInput
                weightInput / (totalInches * totalInches) * 703.0
            }
            // ③ ft-in + kg
            heightUnit == HeightUnit.FT_IN && weightUnit == WeightUnit.KG -> {
                val heightM = feetInput * 0.3048 + inchesInput * 0.0254
                weightKg / (heightM * heightM)
            }
            // ④ cm + lb
            heightUnit == HeightUnit.CM && weightUnit == WeightUnit.LB -> {
                val wKg = weightInput * 0.45359237
                val hM = heightCm / 100.0
                wKg / (hM * hM)
            }
            // 兜底
            else -> {
                val hM = heightCm / 100.0
                weightKg / (hM * hM)
            }
        }
    }
}