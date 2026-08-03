package com.example.bmi.utils

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks
import android.content.res.Configuration
import android.content.res.Resources
import android.util.DisplayMetrics

@Suppress("DEPRECATION")
object DensityUtil {

    private const val DESIGN_WIDTH_DP = 375f   // 设计稿宽度（dp）

    fun setDensity(application: Application) {
        //获取当前屏幕数据
        val appDisplayMetrics = application.resources.displayMetrics
        //获取缩放比例
        val targetDensity = appDisplayMetrics.widthPixels / DESIGN_WIDTH_DP
        //调用函数
        applyDensity(appDisplayMetrics, targetDensity)

        application.registerComponentCallbacks(object : ComponentCallbacks {
            //系统配置改变时触发：当改变屏幕方向、字体大小等此方法会被调用。
            override fun onConfigurationChanged(newConfig: Configuration) {
                //读取系统最新的字体缩放系数。
                val fontScale = newConfig.fontScale
                //更新全局字体配置
                appDisplayMetrics.scaledDensity = targetDensity * fontScale

            }
            override fun onLowMemory() {}
        })
    }

    //activity重建时
    fun setDensity(activity: Activity) {
        //获取当前 Activity 的屏幕数据
        val activityDisplayMetrics = activity.resources.displayMetrics
        //计算目标密度
        val targetDensity = activityDisplayMetrics.widthPixels / DESIGN_WIDTH_DP
        applyDensity(activityDisplayMetrics, targetDensity)
    }

    private fun applyDensity(metrics: DisplayMetrics, density: Float) {
        //设置逻辑密度 决定了 1dp 等于多少像素（px）
        metrics.density = density
        //屏幕密度 DPI
        //density（逻辑密度） = densityDpi（物理 DPI） / 160
        metrics.densityDpi = (density * 160).toInt()

        // 获取当前系统的字体缩放比例
        val fontScale = Resources.getSystem().configuration.fontScale
        //并更新全局文字配置
        metrics.scaledDensity = density * fontScale
    }
}