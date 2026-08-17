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
    val ages = remember { (2..99).toList() }
    val listState = rememberLazyListState()
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

    // 初始定位
    LaunchedEffect(Unit) {
        val index = (selectedAge - 2).coerceIn(0, ages.lastIndex)
        listState.scrollToItem(index)
    }

    // 外部年龄变化时同步滚动
    LaunchedEffect(selectedAge) {
        if (isUserScrolling) return@LaunchedEffect
        if (selectedAge == lastSelectedAge) return@LaunchedEffect

        val targetIndex = (selectedAge - 2).coerceIn(0, ages.lastIndex)
        lastSelectedAge = selectedAge
        listState.animateScrollToItem(targetIndex)
    }

    // 监听滑动状态，滚动停止后自动吸附并回调
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            isUserScrolling = true
            return@LaunchedEffect
        }

        if (!isUserScrolling) return@LaunchedEffect

        val layoutInfo = listState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) return@LaunchedEffect

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

            val finalLayoutInfo = listState.layoutInfo
            val finalCenter = (finalLayoutInfo.viewportStartOffset + finalLayoutInfo.viewportEndOffset) / 2

            val finalItem = finalLayoutInfo.visibleItemsInfo.minByOrNull { finalItem ->
                abs(finalItem.offset + finalItem.size / 2 - finalCenter)
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
                        val itemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull {
                            it.index == index
                        }

                        val viewportCenter = (
                                listState.layoutInfo.viewportStartOffset +
                                        listState.layoutInfo.viewportEndOffset
                                ) / 2f

                        val itemCenter = itemInfo?.let {
                            it.offset + it.size / 2f
                        }

                        val distance = if (itemCenter != null) {
                            abs(itemCenter - viewportCenter)
                        } else {
                            Float.MAX_VALUE
                        }

                        val maxDistance = itemUnitPx * 2f
                        val ratio = (distance / maxDistance).coerceIn(0f, 1f)
                        val alpha = 1f - ratio * 0.65f
                        val textColor = lerp(Color.Black, Color(0xFFBBBBBB), ratio)
                        val scale = 1f - ratio * 0.10f

                        Box(
                            modifier = Modifier
                                .width(itemWidth)
                                .height(39.dp)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
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
                                    scaleX = scale
                                    scaleY = scale
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