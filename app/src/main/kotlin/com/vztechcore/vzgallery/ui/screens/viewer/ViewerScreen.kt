package com.vztechcore.vzgallery.ui.screens.viewer

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import coil.size.Precision
import com.vztechcore.vzgallery.data.Prefs
import com.vztechcore.vzgallery.data.SnackbarBus
import com.vztechcore.vzgallery.model.MediaItem
import com.vztechcore.vzgallery.model.ehVideo
import com.vztechcore.vzgallery.ui.components.media.FitMode
import com.vztechcore.vzgallery.ui.components.media.PhotoZoomable
import com.vztechcore.vzgallery.ui.components.media.VideoCenterControls
import com.vztechcore.vzgallery.ui.components.media.VideoPlayerCore
import com.vztechcore.vzgallery.ui.components.media.ZoomableBox
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzOnSurfaceVariant
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import com.vztechcore.vzgallery.ui.theme.VzSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val s = ms / 1000
    val m = s / 60
    val sec = s % 60
    return "%02d:%02d".format(m, sec)
}

private fun frameMillisFor(durationMs: Long?): Long {
    return if ((durationMs ?: 0L) > 3000L) {
        (durationMs!! / 4L).coerceAtLeast(500L)
    } else {
        1000L
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ViewerScreen(
    items: List<MediaItem>,
    startIndex: Int,
    getCurrentBounds: (MediaItem) -> Rect?,
    onClose: (Int) -> Unit,
    onDelete: (MediaItem) -> Unit,
    onHide: (MediaItem) -> Unit,
    onShare: (MediaItem) -> Unit,
    hiddenMode: Boolean = false,
    trashMode: Boolean = false,
    onRestore: ((MediaItem) -> Unit)? = null,
    onDeletePermanently: ((MediaItem) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activity = context as? Activity
    val prefs = remember { Prefs(context) }

    val favoritesSet by prefs.favorites.collectAsState(initial = emptySet())

    val pagerState = rememberPagerState(initialPage = startIndex) { items.size }
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val reveal = remember { Animatable(0f) }
    var revealDone by remember { mutableStateOf(false) }
    var isClosing by remember { mutableStateOf(false) }
    var contentLoaded by remember { mutableStateOf(false) }
    val initialPage = startIndex

    var exitBounds by remember { mutableStateOf<Rect?>(null) }
    var exitItemIndex by remember { mutableStateOf(initialPage) }

    var controlsVisible by remember { mutableStateOf(true) }
    var interactionId by remember { mutableStateOf(0) }
    var submenuOpen by remember { mutableStateOf(false) }

    var landscapeMode by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }
    var speed by remember { mutableStateOf(1.0f) }
    var fitMode by remember { mutableStateOf(FitMode.ORIGINAL) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showHideDialog by remember { mutableStateOf(false) }

    var isScrubbing by remember { mutableStateOf(false) }
    var wasPlayingBeforeScrub by remember { mutableStateOf(false) }

    var pagerScrollEnabled by remember { mutableStateOf(true) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            setSeekParameters(SeekParameters.EXACT)
        }
    }

    val isPlayingFlow = remember { MutableStateFlow(false) }
    val isPreparedFlow = remember { MutableStateFlow(false) }
    val positionFlow = remember { MutableStateFlow(0L) }
    val durationFlow = remember { MutableStateFlow(0L) }

    val isPlaying by isPlayingFlow.collectAsState()
    val isPrepared by isPreparedFlow.collectAsState()
    val positionMs by positionFlow.collectAsState()
    val durationMs by durationFlow.collectAsState()

    val currentPage = pagerState.currentPage
    val currentItem = items.getOrNull(currentPage)
    val isCurrentVideo = currentItem?.ehVideo == true
    val isFavorite = currentItem?.uri?.toString() in favoritesSet

    val alphaPager by animateFloatAsState(
        targetValue = if (revealDone && !isClosing) 1f else 0f,
        animationSpec = tween(180),
        label = "pager_alpha"
    )

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlayingFlow.value = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val ready = playbackState == Player.STATE_READY
                isPreparedFlow.value = ready
                if (ready) durationFlow.value = exoPlayer.duration.coerceAtLeast(0L)
                if (playbackState == Player.STATE_ENDED) {
                    try {
                        exoPlayer.pause()
                        exoPlayer.seekTo(0L)
                        positionFlow.value = 0L
                        isPlayingFlow.value = false
                    } catch (_: Exception) {}
                }
            }

            override fun onPlayerError(error: PlaybackException) {}
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            try { exoPlayer.release() } catch (_: Exception) {}
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            if (exoPlayer.isPlaying && !isScrubbing) {
                positionFlow.value = exoPlayer.currentPosition.coerceAtLeast(0L)
            }
            delay(200)
        }
    }

    LaunchedEffect(currentPage, currentItem?.uri) {
        val item = items.getOrNull(currentPage) ?: return@LaunchedEffect
        isPreparedFlow.value = false
        isPlayingFlow.value = false
        positionFlow.value = 0L
        durationFlow.value = 0L
        try { exoPlayer.stop(); exoPlayer.clearMediaItems() } catch (_: Exception) {}
        if (item.ehVideo) {
            try {
                exoPlayer.setMediaItem(ExoMediaItem.fromUri(item.uri))
                exoPlayer.prepare()
                exoPlayer.pause()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(muted, exoPlayer) { exoPlayer.volume = if (muted) 0f else 1f }
    LaunchedEffect(speed, exoPlayer) {
        try { exoPlayer.playbackParameters = PlaybackParameters(speed) } catch (_: Exception) {}
    }
    LaunchedEffect(pagerState.settledPage, pagerState.isScrollInProgress) {
        if (pagerState.isScrollInProgress && exoPlayer.isPlaying) {
            try { exoPlayer.pause() } catch (_: Exception) {}
        }
    }

    DisposableEffect(isPlaying, isCurrentVideo) {
        val w = activity?.window
        if (isPlaying && isCurrentVideo) {
            w?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            w?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                try { if (exoPlayer.isPlaying) exoPlayer.pause() } catch (_: Exception) {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun interact() {
        controlsVisible = true
        interactionId++
    }

    LaunchedEffect(controlsVisible, currentPage, interactionId, submenuOpen) {
        if (controlsVisible && !submenuOpen) {
            delay(3000)
            controlsVisible = false
        }
    }

    LaunchedEffect(Unit) {
        withTimeoutOrNull(600L) { while (!contentLoaded) delay(16) }
        reveal.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
        revealDone = true
    }

    DisposableEffect(Unit) {
        onDispose { VideoFramesCache.clearAll() }
    }

    DisposableEffect(landscapeMode) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        if (landscapeMode) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            controller?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun closeWithAnimation() {
        if (isClosing) return
        isClosing = true
        val currentIdx = currentPage
        val itemNow = items.getOrNull(currentIdx)
        exitItemIndex = currentIdx
        exitBounds = itemNow?.let { getCurrentBounds(it) }
        try { exoPlayer.stop(); exoPlayer.clearMediaItems() } catch (_: Exception) {}
        scope.launch {
            if (exitBounds != null) {
                reveal.animateTo(0f, tween(280, easing = FastOutSlowInEasing))
            } else {
                reveal.animateTo(0f, tween(220))
            }
            onClose(currentIdx)
        }
    }

    fun togglePlayPause() {
        try {
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
            } else {
                if (exoPlayer.duration > 0 &&
                    exoPlayer.currentPosition >= exoPlayer.duration - 500
                ) {
                    exoPlayer.seekTo(0)
                }
                exoPlayer.play()
            }
        } catch (_: Exception) {}
        interact()
    }

    BackHandler { closeWithAnimation() }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = alphaPager }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = revealDone && pagerScrollEnabled
            ) { page ->
                val item = items[page]
                val isCurrent = page == currentPage

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.ehVideo) {
                        if (isCurrent) {
                            // Video + pinch/pan/double-tap zoom (same as photo)
                            ZoomableBox(
                                contentKey = item.uri,
                                isCurrentPage = isCurrent,
                                onZoomStateChange = { zoomed ->
                                    if (isCurrent) pagerScrollEnabled = !zoomed
                                },
                                onSingleTap = {
                                    if (isCurrent) {
                                        if (controlsVisible) controlsVisible = false
                                        else interact()
                                    }
                                }
                            ) {
                                VideoPlayerCore(exoPlayer = exoPlayer, fitMode = fitMode)
                            }
                        } else {
                            VideoPlaceholder(uri = item.uri, durationMs = item.duracaoMs)
                        }
                    } else {
                        PhotoZoomable(
                            uri = item.uri,
                            isCurrentPage = isCurrent,
                            onZoomStateChange = { zoomed ->
                                if (isCurrent) pagerScrollEnabled = !zoomed
                            },
                            onSingleTap = {
                                if (isCurrent) {
                                    if (controlsVisible) controlsVisible = false
                                    else interact()
                                }
                            }
                        )
                    }
                }
            }
        }

        // Reveal overlay
        val boundsAnim = if (isClosing) exitBounds else getCurrentBounds(items[initialPage])
        val showingOverlay = boundsAnim != null && (!revealDone || isClosing)

        if (showingOverlay && boundsAnim != null) {
            val p = reveal.value
            val startX = boundsAnim.left
            val startY = boundsAnim.top
            val startW = boundsAnim.width
            val startH = boundsAnim.height
            val curX = startX + (0f - startX) * p
            val curY = startY + (0f - startY) * p
            val curW = startW + (screenWidthPx - startW) * p
            val curH = startH + (screenHeightPx - startH) * p
            val overlayItem = items.getOrNull(if (isClosing) exitItemIndex else initialPage)

            if (overlayItem != null) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(curX.roundToInt(), curY.roundToInt()) }
                        .size(
                            width = with(density) { curW.toDp() },
                            height = with(density) { curH.toDp() }
                        )
                ) {
                    if (overlayItem.ehVideo) {
                        VideoThumbReveal(
                            uri = overlayItem.uri,
                            durationMs = overlayItem.duracaoMs,
                            onLoaded = { contentLoaded = true }
                        )
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(overlayItem.uri)
                                .size(300)
                                .precision(Precision.INEXACT)
                                .memoryCacheKey(overlayItem.uri.toString())
                                .build(),
                            onState = { state ->
                                if (state is coil.compose.AsyncImagePainter.State.Success) {
                                    contentLoaded = true
                                }
                            },
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Center video controls (play/pause overlay)
        if (revealDone && !isClosing && isCurrentVideo && controlsVisible) {
            Box(modifier = Modifier.align(Alignment.Center)) {
                VideoCenterControls(
                    isPlaying = isPlaying,
                    isPrepared = isPrepared,
                    onPlayPause = { togglePlayPause() },
                    onReplay30 = {
                        try {
                            val pos = (exoPlayer.currentPosition - 30000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(pos)
                        } catch (_: Exception) {}
                        interact()
                    },
                    onReplay10 = {
                        try {
                            val pos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(pos)
                        } catch (_: Exception) {}
                        interact()
                    },
                    onForward10 = {
                        try {
                            val dur = exoPlayer.duration.coerceAtLeast(0L)
                            val pos = (exoPlayer.currentPosition + 10000L).coerceAtMost(dur)
                            exoPlayer.seekTo(pos)
                        } catch (_: Exception) {}
                        interact()
                    },
                    onForward30 = {
                        try {
                            val dur = exoPlayer.duration.coerceAtLeast(0L)
                            val pos = (exoPlayer.currentPosition + 30000L).coerceAtMost(dur)
                            exoPlayer.seekTo(pos)
                        } catch (_: Exception) {}
                        interact()
                    }
                )
            }
        }

        // Scrubbing time overlay
        if (isScrubbing && isCurrentVideo && durationMs > 0L) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        formatTime(positionMs),
                        color = VzOnBackground,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "de ${formatTime(durationMs)}",
                        color = VzSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        if (revealDone && !isClosing) {
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(tween(180)) + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut(tween(180)) + slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                ViewerTopBar(
                    item = currentItem,
                    landscapeMode = landscapeMode,
                    onBack = { closeWithAnimation() },
                    onToggleLandscape = {
                        landscapeMode = !landscapeMode
                        interact()
                    }
                )
            }

            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(tween(180)) + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut(tween(180)) + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                ViewerBottomBar(
                    item = currentItem,
                    isVideo = isCurrentVideo,
                    durationMs = durationMs,
                    positionMs = positionMs,
                    isPlaying = isPlaying,
                    trashMode = trashMode,
                    hiddenMode = hiddenMode,
                    isFavorite = isFavorite,
                    muted = muted,
                    speed = speed,
                    fitMode = fitMode,
                    submenuOpen = submenuOpen,
                    onSubmenuChange = { submenuOpen = it },
                    onSeek = { ms ->
                        positionFlow.value = ms
                        try { exoPlayer.seekTo(ms) } catch (_: Exception) {}
                        interact()
                    },
                    onSeekStart = {
                        isScrubbing = true
                        wasPlayingBeforeScrub = exoPlayer.isPlaying
                        try { exoPlayer.pause() } catch (_: Exception) {}
                        interact()
                    },
                    onSeekEnd = {
                        if (wasPlayingBeforeScrub) {
                            try { exoPlayer.play() } catch (_: Exception) {}
                        }
                        scope.launch {
                            delay(500)
                            isScrubbing = false
                        }
                        interact()
                    },
                    onMuteToggle = { muted = !muted; interact() },
                    onSpeedChange = { v -> speed = v; interact() },
                    onFitModeChange = { m -> fitMode = m; interact() },
                    onShare = { interact(); currentItem?.let { onShare(it) } },
                    onHideOrRestore = {
                        interact()
                        if (hiddenMode) currentItem?.let { onHide(it) }
                        else showHideDialog = true
                    },
                    onDelete = { interact(); showDeleteDialog = true },
                    onFavoriteToggle = {
                        interact()
                        currentItem?.let { item ->
                            val wasFav = isFavorite
                            scope.launch {
                                prefs.toggleFavorite(item.uri.toString())
                                SnackbarBus.show(
                                    if (wasFav) "Removido dos favoritos"
                                    else "Adicionado aos favoritos"
                                )
                            }
                        }
                    },
                    onEdit = {
                        interact()
                        currentItem?.let { item ->
                            val type = if (item.ehVideo) "video/*" else "image/*"
                            val intent = Intent(Intent.ACTION_EDIT).apply {
                                setDataAndType(item.uri, type)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                            }
                            try { context.startActivity(intent) }
                            catch (_: Exception) {
                                val fallback = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(item.uri, type)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                try { context.startActivity(fallback) } catch (_: Exception) {}
                            }
                        }
                    },
                    onRestore = onRestore?.let { restore ->
                        { interact(); currentItem?.let { restore(it) } }
                    },
                    onDeletePermanently = onDeletePermanently?.let { perm ->
                        {
                            showDeleteDialog = false
                            currentItem?.let { perm(it) }
                        }
                    }
                )
            }
        }
    }

    // Dialogs (mesmos de antes, sem mudança)
    if (showDeleteDialog && currentItem != null && trashMode) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = VzSurface,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Excluir permanentemente?", style = MaterialTheme.typography.titleLarge, color = VzOnBackground) },
            text = { Text("O item será apagado de vez. Não dá pra recuperar.", style = MaterialTheme.typography.bodyMedium, color = VzOnSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete(currentItem) }) {
                    Text("Excluir", color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar", color = VzOnSurfaceVariant)
                }
            }
        )
    }

    if (showDeleteDialog && currentItem != null && !trashMode) {
        if (onDeletePermanently != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = VzSurface,
                shape = RoundedCornerShape(20.dp),
                title = { Text("Excluir este item?", style = MaterialTheme.typography.titleLarge, color = VzOnBackground) },
                text = {
                    Column {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Outlined.RestoreFromTrash, null, tint = VzSecondary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Lixeira", color = VzSecondary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "O item sai do celular agora, mas fica guardado dentro do VzGallery pelas próximas 24 horas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VzOnSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Outlined.DeleteForever, null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Excluir permanentemente", color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("O item é apagado de vez. Não dá pra recuperar.", style = MaterialTheme.typography.bodyMedium, color = VzOnSurfaceVariant, fontSize = 12.sp)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDeleteDialog = false; onDeletePermanently(currentItem) }) {
                        Text("Excluir", color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar", color = VzOnSurfaceVariant) }
                        TextButton(onClick = { showDeleteDialog = false; onDelete(currentItem) }) {
                            Text("Lixeira", color = VzSecondary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = VzSurface,
                shape = RoundedCornerShape(20.dp),
                title = { Text("Mover pra lixeira?", style = MaterialTheme.typography.titleLarge, color = VzOnBackground) },
                text = { Text("O item vai pra lixeira do app e pode ser restaurado por 24 horas.", style = MaterialTheme.typography.bodyMedium, color = VzOnSurfaceVariant) },
                confirmButton = {
                    TextButton(onClick = { showDeleteDialog = false; onDelete(currentItem) }) {
                        Text("Excluir", color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancelar", color = VzOnSurfaceVariant)
                    }
                }
            )
        }
    }

    if (showHideDialog && currentItem != null && !hiddenMode && !trashMode) {
        AlertDialog(
            onDismissRequest = { showHideDialog = false },
            containerColor = VzSurface,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Ocultar este item?", style = MaterialTheme.typography.titleLarge, color = VzOnBackground) },
            text = { Text("O item NÃO será excluído. Ele apenas ficará oculto da lista principal.", style = MaterialTheme.typography.bodyMedium, color = VzOnSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = { showHideDialog = false; onHide(currentItem) }) {
                    Text("Ocultar", color = VzSecondary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showHideDialog = false }) {
                    Text("Cancelar", color = VzOnSurfaceVariant)
                }
            }
        )
    }
}

@Composable
private fun VideoPlaceholder(uri: Uri, durationMs: Long?) {
    val context = LocalContext.current
    val frameMs = frameMillisFor(durationMs)
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(uri)
            .videoFrameMillis(frameMs)
            .size(800)
            .precision(Precision.INEXACT)
            .memoryCacheKey("$uri@$frameMs")
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun VideoThumbReveal(uri: Uri, durationMs: Long?, onLoaded: () -> Unit) {
    val context = LocalContext.current
    val frameMs = frameMillisFor(durationMs)
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(uri)
            .videoFrameMillis(frameMs)
            .size(800)
            .precision(Precision.INEXACT)
            .memoryCacheKey("$uri@$frameMs")
            .build(),
        onState = { state ->
            if (state is coil.compose.AsyncImagePainter.State.Success) onLoaded()
        },
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize()
    )
}