package com.example.bmi.data.database

/**
 * 推荐应用的数据模型
 *
 * 用于在 BMI 结果页展示与用户健康状态相关的推荐 App 信息，
 * 包括应用名称、分类、评分及跳转所需的包名等。
 *
 * @property id 应用的唯一标识符（本地数据库主键）
 * @property name 应用显示名称（如 "MyFitnessPal"）
 * @property category 应用所属分类（如 "Health & Fitness"）
 * @property rating 应用在应用商店的评分（0.0 ~ 5.0）
 * @property iconResId 应用图标的本地资源 ID（指向 drawable 资源）
 * @property packageName 应用的 Android 包名，用于通过 Intent 跳转到应用详情页
 */
data class RecommendApp(
    val id: Int,
    val name: String,
    val category: String,
    val rating: Double,
    val iconResId: Int,
    val packageName: String
)