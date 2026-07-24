package com.example.bmi.utils

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import com.example.bmi.databinding.CommonBannerBinding

object CommonBanner {

    fun show(
        activity: Activity,
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

        root.addView(binding.root)

        binding.layoutSuccess.post {

            // 直接显示
            binding.layoutSuccess.translationY = 0f
            binding.layoutSuccess.alpha = 1f

            binding.layoutSuccess.postDelayed({

                // 到时间直接移除（没有退出动画）
                root.removeView(binding.root)

            }, 2000)
        }
    }
}