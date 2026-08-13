package com.example.bmi.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bmi.R

@Composable
fun StarRating(rating: Double) {
    val maxStars = 5
    val fullStars = rating.toInt()          // 满星数量
    val hasHalfStar = rating - fullStars >= 0.5  // 是否有半星

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(19.dp)
    ) {
        repeat(maxStars) { index ->
            val starIcon = when {
                index < fullStars -> Icons.Filled.Star          // 满星
                index == fullStars && hasHalfStar -> Icons.AutoMirrored.Filled.StarHalf  // 半星
                else -> Icons.Outlined.Star                     // 空星
            }

            Icon(
                imageVector = starIcon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = String.format("%.1f", rating),
            fontSize = 12.sp,
            fontFamily = FontFamily(Font(R.font.montserrat_regular)),
        )
    }
}
