package com.example.bmi.data.database

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bmi_records")
@Parcelize
data class BmiRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    // ---------- 用户输入的原始数据（用于 UI 精确显示） ----------
    val weightInput: Double,          // 用户输入的体重值（如 70.0 或 154.32）
    val weightUnit: String,           // "KG" 或 "LB"
    val heightUnit: String,           // "CM" 或 "FT_IN"
    val feetInput: Int?,              // ft-in 模式下的英尺（仅当 heightUnit="FT_IN" 时有效）
    val inchesInput: Int?,            // ft-in 模式下的英寸（仅当 heightUnit="FT_IN" 时有效）


    val weightKg: Double,
    val heightCm: Double,

    val timestamp: Long,  //测量时间 todo 格式化
    val timeOfDay: String, //测量时间段 todo 格式化
    val age: Int,
    val gender: String,
    val bmi: Double,
    val category: String,   //BMI 对应的等级
    val createTime: Long = 0 //todo 格式化
) : Parcelable