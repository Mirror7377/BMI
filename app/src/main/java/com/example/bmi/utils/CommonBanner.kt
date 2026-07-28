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

        //这是 Android 系统中 Activity 的根视图容器（FrameLayout），是所有 UI 视图的最底层父容器。
        //获取它的目的：把 Banner 视图添加到这个根容器里，确保 Banner 能覆盖在当前 Activity 的所有内容之上
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

        //把内部逻辑放到主线程的消息队列末尾
        binding.layoutSuccess.post {

            binding.layoutSuccess.postDelayed({
                // 延迟 2000 毫秒 , 2 秒 后执行移除操作。
                // root.removeView(binding.root) 会把 Banner 从根布局中移除，彻底销毁这个视图。
                root.removeView(binding.root)
            }, 2000)
        }
    }
}