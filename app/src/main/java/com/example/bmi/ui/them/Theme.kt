package com.example.bmi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bmi.R

// ===== 颜色定义  =====
val BmiLightColors = lightColorScheme(
    primary = Color(0xFF3659CF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8EDFA),
    onPrimaryContainer = Color(0xFF1A2B6B),
    secondary = Color(0xFF54A529),
    onSecondary = Color.White,
    tertiary = Color(0xFFFFA100),
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF222222),
    surface = Color.White,
    onSurface = Color(0xFF222222),
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = Color(0xFF888888),
)

val BmiDarkColors = darkColorScheme(
    primary = Color(0xFF6B8CE0),
    onPrimary = Color(0xFF0D1B3E),
    primaryContainer = Color(0xFF1A2B6B),
    onPrimaryContainer = Color(0xFFD0DDFF),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
)

// ===== 字体 =====
val BmiTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.009).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.009).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.009).sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily(Font(R.font.montserrat_regular)),
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.009).sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily(Font(R.font.montserrat_regular)),
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.009).sp
    ),
)

// ===== 形状 =====
val BmiShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(18.dp),
)

// ===== 主题 Composable =====
@Composable
fun BmiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) BmiDarkColors else BmiLightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = BmiTypography,
        shapes = BmiShapes,
        content = content
    )
}