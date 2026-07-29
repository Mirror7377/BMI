package com.example.bmi.utils

import com.example.bmi.R
import com.example.bmi.data.database.RecommendApp
import com.example.bmi.data.enums.Gender
import com.example.bmi.ui.bmigauge.BmiLevel

/**
 * 推荐 App 工具类，封装推荐算法和 App 数据源
 */
object RecommendationUtils {

    private val allApps = listOf(
        RecommendApp(
            id = 1,
            name = "Lose Weight App for Women - Workout at Home",
            category = "Home Workout for Men & Women",
            rating = 4.9,
            iconResId = R.drawable.ic_app_1,
            packageName = "loseweightapp.loseweightappforwomen.womenworkoutathome"
        ),
        RecommendApp(
            id = 2,
            name = "Lose Weight App for Men - Weight Loss in 30 Days",
            category = "Bodyweight Fitness ＆ Training",
            rating = 5.0,
            iconResId = R.drawable.ic_app_2,
            packageName = "menloseweight.loseweightappformen.weightlossformen"
        ),
        RecommendApp(
            id = 3,
            name = "Lose Weight at Home - Home Workout in 30 Days",
            category = "Home Workout for Weight Loss",
            rating = 4.9,
            iconResId = R.drawable.ic_app_3,
            packageName = "loseweight.weightloss.workout.fitness"
        ),
        RecommendApp(
            id = 4,
            name = "Fasting App - Fasting Tracker & Intermittent Fast",
            category = "Home Workout for Women, Fit",
            rating = 4.9,
            iconResId = R.drawable.ic_app_4,
            packageName = "bodyfast.zero.fastingtracker.weightloss"
        ),
        RecommendApp(
            id = 5,
            name = "Walking App - Walking for Weight Loss",
            category = "Weight Loss, Lose Belly Fat",
            rating = 4.6,
            iconResId = R.drawable.ic_app_5,
            packageName = "walking.weightloss.walk.tracker"
        ),
        RecommendApp(
            id = 6,
            name = "Home Workout - No Equipment",
            category = "Simple Fast Lose Weight Diet",
            rating = 4.9,
            iconResId = R.drawable.ic_app_6,
            packageName = "homeworkout.homeworkouts.noequipment"
        ),
        RecommendApp(
            id = 7,
            name = "30 Day Fitness Challenge - Workout at Home",
            category = "Walking Tracker & Pedometer",
            rating = 4.7,
            iconResId = R.drawable.ic_app_7,
            packageName = "com.popularapp.thirtydayfitnesschallenge"
        ),
        RecommendApp(
            id = 8,
            name = "Six Pack in 30 Days - Abs Workout",
            category = "6 Pack, Core, Abs Exercise",
            rating = 5.0,
            iconResId = R.drawable.ic_app_8,
            packageName = "sixpack.sixpackabs.absworkout"
        ),
        RecommendApp(
            id = 9,
            name = "Step Tracker - Pedometer Free & Calorie Tracker",
            category = "Step Counter, Weight Loss",
            rating = 4.9,
            iconResId = R.drawable.ic_app_9,
            packageName = "steptracker.healthandfitness.walkingtracker.pedometer"
        ),
        RecommendApp(
            id = 10,
            name = "Blood Pressure Monitor - Blood Pressure App",
            category = "Bodyweight Fitness ＆ Training",
            rating = 4.7,
            iconResId = R.drawable.ic_app_10,
            packageName = "bloodpressuremonitor.bloodpressureapp.bpmonitor"
        )
    )

    /**
     * 根据 BMI 等级和性别，随机推荐 3 个 App
     * @param bmiLevel 当前用户的 BMI 等级
     * @param gender 用户性别（"Male" 或 "Female"）
     * @return 包含 3 个 RecommendApp 的列表
     */
    fun getRecommendedApps(bmiLevel: BmiLevel, gender: String): List<RecommendApp> {
        // 判断是否为 Normal 以下（体重过轻）
        val isUnderNormal = bmiLevel.ordinal < BmiLevel.NORMAL.ordinal

        val ids = if (isUnderNormal) {
            // 体重过轻：推荐增重、力量训练类 App
            val pool12 = listOf(6, 7, 8).shuffled().take(2)
            val pool3 = listOf(5, 9, 10).shuffled().first()
            pool12 + listOf(pool3)
        } else {
            // 正常及以上：根据性别推荐
            val pool12 = if (gender == Gender.MALE.name) {
                listOf(2, 3, 6, 7, 8).shuffled().take(2)
            } else {
                listOf(1, 3, 6, 7, 8).shuffled().take(2)
            }
            val pool3 = listOf(4, 5, 9, 10).shuffled().first()
            pool12 + listOf(pool3)
        }

        // 根据 id 查找对应的 RecommendApp 对象，过滤掉 null
        return ids.mapNotNull { id -> allApps.find { it.id == id } }
    }
}