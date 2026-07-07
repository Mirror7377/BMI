package com.example.bmi

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    companion object {
        private const val DESIGN_WIDTH = 375f
        private const val DESIGN_HEIGHT = 750f
    }

    private lateinit var logoContainer: FrameLayout
    private lateinit var gaugeContainer: FrameLayout

    private lateinit var gauge: ImageView
    private lateinit var needle: ImageView
    private lateinit var title: ImageView
    private lateinit var leap: ImageView

    private var scale = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        initView()

        layoutViews()

        initState()

        startAnimation()
    }

    /**
     * 初始化View
     */
    private fun initView() {

        logoContainer = findViewById(R.id.logoContainer)
        gaugeContainer = findViewById(R.id.gaugeContainer)

        gauge = findViewById(R.id.imgGauge)
        needle = findViewById(R.id.imgNeedle)
        title = findViewById(R.id.imgTitle)
        leap = findViewById(R.id.imgLeap)

    }

    /**
     * 初始化动画开始状态
     */
    private fun initState() {

        logoContainer.alpha = 0f
        logoContainer.translationY = 25f * scale

    }

    /**
     * 按375×750设计稿布局
     */
    private fun layoutViews() {

        val dm = resources.displayMetrics

        val sw = dm.widthPixels.toFloat()
        val sh = dm.heightPixels.toFloat()

        scale = minOf(
            sw / DESIGN_WIDTH,
            sh / DESIGN_HEIGHT
        )

        // ===== 新增 =====
        offsetX = (sw - DESIGN_WIDTH * scale) / 2f
        offsetY = (sh - DESIGN_HEIGHT * scale) / 2f
        // ================

        layout(
            logoContainer,
            30f,
            293.5f,
            170f,
            129f
        )

        layout(
            leap,
            112.5f,
            680f,
            150f,
            40f
        )

    }

    /**
     * 开始动画
     */
    private fun startAnimation() {

        needle.post {

            // 指针底部作为旋转中心
            needle.pivotX = needle.width / 2f
            needle.pivotY = needle.height.toFloat()

            //-----------------------------
            // 第一阶段 Logo出现
            //-----------------------------

            val logoMove = AnimatorSet()

            logoMove.playTogether(

                ObjectAnimator.ofFloat(
                    logoContainer,
                    View.TRANSLATION_Y,
                    25f * scale,
                    0f
                ),

                ObjectAnimator.ofFloat(
                    logoContainer,
                    View.ALPHA,
                    0f,
                    1f
                )

            )

            logoMove.duration = 1000

            //-----------------------------
            // 第一阶段 指针
            //-----------------------------

            val firstNeedle = ObjectAnimator.ofFloat(
                needle,
                View.ROTATION,
                -35f,
                45f
            )

            firstNeedle.duration = 1000

            //-----------------------------
            // 第二阶段 指针回弹
            //-----------------------------

            val secondNeedle = ObjectAnimator.ofFloat(
                needle,
                View.ROTATION,
                45f,
                -18f
            )

            secondNeedle.duration = 1000
            secondNeedle.interpolator = OvershootInterpolator(0.5f)

            //-----------------------------
            // 停留
            //-----------------------------

            val hold = ValueAnimator.ofFloat(0f, 1f)
            hold.duration = 1000

            //-----------------------------
            // 第一秒
            //-----------------------------

            val firstStage = AnimatorSet()

            firstStage.playTogether(
                logoMove,
                firstNeedle
            )

            //-----------------------------
            // 全部动画
            //-----------------------------

            val all = AnimatorSet()

            all.playSequentially(

                firstStage,

                secondNeedle,

                hold

            )

            all.addListener(object : AnimatorListenerAdapter() {

                override fun onAnimationEnd(animation: Animator) {

                    startActivity(
                        Intent(
                            this@SplashActivity,
                            MainActivity::class.java
                        )
                    )

                    finish()

                }

            })

            all.start()

        }

    }

    /**
     * 根据设计稿布局
     */
    private fun layout(
        view: View,
        left: Float,
        top: Float,
        width: Float,
        height: Float
    ) {

        val lp = FrameLayout.LayoutParams(
            (width * scale).toInt(),
            (height * scale).toInt()
        )

        lp.leftMargin = (left * scale + offsetX).toInt()

        lp.topMargin = (top * scale + offsetY).toInt()

        view.layoutParams = lp

    }

}