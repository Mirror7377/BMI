package com.example.bmi.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bmi.R
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun AgePicker(
    selectedAge: Int,
    onAgeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {

    val focusManager = LocalFocusManager.current
    val ages = remember { (2..99).toList() }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val itemWidth = 47.dp
    val itemSpace = 18.dp
    val itemWidthPx = with(density) { itemWidth.toPx() }
    val itemSpacePx = with(density) { itemSpace.toPx() }
    val itemUnitPx = itemWidthPx + itemSpacePx

    val boldFont = FontFamily(Font(R.font.montserrat_extrabold))

    var isUserScrolling by remember { mutableStateOf(false) }
    var lastSelectedAge by remember { mutableStateOf(selectedAge) }

    val initialIndex = (selectedAge - 2).coerceIn(0, ages.lastIndex)

    // 使用 initialFirstVisibleItemIndex 让第一帧直接停在正确位置
    // 配合 contentPadding 左右各 130dp，目标项会自动居中
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)


    LaunchedEffect(selectedAge) {
        if (isUserScrolling) return@LaunchedEffect
        if (selectedAge == lastSelectedAge) return@LaunchedEffect

        val targetIndex = (selectedAge - 2).coerceIn(0, ages.lastIndex)
        lastSelectedAge = selectedAge
        //执行滚动
        listState.animateScrollToItem(targetIndex)
    }


    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            focusManager.clearFocus()
            isUserScrolling = true
            return@LaunchedEffect
        }
        if (!isUserScrolling) return@LaunchedEffect

        val layoutInfo = listState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) {
            isUserScrolling = false
            return@LaunchedEffect
        }

        val center = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
        val centerItem = visibleItems.minByOrNull { item ->
            abs(item.offset + item.size / 2 - center)
        }

        centerItem?.let { item ->
            val itemCenter = item.offset + item.size / 2
            val distance = itemCenter - center
            if (distance != 0) {
                listState.animateScrollBy(distance.toFloat())
            }

            // 吸附后再精确取一次中心项，确保回调正确
            val finalInfo = listState.layoutInfo
            val finalCenter = (finalInfo.viewportStartOffset + finalInfo.viewportEndOffset) / 2
            val finalItem = finalInfo.visibleItemsInfo.minByOrNull {
                abs(it.offset + it.size / 2 - finalCenter)
            }
            finalItem?.let {
                val age = ages[it.index]
                if (age != lastSelectedAge) {
                    lastSelectedAge = age
                    onAgeSelected(age)
                }
            }
        }
        isUserScrolling = false
    }

    val visibleItemEffects by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visible = layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) return@derivedStateOf emptyMap<Int, Pair<Float, Color>>()

            val center = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
            val maxDistance = itemUnitPx * 2f

            visible.associate { item ->
                val itemCenter = item.offset + item.size / 2f
                val distance = abs(itemCenter - center)
                val ratio = (distance / maxDistance).coerceIn(0f, 1f)
                val alpha = 1f - ratio * 0.65f
                val color = lerp(Color.Black, Color(0xFFBBBBBB), ratio)
                item.index to (alpha to color)
            }
        }
    }

    val fallbackEffects = remember(initialIndex) {
        val maxDistance = itemUnitPx * 2f
        ages.indices.associateWith { idx ->
            val distance = abs(idx - initialIndex) * itemUnitPx
            val ratio = (distance / maxDistance).coerceIn(0f, 1f)
            val alpha = 1f - ratio * 0.65f
            val color = lerp(Color.Black, Color(0xFFBBBBBB), ratio)
            (alpha to color)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.white)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(307.dp)
                    .height(39.dp)
            ) {
                LazyRow(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(39.dp),
                    horizontalArrangement = Arrangement.spacedBy(itemSpace),
                    contentPadding = PaddingValues(horizontal = 130.dp)
                ) {
                    itemsIndexed(
                        items = ages,
                        key = { _, age -> age }
                    ) { index, age ->
                        // 优先使用精确计算，未就绪时使用 fallback，彻底避免闪烁
                        val (alpha, textColor) = visibleItemEffects[index]
                            ?: fallbackEffects[index]
                            ?: (1f to Color.Black)

                        Box(
                            modifier = Modifier
                                .width(itemWidth)
                                .height(39.dp)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    focusManager.clearFocus()
                                    lastSelectedAge = age
                                    onAgeSelected(age)
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(index)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = age.toString(),
                                style = TextStyle(
                                    fontSize = 32.sp,
                                    fontFamily = boldFont,
                                    color = textColor
                                ),
                                modifier = Modifier.graphicsLayer {
                                    this.alpha = alpha
                                }
                            )
                        }
                    }
                }
            }

            Image(
                painter = painterResource(R.drawable.ic_triangle_blue),
                contentDescription = null,
                modifier = Modifier
                    .size(width = 15.dp, height = 11.dp)
                    .align(Alignment.BottomCenter)
            )
        }
    }
}