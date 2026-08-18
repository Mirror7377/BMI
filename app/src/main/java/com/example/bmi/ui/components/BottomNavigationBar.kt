package com.example.bmi.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bmi.R
import com.example.bmi.ui.navigation.Screen

@Composable
fun BottomNavigationBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {

    NavigationBar(
        containerColor = Color.White,        // 白色背景，与旧 XML 一致
        tonalElevation = 0.dp,               // 去掉 M3 默认阴影
        modifier = Modifier.height(60.dp)    // 与旧 XML 高度一致
    ) {
        // 定义导航项：Screen 映射到 (图标资源, 文字资源)
        val items = listOf(
            Screen.Home to (R.drawable.ic_home to R.string.calculator),
            Screen.Display to (R.drawable.ic_display to R.string.bmi),
            Screen.Statistics to (R.drawable.ic_statistics to R.string.statistics)
        )

        items.forEach { (screen, resources) ->
            val (iconRes, titleRes) = resources
            val isSelected = currentScreen == screen

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(screen) },
                icon = {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = stringResource(titleRes)
                    )
                },
                label = {
                    Text(
                        text = stringResource(titleRes),
                        fontSize = 12.sp
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    // ★ 选中状态：改为黑色
                    selectedIconColor = Color.Black,
                    selectedTextColor = Color.Black,
                    // ★ 未选中状态：保持灰色
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    // ★ 去掉 M3 默认的选中背景指示器
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}