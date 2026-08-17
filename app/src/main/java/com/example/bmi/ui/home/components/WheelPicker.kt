package com.example.bmi.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bmi.R
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

@Composable
fun WheelPicker(
    items: List<String>,
    initialIndex: Int = 0,
    itemHeight: Dp = 25.dp,
    visibleItems: Int = 7,
    lineLength: Dp = 40.dp,
    modifier: Modifier = Modifier,
    onItemSelected: (Int) -> Unit
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex.coerceAtMost(items.size - 1)
    )

    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.roundToPx() }

    // 渐变最大距离：上下两侧逐渐变淡
    val maxDistance = itemHeightPx * 1.5f

    // 滚动停止后，找到真正位于中心的 item
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { isScrolling ->
                if (!isScrolling) {
                    val layoutInfo = listState.layoutInfo
                    val center = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f

                    val targetIndex = layoutInfo.visibleItemsInfo
                        .minByOrNull { itemInfo ->
                            val itemCenter = itemInfo.offset + itemInfo.size / 2f
                            abs(itemCenter - center)
                        }
                        ?.index

                    targetIndex?.let { onItemSelected(it) }
                }
            }
    }

    // Snap：让 item 最终停在正中间
    val flingBehavior = rememberSnapFlingBehavior(
        lazyListState = listState,
        snapPosition = SnapPosition.Center
    )

    val pickerHeight = itemHeight * visibleItems

    // 计算每个 item 的透明度
    val visibleItemAlpha by derivedStateOf {
        val layoutInfo = listState.layoutInfo
        if (layoutInfo.visibleItemsInfo.isEmpty()) {
            emptyMap()
        } else {
            val center = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
            layoutInfo.visibleItemsInfo.associate { itemInfo ->
                val itemCenter = itemInfo.offset + itemInfo.size / 2f
                val distance = abs(itemCenter - center)
                val ratio = (distance / maxDistance).coerceIn(0f, 1f)
                val alpha = 1f - ratio * 0.9f
                itemInfo.index to alpha
            }
        }
    }

    Box(
        modifier = modifier
            .height(pickerHeight)
            .clipToBounds()
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                vertical = (pickerHeight - itemHeight) / 2
            )
        ) {
            itemsIndexed(items) { index, text ->
                val alphaValue = visibleItemAlpha[index] ?: 0.25f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text,
                        fontSize = 16.sp,
                        fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.alpha(alphaValue)
                    )
                }
            }
        }

        // 两条蓝色选中线
        val lineColor = colorResource(R.color.bg_rounded_blue)
        val lineHeight = 0.5.dp

        Box(
            modifier = Modifier
                .width(lineLength)
                .height(lineHeight)
                .align(Alignment.Center)
                .offset(y = -itemHeight / 2)
                .background(lineColor)
        )

        Box(
            modifier = Modifier
                .width(lineLength)
                .height(lineHeight)
                .align(Alignment.Center)
                .offset(y = itemHeight / 2)
                .background(lineColor)
        )
    }
}