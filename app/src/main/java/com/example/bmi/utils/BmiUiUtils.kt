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
import com.example.bmi.ui.bmigauge.BmiClassifier
import com.example.bmi.ui.bmigauge.BmiLevel

object BmiUiUtils {

    /**
     * 绑定推荐 App 卡片
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

}