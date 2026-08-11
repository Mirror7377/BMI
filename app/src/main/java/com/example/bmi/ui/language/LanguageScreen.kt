package com.example.bmi.ui.language

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bmi.R

/**
 * 语言页面（Compose 版本）
 * 展示 11 种语言列表，点击选中后切换语言并重启 App
 *
 * @param viewModel 语言 ViewModel
 * @param onNavigateToMain 导航到主页的回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(
    viewModel: LanguageViewModel,
    onNavigateToMain: () -> Unit,
    onNavigateBack: () -> Unit
) {
    // 订阅状态
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedLanguage = state.selectedLanguage

    // 订阅 Effect（导航事件）
    LaunchedEffect(Unit) {
        // 加载已保存的语言
        viewModel.handleIntent(LanguageIntent.LoadSavedLanguage)

        viewModel.effect.collect { effect ->
            when (effect) {
                is LanguageEffect.NavigateToMain -> onNavigateToMain()
            }
        }
    }

    // ----- 语言数据（完全对应原始 XML） -----
    val languages = listOf(
        LanguageItem("en", R.string.english),
        LanguageItem("pt", R.string.portugu_s),
        LanguageItem("ru", R.string.pycc),
        LanguageItem("de", R.string.deutsch),
        LanguageItem("zh-TW", R.string.ch_tw),
        LanguageItem("zh-CN", R.string.ch),
        LanguageItem("fr", R.string.fran_ais),
        LanguageItem("es", R.string.espa_ol),
        LanguageItem("it", R.string.italiano),
        LanguageItem("ko", R.string.hg)
    )

    val bgGray = Color(0xFFF5F5F5)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGray)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()        // 自动适配状态栏高度
                .padding(horizontal = 15.dp), // 左右内边距,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回箭头
            Icon(
                painter = painterResource(id = R.drawable.arrow_left),
                contentDescription = "Back",
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        onClick = onNavigateBack,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ),
                tint = Color(0xFF222222) // text_black
            )

            Spacer(modifier = Modifier.width(10.dp))

            // 标题（使用 language_options）
            Text(
                text = stringResource(R.string.language_options),
                fontSize = 20.sp,
                fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
                color = Color(0xFF222222) // text_black
            )
        }

        // ---- 白色卡片容器 ----
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp)
                .padding(top = 20.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            ) {
                // 使用索引遍历，因为需要控制分割线
                languages.forEachIndexed { index, item ->
                    // 语言项
                    LanguageItemRow(
                        languageName = stringResource(item.nameResId),
                        isSelected = item.code == selectedLanguage,
                        onClick = {
                            viewModel.handleIntent(LanguageIntent.SelectLanguage(item.code))
                        }
                    )

                    // 分割线（最后一项不显示）
                    if (index < languages.size - 1) {
                        Divider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 15.dp),
                            color = Color(0xFFEEEEEE), // line 颜色
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 语言列表项数据类
 */
private data class LanguageItem(
    val code: String,
    val nameResId: Int
)

/**
 * 单个语言行（文字 + 右侧勾选图标）
 */
@Composable
private fun LanguageItemRow(
    languageName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 15.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = languageName,
            fontSize = 16.sp,
            fontFamily = FontFamily(Font(R.font.montserrat_regular)),
            color = Color(0xFF222222) // text_black
        )

        if (isSelected) {
            Icon(
                painter = painterResource(id = R.drawable.check),
                contentDescription = "Selected",
                modifier = Modifier.size(24.dp),
                tint = Color(0xFF3659CF) // splash_blue / primary
            )
        } else {
            // 保持占位，让文字保持左对齐
            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}