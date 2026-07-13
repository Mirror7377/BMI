package com.example.bmi

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.bmi.databinding.ActivitySplashBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    companion object {
        private const val DESIGN_WIDTH = 375f
        private const val DESIGN_HEIGHT = 750f
    }

    private lateinit var binding: ActivitySplashBinding
    private var scale = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.post {
            layoutViews()
            initState()
            startAnimation()
        }

    }

    private fun initState() {
        binding.logoContainer.alpha = 0f
        binding.logoContainer.translationY = 100f * scale
    }

    private fun layoutViews() {
        val dm = resources.displayMetrics
        val sw = dm.widthPixels.toFloat()
        val sh = dm.heightPixels.toFloat()

        scale = minOf(sw / DESIGN_WIDTH, sh / DESIGN_HEIGHT)
        offsetX = (sw - DESIGN_WIDTH * scale) / 2f
        offsetY = (sh - DESIGN_HEIGHT * scale) / 2f

        layout(binding.logoContainer, 30f, 293.5f, 170f, 129f)
        layout(binding.imgLeap, 112.5f, 680f, 150f, 40f)
    }

    private fun startAnimation() {
        binding.imgNeedle.post {
            binding.imgNeedle.pivotX = binding.imgNeedle.width / 2f
            binding.imgNeedle.pivotY = binding.imgNeedle.height.toFloat()

            val logoMove = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(binding.logoContainer, View.TRANSLATION_Y, 100f * scale, 0f),
                    ObjectAnimator.ofFloat(binding.logoContainer, View.ALPHA, 0f, 1f)
                )
                duration = 1000
            }

            val firstNeedle = ObjectAnimator.ofFloat(binding.imgNeedle, View.ROTATION, -30f, 45f).apply {
                duration = 1000
            }

            val secondNeedle = ObjectAnimator.ofFloat(binding.imgNeedle, View.ROTATION, 45f, -45f).apply {
                duration = 1000
                interpolator = PathInterpolator(0.25f, 0f, 0.1f, 0.1f)
            }

            val firstStage = AnimatorSet().apply {
                playTogether(logoMove, firstNeedle)
            }

            val hold = ValueAnimator.ofFloat(0f, 1f).apply { duration = 1000 }

            val all = AnimatorSet().apply {
                playSequentially(firstStage, secondNeedle, hold)
            }

            all.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                }
            })
            all.start()
        }
    }

    private fun layout(view: View, left: Float, top: Float, width: Float, height: Float) {
        val lp = FrameLayout.LayoutParams(
            (width * scale).toInt(),
            (height * scale).toInt()
        )
        lp.leftMargin = (left * scale + offsetX).toInt()
        lp.topMargin = (top * scale + offsetY).toInt()
        view.layoutParams = lp
    }



}