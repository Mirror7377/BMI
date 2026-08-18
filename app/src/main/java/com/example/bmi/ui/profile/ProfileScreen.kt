package com.example.bmi.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bmi.R
import com.example.bmi.ui.profile.components.CustomSwitch
import com.example.bmi.utils.Banner
import com.example.bmi.utils.rememberBannerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateToLanguage: () -> Unit,
    onNavigateToFeedback: () -> Unit,
    onShowLoginDialog: () -> Unit,
    onShowUserInfoDialog: () -> Unit,
    onNavigateBack: () -> Unit,
    onRateUs: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Banner 状态（新版）
    val bannerState = rememberBannerState()

    // 监听 Effect
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ProfileEffect.ShowLoginDialog -> onShowLoginDialog()
                is ProfileEffect.ShowUserInfoDialog -> onShowUserInfoDialog()
                is ProfileEffect.ImportSuccess -> {
                    bannerState.show(
                        iconRes = R.drawable.check_circle,
                        message = "Import records successfully."
                    )
                }
                is ProfileEffect.ShowFeedbackBanner -> {
                    bannerState.show(
                        iconRes = R.drawable.check_circle,
                        message = effect.message
                    )
                }
                is ProfileEffect.ShowBanner -> {
                    bannerState.show(
                        iconRes = effect.iconRes,
                        message = effect.message
                    )
                }
            }
        }
    }

    val bgGray = Color(0xFFEAEAEE)

    Box(modifier = Modifier.fillMaxSize()) {
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
                Icon(
                    painter = painterResource(id = R.drawable.arrow_left),
                    contentDescription = "Back",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(
                            onClick = onNavigateBack,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = stringResource(R.string.me),
                    fontSize = 20.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_extrabold))
                )
            }

            // ---- 用户信息卡片 ----
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp)
                    .padding(top = 10.dp)
                    .clickable(
                        onClick = {
                            viewModel.handleIntent(ProfileIntent.AvatarClicked)
                        },
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.isLoggedIn) {
                        Image(
                            painter = painterResource(id = R.drawable.utx),
                            contentDescription = null,
                            modifier = Modifier.size(55.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = state.userName,
                                fontSize = 16.sp,
                                fontFamily = FontFamily(Font(R.font.montserrat_extrabold))
                            )
                            Text(
                                text = state.userEmail,
                                fontSize = 14.sp,
                                fontFamily = FontFamily(Font(R.font.montserrat_regular))
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Icon(
                            painter = painterResource(id = R.drawable.ic_autorenew_black),
                            contentDescription = "Import",
                            modifier = Modifier
                                .size(24.dp)
                                .clickable(
                                    onClick = {
                                        viewModel.handleIntent(ProfileIntent.ImportSampleData)
                                    },
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ),
                            tint = Color.Unspecified
                        )
                    } else {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = stringResource(R.string.backup_amp_restore),
                                        fontSize = 16.sp,
                                        fontFamily = FontFamily(Font(R.font.montserrat_extrabold))
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Icon(
                                        painter = painterResource(id = R.drawable.group),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = Color.Unspecified
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.synchronize_your_data),
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily(Font(R.font.montserrat_regular))
                                )
                            }

                            Icon(
                                painter = painterResource(id = R.drawable.ic_autorenew_black),
                                contentDescription = "Import",
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable(
                                        onClick = {
                                            viewModel.handleIntent(ProfileIntent.ImportSampleData)
                                        },
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ),
                                tint = Color.Unspecified
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- 语言 + 同步卡片 ----
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                onClick = onNavigateToLanguage,
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            )
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.thumbnail),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color.Unspecified
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = stringResource(R.string.language),
                            fontSize = 16.sp,
                            fontFamily = FontFamily(Font(R.font.montserrat_regular))
                        )
                    }

                    HorizontalDivider(
                        color = Color(0xFFEEEEEE),
                        thickness = 1.dp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.bitmap),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = stringResource(R.string.connect_to_google_fit),
                            fontSize = 16.sp,
                            fontFamily = FontFamily(Font(R.font.montserrat_regular)),
                            color = Color(0xFF222222)
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // 直接使用 ViewModel 状态，不再维护本地 isSyncEnabled
                        CustomSwitch(
                            checked = state.isSyncEnabled,
                            onCheckedChange = { isChecked ->
                                viewModel.handleIntent(ProfileIntent.ToggleSync(isChecked))
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- 更多选项卡片 ----
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF607D8B), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.icon),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.Unspecified
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = stringResource(R.string.remove_ads),
                            fontSize = 16.sp,
                            fontFamily = FontFamily(Font(R.font.montserrat_regular))
                        )
                    }

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = Color(0xFFEEEEEE)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                onClick = onRateUs,
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            )
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF607D8B), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.icon_2),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.Unspecified
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = stringResource(R.string.rate_us),
                            fontSize = 16.sp,
                            fontFamily = FontFamily(Font(R.font.montserrat_regular))
                        )
                    }

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = Color(0xFFEEEEEE)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                onClick = onNavigateToFeedback,
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            )
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF607D8B), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.icon_3),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.Unspecified
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = stringResource(R.string.feedback),
                            fontSize = 16.sp,
                            fontFamily = FontFamily(Font(R.font.montserrat_regular))
                        )
                    }

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = Color(0xFFEEEEEE)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF607D8B), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.icon_4),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.Unspecified
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = stringResource(R.string.privacy_policy),
                            fontSize = 16.sp,
                            fontFamily = FontFamily(Font(R.font.montserrat_regular)),
                            color = Color(0xFF222222)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.version_1_0_0),
                fontSize = 14.sp,
                fontFamily = FontFamily(Font(R.font.montserrat_regular)),
                color = Color(0xFF222222),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        // Banner 覆盖层（新版）
        Banner(
            state = bannerState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}