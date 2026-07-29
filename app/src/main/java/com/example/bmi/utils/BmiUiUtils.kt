package com.example.bmi.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.core.content.edit
import com.example.bmi.R
import com.example.bmi.data.database.RecommendApp
import com.example.bmi.data.enums.Gender
import com.example.bmi.databinding.DialogDiscardConfirmBinding
import com.example.bmi.ui.bmigauge.BmiClassifier
import com.example.bmi.ui.bmigauge.BmiLevel

object BmiUiUtils {

    /**
     * 绑定推荐 App 卡片（完全一致）
     */
    fun bindAppToCard(
        cardView: View,
        iconView: ImageView,
        nameView: TextView,
        categoryView: TextView,
        ratingBar: RatingBar,
        ratingTextView: TextView,
        app: RecommendApp?
    ) {
        if (app == null) {
            cardView.visibility = View.GONE
            return
        }
        cardView.visibility = View.VISIBLE

        iconView.setImageResource(app.iconResId)
        nameView.text = app.name
        categoryView.text = app.category
        ratingBar.rating = app.rating.toFloat()
        ratingTextView.text = String.format("%.1f", app.rating)

        // 点击跳转 Google Play
        cardView.setOnClickListener {
            val url = "https://play.google.com/store/apps/details?id=${app.packageName}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            cardView.context.startActivity(intent)
        }
    }

    /**
     * 显示统一的“确认丢弃/删除”对话框
     */
    fun showConfirmDialog(
        activity: Activity,
        onConfirm: () -> Unit
    ) {
        val dialogBinding = DialogDiscardConfirmBinding.inflate(
            activity.layoutInflater
        )
        val dialog = Dialog(activity).apply {
            setContentView(dialogBinding.root)
            window?.apply {
                setBackgroundDrawableResource(android.R.color.transparent)
                setGravity(Gravity.CENTER)
                setLayout(dpToPx(activity, 301f), dpToPx(activity, 154f))
            }
        }
        dialogBinding.tvCancel.setOnClickListener { dialog.dismiss() }
        dialogBinding.tvDelete.setOnClickListener {
            dialog.dismiss()
            onConfirm.invoke()
        }
        dialog.show()
    }

    /**
    * 获取该身高对应的正常体重范围（公斤）
    */
    fun getStandardWeightRangeCm(
        heightCm: Double,
        age: Int,
        gender: String
    ): Pair<Double, Double> {
        val h = heightCm / 100.0
        return if (age in 2..20) {
            val genderEnum = if (gender == Gender.MALE.name) Gender.MALE else Gender.FEMALE
            val (bmiLow, bmiHigh) = BmiClassifier.getNormalBmiRange(age, genderEnum)
            Pair(bmiLow * h * h, bmiHigh * h * h)
        } else {
            Pair(18.5 * h * h, 24.9 * h * h)
        }
    }

    /**
     * dp → px 转换
     */
    fun dpToPx(context: Context, dp: Float): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}