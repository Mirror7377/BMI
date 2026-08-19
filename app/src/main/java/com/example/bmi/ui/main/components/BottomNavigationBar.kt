package com.example.bmi.ui.main.components

import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bmi.R
import com.example.bmi.ui.navigation.DisplayRoute
import com.example.bmi.ui.navigation.HomeRoute
import com.example.bmi.ui.navigation.StatisticsRoute

@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onNavigate: (Any) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp,
        modifier = Modifier.height(60.dp)
    ) {
        val items = listOf(
            Triple(HomeRoute, R.drawable.ic_home, R.string.calculator),
            Triple(DisplayRoute, R.drawable.ic_display, R.string.bmi),
            Triple(StatisticsRoute, R.drawable.ic_statistics, R.string.statistics)
        )

        items.forEach { (route, iconRes, titleRes) ->
            val isSelected = currentRoute == route::class.qualifiedName

            NavigationBarItem(
                selected = isSelected,
                onClick = { if (!isSelected) onNavigate(route) },
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
                    selectedIconColor = Color.Black,
                    selectedTextColor = Color.Black,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}