package com.example.bmi.utils

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks
import android.content.res.Configuration
import android.content.res.Resources
import android.util.DisplayMetrics

/**
 * 今日头条屏幕适配方案 —— 修改 Density
 * 设计稿基准宽度：375dp
 */
object DensityUtil {

    private const val DESIGN_WIDTH_DP = 375f   // 设计稿宽度（dp）

    /**
     * 在 Application 中全局修改 Density
     * 需要在 Application.onCreate() 中调用
     */
    fun setDensity(application: Application) {
        val appDisplayMetrics = application.resources.displayMetrics
        val targetDensity = appDisplayMetrics.widthPixels / DESIGN_WIDTH_DP
        applyDensity(appDisplayMetrics, targetDensity)

        // 监听系统字体缩放变化，重新设置 scaledDensity
        application.registerComponentCallbacks(object : ComponentCallbacks {
            override fun onConfigurationChanged(newConfig: Configuration) {
                if (newConfig.fontScale > 0) {
                    val fontScale = newConfig.fontScale
                    val newScaledDensity = targetDensity * fontScale
                    applyScaledDensity(appDisplayMetrics, newScaledDensity)
                }
            }
            override fun onLowMemory() {}
        })
    }

    /**
     * 在 Activity 中单独修改 Density
     * 可在 Activity.onCreate() 中调用，通常配合 Lifecycle 在 onResume 中调用以应对某些机型
     */
    fun setDensity(activity: Activity) {
        val activityDisplayMetrics = activity.resources.displayMetrics
        val targetDensity = activityDisplayMetrics.widthPixels / DESIGN_WIDTH_DP
        applyDensity(activityDisplayMetrics, targetDensity)
    }

    private fun applyDensity(metrics: DisplayMetrics, density: Float) {
        metrics.density = density
        metrics.densityDpi = (density * 160).toInt()

        // scaledDensity 保留系统字体缩放
        val fontScale = Resources.getSystem().configuration.fontScale
        metrics.scaledDensity = density * fontScale
    }

    private fun applyScaledDensity(metrics: DisplayMetrics, scaledDensity: Float) {
        metrics.scaledDensity = scaledDensity
    }
}