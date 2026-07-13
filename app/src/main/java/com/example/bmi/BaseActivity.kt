package com.example.bmi

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.bmi.utils.DensityUtil

//创建自定义 Application 并注册生命周期观察者（确保每个 Activity 的 Density 都正确）。
abstract class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 在 super.onCreate() 之前调用可确保布局加载时 density 已修改
        DensityUtil.setDensity(this)
        super.onCreate(savedInstanceState)
    }
}