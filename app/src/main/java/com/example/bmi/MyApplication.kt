package com.example.bmi

import android.app.Application
import com.example.bmi.utils.DensityUtil
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application(){
    override fun onCreate() {
        super.onCreate()
        DensityUtil.setDensity(this)
    }
}
