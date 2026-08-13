package com.example.bmi.ui.common

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.example.bmi.R
import com.example.bmi.data.database.RecommendApp

@Composable
fun AppRecommendations(
    apps: List<RecommendApp>,
    dateTime: String? = null
) {
    Spacer(modifier = Modifier.height(30.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // ---- 分隔线（横线 + 可选日期） ----
        if (dateTime != null) {
            // History 页面：左右横线 + 中间日期
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = 0.5.dp,
                    color = Color(0xFFCCCCCC)
                )
                Text(
                    text = dateTime,
                    fontSize = 12.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
                    color = Color(0xFFCCCCCC),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = 0.5.dp,
                    color = Color(0xFFCCCCCC)
                )
            }
        } else {
            // Result 页面：只有一条横线
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 0.5.dp,
                color = Color(0xFFCCCCCC)
            )
        }

        Spacer(modifier = Modifier.height(25.dp))

        // "Apps you might need" 标题
        Text(
            text = stringResource(R.string.apps_you_might_need),
            fontSize = 16.sp,
            fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
            color = Color(0xFF222222)
        )

        Spacer(modifier = Modifier.height(12.dp))

        apps.forEach { app ->
            AppRecommendationCard(app = app)
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun AppRecommendationCard(app: RecommendApp) {
    val appContext = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F4F4)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val url = "https://play.google.com/store/apps/details?id=${app.packageName}"
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (intent.resolveActivity(appContext.packageManager) != null) {
                        appContext.startActivity(intent)
                    }
                }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = app.iconResId),
                contentDescription = app.name,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray, RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    fontSize = 13.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_regular)),
                    maxLines = 1,
                    modifier = Modifier.height(19.dp)
                )
                Text(
                    text = app.category,
                    fontSize = 13.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_regular)),
                    modifier = Modifier.height(19.dp)
                )
                StarRating(rating = app.rating)
            }
        }
    }
}