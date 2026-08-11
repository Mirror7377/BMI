package com.example.bmi.ui.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bmi.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    viewModel: FeedbackViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()


    // 监听 Effect
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is FeedbackEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    val bgGray = Color(0xFFF5F5F5)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGray)
    ) {
        // ---- 顶部导航栏 ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 15.dp)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回箭头
            Icon(
                painter = painterResource(id = R.drawable.arrow_left),
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        onClick = { viewModel.handleIntent(FeedbackIntent.NavigateBack) },
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ),
                tint = Color(0xFF222222)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = stringResource(R.string.feedback),
                fontSize = 20.sp,
                fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
                color = Color(0xFF222222)
            )
        }

        // ---- 白色卡片容器（带键盘避让） ----
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp)
                .padding(top = 10.dp, bottom = 20.dp)
                .imePadding(),  // 键盘弹出时自动上移

            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // 输入框（占据剩余空间）
                TextField(
                    value = state.feedbackText,
                    onValueChange = {
                        viewModel.handleIntent(FeedbackIntent.UpdateFeedbackText(it))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.feedback_or_suggestion),
                            color = Color(0xFF888888),
                            fontSize = 16.sp,
                            fontFamily = FontFamily(Font(R.font.montserrat_regular))
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 16.sp,
                        fontFamily = FontFamily(Font(R.font.montserrat_regular)),
                        color = Color(0xFF222222)
                    ),
                    maxLines = 10,
                    minLines = 8
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ---- 提交按钮 ----
                Button(
                    onClick = {
                        viewModel.handleIntent(FeedbackIntent.SubmitFeedback)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(75.dp)
                        .padding(10.dp),
                    enabled = state.isSubmitEnabled && !state.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isSubmitEnabled) {
                            Color(0xFF3659CF)  // splash_blue
                        } else {
                            Color(0xFFE0E0E0)   // bg_gray
                        },
                        disabledContainerColor = Color(0xFFE0E0E0)
                    ),
                    shape = RoundedCornerShape(27.5.dp)
                ) {
                    Text(
                        text = stringResource(R.string.save),
                        fontSize = 20.sp,
                        fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
                        color = Color.White,
                        letterSpacing = (-0.00935).sp
                    )
                }

                // 底部间距
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}