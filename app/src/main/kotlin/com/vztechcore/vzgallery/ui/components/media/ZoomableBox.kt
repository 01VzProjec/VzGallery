package com.vztechcore.vzgallery.ui.components.media

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f
private const val DOUBLE_TAP_TIMEOUT = 300L

/**
 * Generic pinch/pan/double-tap zoom container.
 *
 * Wraps any composable content (image, video player, etc) with the same
 * gesture behavior. Used by PhotoZoomable (image) and by ViewerScreen
 * for video playback.
 *
 * Gestures:
 *  - 2 fingers pinch -> zoom in/out (1x..5x)
 *  - 1 finger pan (while zoomed) -> pan the content
 *  - double tap -> toggle between 1x and 2.5x (focused on tap point)
 *  - single tap (with 300ms delay to disambiguate from double tap) -> onSingleTap
 */
@Composable
fun ZoomableBox(
    contentKey: Any,
    modifier: Modifier = Modifier,
    isCurrentPage: Boolean = true,
    onZoomStateChange: (Boolean) -> Unit = {},
    onSingleTap: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()

    var scale by remember(contentKey) { mutableStateOf(1f) }
    var offset by remember(contentKey) { mutableStateOf(Offset.Zero) }
    var lastTapTime by remember(contentKey) { mutableStateOf(0L) }
    var pendingTapJob by remember(contentKey) { mutableStateOf<Job?>(null) }

    val isZoomed = scale > 1.01f

    LaunchedEffect(isZoomed) { onZoomStateChange(isZoomed) }

    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            scale = 1f
            offset = Offset.Zero
            onZoomStateChange(false)
        }
    }

    DisposableEffect(Unit) {
        onDispose { pendingTapJob?.cancel() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(contentKey) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downPos = down.position
                    val downTime = System.currentTimeMillis()
                    var totalMovement = 0f
                    var maxPointers = 1
                    var consumedByGesture = false

                    do {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.count { it.pressed }
                        if (pressed > maxPointers) maxPointers = pressed

                        if (pressed >= 2) {
                            val zoom = event.calculateZoom()
                            if (zoom != 1f && zoom.isFinite() && zoom > 0f) {
                                val newScale = (scale * zoom).coerceIn(1f, MAX_ZOOM)
                                if (newScale <= 1.01f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = newScale
                                }
                                consumedByGesture = true
                            }
                            if (scale > 1f) {
                                val pan = event.calculatePan()
                                if (pan != Offset.Zero) {
                                    val maxX = (size.width * (scale - 1f)) / 2f
                                    val maxY = (size.height * (scale - 1f)) / 2f
                                    offset = Offset(
                                        x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                        y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                    )
                                }
                            }
                            event.changes.forEach { if (it.pressed) it.consume() }
                        } else if (pressed == 1 && scale > 1f) {
                            val pan = event.calculatePan()
                            if (pan != Offset.Zero) {
                                val maxX = (size.width * (scale - 1f)) / 2f
                                val maxY = (size.height * (scale - 1f)) / 2f
                                offset = Offset(
                                    x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                    y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                )
                                consumedByGesture = true
                                event.changes.forEach { if (it.pressed) it.consume() }
                            }
                        }

                        event.changes.forEach { c ->
                            if (c.pressed) totalMovement += c.positionChange().getDistance()
                        }
                    } while (event.changes.any { it.pressed })

                    val duration = System.currentTimeMillis() - downTime
                    if (!consumedByGesture && maxPointers == 1 && totalMovement < 20f && duration < 400) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime < DOUBLE_TAP_TIMEOUT) {
                            pendingTapJob?.cancel()
                            pendingTapJob = null
                            lastTapTime = 0
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                val s = DOUBLE_TAP_ZOOM
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val dx = downPos.x - cx
                                val dy = downPos.y - cy
                                val maxX = (size.width * (s - 1f)) / 2f
                                val maxY = (size.height * (s - 1f)) / 2f
                                scale = s
                                offset = Offset(
                                    x = (-(s - 1f) * dx).coerceIn(-maxX, maxX),
                                    y = (-(s - 1f) * dy).coerceIn(-maxY, maxY)
                                )
                            }
                        } else {
                            lastTapTime = now
                            pendingTapJob?.cancel()
                            pendingTapJob = scope.launch {
                                delay(DOUBLE_TAP_TIMEOUT)
                                onSingleTap()
                                lastTapTime = 0
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        ) {
            content()
        }
    }
}