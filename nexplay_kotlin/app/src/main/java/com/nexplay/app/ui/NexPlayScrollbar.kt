package com.nexplay.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun NexPlayScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val layoutInfo = listState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    val totalItems = layoutInfo.totalItemsCount

    if (visibleItems.isEmpty()) return
    if (totalItems <= visibleItems.size) return

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var containerHeightPx by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(listState.isScrollInProgress, isDragging, totalItems) {
        if (listState.isScrollInProgress || isDragging) {
            isVisible = true
        } else {
            delay(700)
            if (!listState.isScrollInProgress && !isDragging) {
                isVisible = false
            }
        }
    }

    val averageItemSizePx =
        visibleItems
            .map { it.size }
            .average()
            .toFloat()
            .coerceAtLeast(1f)

    val viewportPx =
        (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset)
            .toFloat()
            .coerceAtLeast(1f)

    val estimatedContentPx =
        (averageItemSizePx * totalItems)
            .coerceAtLeast(viewportPx)

    val currentScrollPx =
        (
            (listState.firstVisibleItemIndex * averageItemSizePx) +
                listState.firstVisibleItemScrollOffset
        ).coerceAtLeast(0f)

    val maxScrollPx =
        (estimatedContentPx - viewportPx)
            .coerceAtLeast(1f)

    val scrollProgress =
        (currentScrollPx / maxScrollPx)
            .coerceIn(0f, 1f)

    val minThumbHeightPx =
        with(density) { 36.dp.toPx() }

    val thumbHeightPx =
        if (containerHeightPx <= 0f) {
            minThumbHeightPx
        } else {
            val effectiveMinThumbHeightPx =
                minThumbHeightPx
                    .coerceAtMost(
                        containerHeightPx,
                    )

            ((viewportPx / estimatedContentPx) * containerHeightPx)
                .coerceIn(
                    effectiveMinThumbHeightPx,
                    containerHeightPx,
                )
        }

    val maxThumbOffsetPx =
        (containerHeightPx - thumbHeightPx)
            .coerceAtLeast(0f)

    val thumbOffsetPx =
        (scrollProgress * maxThumbOffsetPx)
            .coerceIn(0f, maxThumbOffsetPx)

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "nexplay_scrollbar_alpha",
    )

    Box(
        modifier =
            modifier
                .fillMaxHeight()
                .width(20.dp)
                .padding(
                    top = 12.dp,
                    bottom = 12.dp,
                    end = 6.dp,
                )
                .onSizeChanged { size ->
                    containerHeightPx = size.height.toFloat()
                }
                .alpha(animatedAlpha)
                .pointerInput(totalItems, containerHeightPx) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true

                            if (containerHeightPx > 0f) {
                                val proportion =
                                    (offset.y / containerHeightPx)
                                        .coerceIn(0f, 1f)

                                val targetIndex =
                                    (proportion * (totalItems - 1))
                                        .roundToInt()
                                        .coerceIn(0, totalItems - 1)

                                scope.launch {
                                    listState.scrollToItem(targetIndex)
                                }
                            }
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()

                            if (containerHeightPx > 0f) {
                                val proportion =
                                    (change.position.y / containerHeightPx)
                                        .coerceIn(0f, 1f)

                                val targetIndex =
                                    (proportion * (totalItems - 1))
                                        .roundToInt()
                                        .coerceIn(0, totalItems - 1)

                                scope.launch {
                                    listState.scrollToItem(targetIndex)
                                }
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                    )
                },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.32f,
                        ),
                    ),
        )

        Box(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .offset {
                        IntOffset(
                            x = 0,
                            y = thumbOffsetPx.roundToInt(),
                        )
                    }
                    .width(6.dp)
                    .height(
                        with(density) {
                            thumbHeightPx.toDp()
                        },
                    )
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.95f,
                        ),
                    ),
        )
    }
}