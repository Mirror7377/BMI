package com.example.bmi.ui.profile.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.bmi.R

@Composable
fun LoginDialog(
    onDismiss: () -> Unit,
    onLogin: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            // 弹窗容器：灰色背景
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 0.dp),
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5F5)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 19.dp,
                            bottom = 10.dp
                        )
                ) {
                    // 用户信息行（头像 + 姓名 + 箭头 + 邮箱）
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.utx),
                            contentDescription = null,
                            modifier = Modifier
                                .width(65.dp)
                                .height(84.dp)
                        )

                        Spacer(modifier = Modifier.width(15.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.cassie),
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
                                    color = Color.Black
                                )

                                Spacer(modifier = Modifier.width(5.dp))

                                Icon(
                                    painter = painterResource(id = R.drawable.group),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = Color.Unspecified
                                )
                            }

                            Spacer(modifier = Modifier.height(5.dp))

                            Text(
                                text = stringResource(R.string.cassiexiao_gmail_com),
                                fontSize = 16.sp,
                                fontFamily = FontFamily(Font(R.font.montserrat_regular)),
                                color = Color(0xFF888888)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Login 按钮：纯白背景
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                            .height(55.dp)
                            .clickable(
                                onClick = onLogin,
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ),
                        shape = RoundedCornerShape(27.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White  // 👈 纯白背景
                        ),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.log_in),
                                fontSize = 18.sp,
                                fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
                                color = Color.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(15.dp))

                    // Cancel 按钮：纯白背景
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                            .height(55.dp)
                            .clickable(
                                onClick = onDismiss,
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ),
                        shape = RoundedCornerShape(27.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.cancell),
                                fontSize = 18.sp,
                                fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
                                color = Color.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}