package com.example.bmi.data.database


import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "bmi_records")
data class BmiRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    // ---------- 用户输入的原始数据（用于 UI 精确显示） ----------
    val weightInput: Double,          // 用户输入的体重值（如 70.0 或 154.32）
    val weightUnit: String,           // "KG" 或 "LB"
    val heightInput: Double,          // 用户输入的身高值（cm 或 总英寸数）
    val heightUnit: String,           // "CM" 或 "FT_IN"
    val feetInput: Int?,              // ft-in 模式下的英尺（仅当 heightUnit="FT_IN" 时有效）
    val inchesInput: Int?,            // ft-in 模式下的英寸（仅当 heightUnit="FT_IN" 时有效）

    // ---------- 标准换算值（用于 BMI 计算和统计） ----------
    val weightKg: Double,             // 统一为 kg（计算用）
    val heightCm: Double,             // 统一为 cm（计算用）

    // ---------- 其他字段 ----------
    val timestamp: Long,
    val timeOfDay: String,
    val age: Int,
    val gender: String,
    val bmi: Double,
    val category: String,
    val createTime: Long = 0
): Serializable