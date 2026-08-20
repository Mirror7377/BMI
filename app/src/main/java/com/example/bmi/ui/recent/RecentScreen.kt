package com.example.bmi.ui.recent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bmi.R
import com.example.bmi.data.database.BmiRecord
import com.example.bmi.ui.bmigauge.BmiClassifier
import com.example.bmi.utils.Banner
import com.example.bmi.utils.rememberBannerState
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun RecentScreen(
    viewModel: RecentViewModel,
    onRecordClick: (Long) -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val records = state.records

    val bannerState = rememberBannerState()

    // 收集全局事件
    LaunchedEffect(Unit) {
        viewModel.bannerEvent.collect { data ->
            bannerState.show(data.iconRes, data.message)
        }
    }

    // 首次加载时触发数据加载
    LaunchedEffect(Unit) {
        viewModel.handleIntent(RecentIntent.LoadRecords)
    }

    val bgGray = Color(0xFFEAEAEE)

    Box(modifier = Modifier.fillMaxSize()) {
        // 原有内容
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
                        .clickable { onNavigateBack() },
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = stringResource(R.string.recent),
                    fontSize = 20.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
                )
            }

            // 列表或空状态
            if (records.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(records, key = { it.id }) { record ->
                        RecentItem(record = record, onClick = { onRecordClick(record.id) })
                    }
                }
            }
        }

        // Banner 悬浮在顶层，覆盖所有内容
        Banner(
            state = bannerState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

/**
 * 单个历史记录卡片项
 */
@Composable
private fun RecentItem(
    record: BmiRecord,
    onClick: () -> Unit
) {
    // 根据年龄判断 BMI 等级
    val bmiLevel = if (record.age > 20) {
        BmiClassifier.classifyAdult(record.bmi)
    } else {
        BmiClassifier.classifyChild(record.age, record.gender, record.bmi)
    }

    // 格式化日期
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val dateStr = dateFormat.format(record.timestamp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
            onClick = { onClick() },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 20.dp,
                    end = 15.dp,
                    top = 12.dp,
                    bottom = 12.dp
                )
        ) {


            Column(
                modifier = Modifier.align(Alignment.CenterStart)
            ) {

                // BMI 数值
                Text(
                    text = String.format("%.1f", record.bmi),
                    fontSize = 28.sp,
                    fontFamily = FontFamily(
                        Font(R.font.montserrat_extrabold)
                    )
                )

                // 状态
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                Color(bmiLevel.cardBgColor)
                            )
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Text(
                        text = stringResource(
                            bmiLevel.statusTextRes
                        ),
                        fontSize = 16.sp,
                        fontFamily = FontFamily(
                            Font(R.font.montserrat_regular)
                        ),
                        // 只允许一行
                        maxLines = 1,
                        // 禁止自动换行
                        softWrap = false,
                        // 超出自己的布局范围后继续绘制
                        overflow = TextOverflow.Visible
                    )
                }
            }


            Row(
                modifier = Modifier.align(
                    Alignment.CenterEnd
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    horizontalAlignment = Alignment.End
                ) {

                    Text(
                        text = dateStr,
                        fontSize = 14.sp,
                        fontFamily = FontFamily(
                            Font(R.font.montserrat_regular)
                        ),
                        lineHeight = 14.sp
                    )

                    Spacer(
                        modifier = Modifier.height(2.dp)
                    )

                    Text(
                        text = record.timeOfDay,
                        fontSize = 14.sp,
                        fontFamily = FontFamily(
                            Font(R.font.montserrat_regular)
                        ),
                        lineHeight = 14.sp
                    )
                }

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Icon(
                    painter = painterResource(
                        id = R.drawable.arrow_right
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}