package com.example.bmi.utils

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
}