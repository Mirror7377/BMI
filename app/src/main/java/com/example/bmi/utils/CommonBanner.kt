package com.example.bmi.utils

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bmi.R
import com.example.bmi.databinding.CommonBannerBinding
import kotlinx.coroutines.delay

object CommonBanner {

    // ==================== 传统 View 模式 ====================
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
            binding.layoutSuccess.postDelayed({
                root.removeView(binding.root)
            }, 2000)
        }
    }

    // ==================== Compose 原生版本 ====================
    data class BannerData(
        val iconRes: Int,
        val message: String,
        val durationMillis: Long = 2000
    )

    data class BannerState(
        val visible: Boolean = false,
        val data: BannerData? = null
    )

    fun initialState(): BannerState = BannerState()

    fun showBanner(
        currentState: BannerState,
        iconRes: Int,
        message: String,
        durationMillis: Long = 2000
    ): BannerState {
        return currentState.copy(
            visible = true,
            data = BannerData(iconRes, message, durationMillis)
        )
    }

    fun hideBanner(currentState: BannerState): BannerState {
        return currentState.copy(visible = false)
    }

    @Composable
    fun BannerHost(
        state: BannerState,
        onDismiss: () -> Unit
    ) {
        if (!state.visible || state.data == null) return

        val data = state.data!!

        LaunchedEffect(Unit) {
            delay(data.durationMillis)
            onDismiss()
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                    initialScale = 0.9f,
                    animationSpec = tween(300)
                ),
                exit = fadeOut(animationSpec = tween(300)) + scaleOut(
                    targetScale = 0.9f,
                    animationSpec = tween(300)
                ),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(top = 60.dp)
                        .wrapContentHeight()
                        .align(Alignment.TopCenter),
                    shape = RoundedCornerShape(15.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = data.iconRes),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = data.message,
                            color = Color.Black,
                            fontSize = 14.sp,
                            fontFamily = FontFamily(Font(R.font.montserrat_regular)),
                            maxLines = 2,
                            softWrap = true
                        )
                    }
                }
            }
        }
    }
}