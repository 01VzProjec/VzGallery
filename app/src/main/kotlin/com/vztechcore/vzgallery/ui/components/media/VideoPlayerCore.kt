package com.vztechcore.vzgallery.ui.components.media

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

enum class FitMode { ORIGINAL, FILL, STRETCH }

/**
 * ExoPlayer rendering surface with aspect-ratio-aware sizing.
 *
 * Future: to support alternative players (VLC, mpv), create a parallel
 * "VlcPlayerCore" with the SAME signature and swap it in the orchestrator.
 */
@Composable
fun VideoPlayerCore(
    exoPlayer: ExoPlayer,
    fitMode: FitMode,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var videoAspect by remember { mutableStateOf(16f / 9f) }
    var isPrepared by remember { mutableStateOf(false) }

    LaunchedEffect(exoPlayer) {
        while (true) {
            val size = exoPlayer.videoSize
            if (size.width > 0 && size.height > 0) {
                val newAspect = size.width.toFloat() / size.height.toFloat()
                if (newAspect != videoAspect) videoAspect = newAspect
            }
            val ready = exoPlayer.playbackState == Player.STATE_READY
            if (ready != isPrepared) isPrepared = ready
            delay(100)
        }
    }

    val playerView = remember {
        PlayerView(context).apply {
            player = exoPlayer
            useController = false
            setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
            setKeepContentOnPlayerReset(true)
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    }

    LaunchedEffect(fitMode) {
        playerView.resizeMode = when (fitMode) {
            FitMode.ORIGINAL -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            FitMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            FitMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        }
    }

    DisposableEffect(Unit) {
        onDispose { playerView.player = null }
    }

    if (!isPrepared) return

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val containerWidthDp = maxWidth.value
        val containerHeightDp = maxHeight.value
        val containerAspect =
            if (containerHeightDp > 0f) containerWidthDp / containerHeightDp
            else 16f / 9f

        val viewModifier = when (fitMode) {
            FitMode.ORIGINAL -> {
                if (videoAspect >= containerAspect) {
                    Modifier.fillMaxWidth().aspectRatio(videoAspect)
                } else {
                    Modifier.fillMaxHeight().aspectRatio(videoAspect)
                }
            }
            FitMode.FILL, FitMode.STRETCH -> Modifier.fillMaxSize()
        }

        AndroidView(
            factory = { playerView },
            modifier = viewModifier
        )
    }
}