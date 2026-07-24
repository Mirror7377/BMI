package com.example.bmi.utils

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import com.example.bmi.databinding.CommonBannerBinding

object CommonBanner {

    fun show(
        activity: Activity,//当前的 Activity 实例
        @DrawableRes iconRes: Int,
        message: String
    ) {

        val root = activity.findViewById<ViewGroup>(android.R.id.content)

        val binding = CommonBannerBinding.inflate(
            LayoutInflater.from(activity),
            root,
            false
        )

        binding.ivIcon.setImageResource(iconRes)
        binding.tvMessage.text = message

        //将 Banner 视图添加到 Activity 的根布局中，使其显示在屏幕最上层。
        root.addView(binding.root)

        binding.layoutSuccess.post {

            binding.layoutSuccess.postDelayed({
                // 到时间直接移除
                root.removeView(binding.root)
            }, 2000)
        }
    }
}