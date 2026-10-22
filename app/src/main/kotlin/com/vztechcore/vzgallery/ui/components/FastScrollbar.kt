package com.vztechcore.vzgallery.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import com.vztechcore.vzgallery.ui.theme.VzSurfaceVariant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val TRACK_HEIGHT = 700.dp
private val TRACK_WIDTH = 3.dp
private val BUBBLE_WIDTH = 20.dp
private val BUBBLE_HEIGHT = 44.dp
private val END_PADDING = 8.dp
private const val HIDE_DELAY_MS = 1200L

@Composable
fun FastScrollbar(
    gridState: LazyGridState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val layoutInfo by remember { derivedStateOf { gridState.layoutInfo } }
    val totalItems = layoutInfo.totalItemsCount
    if (totalItems == 0) return

    val visibleCount = layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
    val firstVisible = gridState.firstVisibleItemIndex
    val maxScrollIndex = (totalItems - visibleCount).coerceAtLeast(1)

    // Fração derivada do scroll real da lista
    val scrollFraction: Float = when {
        totalItems <= visibleCount -> 0f
        firstVisible >= maxScrollIndex -> 1f
        else -> (firstVisible.toFloat() / maxScrollIndex).coerceIn(0f, 1f)
    }

    // Enquanto arrasta, a bolha usa dragFraction (que vem do dedo).
    // Fora disso, usa scrollFraction (que vem da lista).
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableStateOf(0f) }

    val positionFraction = if (isDragging) dragFraction else scrollFraction

    // Auto-hide
    val isListScrolling by remember { derivedStateOf { gridState.isScrollInProgress } }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(isListScrolling, isDragging) {
        if (isListScrolling || isDragging) {
            visible = true
        } else {
            delay(HIDE_DELAY_MS)
            visible = false
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(220),
        label = "scrollbar_alpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .padding(end = END_PADDING)
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        val availableHeight = maxHeight
        val trackHeight = minOf(TRACK_HEIGHT, availableHeight * 0.9f)

        Box(
            modifier = Modifier
                .width(BUBBLE_WIDTH)
                .height(trackHeight),
            contentAlignment = Alignment.Center
        ) {
            val trackHeightPx = with(density) { trackHeight.toPx() }
            val bubbleHeightPx = with(density) { BUBBLE_HEIGHT.toPx() }
            val halfBubblePx = bubbleHeightPx / 2f
            // Range de movimento da bolha dentro do track
            val availableRangePx = (trackHeightPx - bubbleHeightPx).coerceAtLeast(1f)

            val bubbleTopOffsetDp = with(density) {
                (availableRangePx * positionFraction).toDp()
            }

            // ===== Track visual =====
            Box(
                modifier = Modifier
                    .width(TRACK_WIDTH)
                    .fillMaxHeight()
                    .background(
                        color = VzSurfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(50)
                    )
            )

            // ===== Bolha visual (não recebe toque) =====
            val bubbleColor = if (isDragging) VzSecondary
            else VzSurfaceVariant.copy(alpha = 0.75f)
            val iconTint = if (isDragging) Color.White else VzSecondary

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = bubbleTopOffsetDp)
                    .width(BUBBLE_WIDTH)
                    .height(BUBBLE_HEIGHT)
                    .clip(RoundedCornerShape(50))
                    .background(bubbleColor),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(12.dp)
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // ===== Zona de toque (cobre o track inteiro, invisível) =====
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(totalItems, availableRangePx) {
                        // Converte um Y absoluto no track para uma fração 0..1
                        fun fractionForY(y: Float): Float {
                            return ((y - halfBubblePx) / availableRangePx).coerceIn(0f, 1f)
                        }

                        fun applyFraction(fraction: Float) {
                            dragFraction = fraction
                            val targetIndex = (fraction * maxScrollIndex)
                                .roundToInt()
                                .coerceIn(0, (totalItems - 1).coerceAtLeast(0))
                            scope.launch {
                                gridState.scrollToItem(targetIndex)
                            }
                        }

                        detectDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                applyFraction(fractionForY(offset.y))
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                applyFraction(fractionForY(change.position.y))
                            },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false }
                        )
                    }
            )
        }
    }
}