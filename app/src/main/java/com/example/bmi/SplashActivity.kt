package com.example.bmi

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.bmi.databinding.ActivitySplashBinding
import com.example.bmi.data.repository.BmiRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : BaseActivity() {

    @Inject
    lateinit var repository: BmiRepository

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

        val window = this.window

        window.statusBarColor =
            ContextCompat.getColor(this, R.color.splash_blue)

        WindowInsetsControllerCompat(
            window,
            window.decorView
        ).isAppearanceLightStatusBars = false

        layoutViews()
        initState()
        binding.root.post {
            startAnimation()
        }
    }

    private fun initState() {
        //设为完全透明，会有淡入效果
        binding.logoContainer.alpha = 0f
        //位移距离
        binding.logoContainer.translationY = 100f * scale
    }

    //根据实际屏幕尺寸与设计尺寸的比例
    private fun layoutViews() {
        val dm = resources.displayMetrics//获取当前设备信息
        val sw = dm.widthPixels.toFloat()
        val sh = dm.heightPixels.toFloat()

        //取两者中的较小值作为最终缩放比例。
        scale = minOf(sw / DESIGN_WIDTH, sh / DESIGN_HEIGHT)
        //计算偏移量
        offsetX = (sw - DESIGN_WIDTH * scale) / 2f
        offsetY = (sh - DESIGN_HEIGHT * scale) / 2f

        layout(binding.logoContainer, 30f, 293.5f, 170f, 129f)
        layout(binding.imgLeap, 112.5f, 680f, 150f, 40f)
    }

    private fun startAnimation() {
        // 设置指针旋转轴心（在视图测量完成后调用，确保宽高已确定）
        binding.imgNeedle.pivotX = binding.imgNeedle.width / 2f
        binding.imgNeedle.pivotY = binding.imgNeedle.height.toFloat()

        //Logo 容器动画：同时执行上移和淡入
        val logoMove = AnimatorSet().apply {
            //并行播放括号内的所有动画
            playTogether(
                ObjectAnimator.ofFloat(binding.logoContainer, View.TRANSLATION_Y, 100f * scale, 0f),
                ObjectAnimator.ofFloat(binding.logoContainer, View.ALPHA, 0f, 1f)
            )
            duration = 800
        }

        val firstNeedle = ObjectAnimator.ofFloat(binding.imgNeedle, View.ROTATION, -30f, 45f).apply {
            duration = 800
        }

        val secondNeedle = ObjectAnimator.ofFloat(binding.imgNeedle, View.ROTATION, 45f, -45f).apply {
            duration = 800
            interpolator = PathInterpolator(0.1f, 0f, 0.25f, 0.1f)
        }

        // 第一阶段：Logo 动画和第一次摆动同时进行
        val firstStage = AnimatorSet().apply {
            playTogether(logoMove, firstNeedle)
        }


        // 依次执行
        val all = AnimatorSet().apply {
            playSequentially(firstStage, secondNeedle)
        }

        // 动画结束监听：查询数据库，跳转主界面
        all.addListener(object : AnimatorListenerAdapter() {
            //当 all 动画序列播放完毕时自动回调
            override fun onAnimationEnd(animation: Animator) {
                lifecycleScope.launch {
                    val hasData = repository.getRecordCount() != 0
                    val intent = Intent(this@SplashActivity, MainActivity::class.java).apply {
                        putExtra("hasData", hasData)
                    }
                    startActivity(intent)
                    finish()
                }
            }
        })

        // 启动动画
        all.start()
    }

    private fun layout(view: View, left: Float, top: Float, width: Float, height: Float) {
        //换算后的布局
        val lp = FrameLayout.LayoutParams(
            (width * scale).toInt(),
            (height * scale).toInt()
        )
        //设置左右边距
        lp.leftMargin = (left * scale + offsetX).toInt()
        lp.topMargin = (top * scale + offsetY).toInt()
        view.layoutParams = lp
    }
}