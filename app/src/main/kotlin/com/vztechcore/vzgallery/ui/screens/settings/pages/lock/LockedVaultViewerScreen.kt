package com.vztechcore.vzgallery.ui.screens.settings.pages.lock

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.vztechcore.vzgallery.data.LockRepository
import com.vztechcore.vzgallery.ui.components.media.FitMode
import com.vztechcore.vzgallery.ui.components.media.PhotoZoomable
import com.vztechcore.vzgallery.ui.components.media.VideoCenterControls
import com.vztechcore.vzgallery.ui.components.media.VideoPlayerCore
import com.vztechcore.vzgallery.ui.components.media.ZoomableBox
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzOnSurfaceVariant
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import com.vztechcore.vzgallery.ui.theme.VzSurface

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LockedVaultViewerScreen(
    items: List<LockRepository.LockedFile>,
    decryptedUris: Map<String, Uri>,
    startIndex: Int,
    onClose: (Int) -> Unit,
    onRestore: (LockRepository.LockedFile) -> Unit,
    onDelete: (LockRepository.LockedFile) -> Unit,
    onShare: (Uri, Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(initialPage = startIndex) { items.size }
    val currentPage = pagerState.currentPage
    val currentItem = items.getOrNull(currentPage)
    val currentUri = currentItem?.let { decryptedUris[it.id] }
    val isVideo = currentItem?.isVideo == true

    var showDeleteDialog by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var isPrepared by remember { mutableStateOf(false) }

    // Disables pager swipe while zoomed on the current item
    var pagerScrollEnabled by remember { mutableStateOf(true) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                isPrepared = state == Player.STATE_READY
                if (state == Player.STATE_ENDED) {
                    try {
                        exoPlayer.pause()
                        exoPlayer.seekTo(0L)
                        isPlaying = false
                    } catch (_: Exception) {}
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            try { exoPlayer.release() } catch (_: Exception) {}
        }
    }

    LaunchedEffect(currentPage, currentItem?.id) {
        isPrepared = false
        isPlaying = false
        pagerScrollEnabled = true
        try {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        } catch (_: Exception) {}

        val uri = currentItem?.let { decryptedUris[it.id] }
        if (uri != null && currentItem.isVideo) {
            try {
                exoPlayer.setMediaItem(ExoMediaItem.fromUri(uri))
                exoPlayer.prepare()
                exoPlayer.pause()
            } catch (_: Exception) {}
        }
    }

    BackHandler { onClose(currentPage) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = pagerScrollEnabled
        ) { page ->
            val item = items[page]
            val uri = decryptedUris[item.id]
            val isCurrent = page == currentPage

            if (uri == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = VzSecondary)
                }
            } else if (item.isVideo) {
                if (isCurrent) {
                    // Video + pinch/pan/double-tap zoom (same as photo)
                    ZoomableBox(
                        contentKey = item.id,
                        isCurrentPage = isCurrent,
                        onZoomStateChange = { zoomed ->
                            if (isCurrent) pagerScrollEnabled = !zoomed
                        }
                    ) {
                        VideoPlayerCore(exoPlayer = exoPlayer, fitMode = FitMode.ORIGINAL)
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize())
                }
            } else {
                PhotoZoomable(
                    uri = uri,
                    isCurrentPage = isCurrent,
                    onZoomStateChange = { zoomed ->
                        if (isCurrent) pagerScrollEnabled = !zoomed
                    }
                )
            }
        }

        // Center video controls (play/pause + skip)
        if (isVideo && currentUri != null) {
            Box(modifier = Modifier.align(Alignment.Center)) {
                VideoCenterControls(
                    isPlaying = isPlaying,
                    isPrepared = isPrepared,
                    onPlayPause = {
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
                    },
                    onReplay30 = {
                        try {
                            val pos = (exoPlayer.currentPosition - 30000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(pos)
                        } catch (_: Exception) {}
                    },
                    onReplay10 = {
                        try {
                            val pos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(pos)
                        } catch (_: Exception) {}
                    },
                    onForward10 = {
                        try {
                            val dur = exoPlayer.duration.coerceAtLeast(0L)
                            val pos = (exoPlayer.currentPosition + 10000L).coerceAtMost(dur)
                            exoPlayer.seekTo(pos)
                        } catch (_: Exception) {}
                    },
                    onForward30 = {
                        try {
                            val dur = exoPlayer.duration.coerceAtLeast(0L)
                            val pos = (exoPlayer.currentPosition + 30000L).coerceAtMost(dur)
                            exoPlayer.seekTo(pos)
                        } catch (_: Exception) {}
                    }
                )
            }
        }

        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.55f))
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onClose(currentPage) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = VzOnBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = currentItem?.originalName ?: "",
                    color = VzOnBackground,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
        }

        // Bottom actions
        if (currentItem != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 12.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VaultAction(
                        icon = Icons.Outlined.Share,
                        title = "Compartilhar",
                        color = VzOnBackground,
                        onClick = {
                            currentUri?.let { onShare(it, currentItem.isVideo) }
                        }
                    )
                    VaultAction(
                        icon = Icons.Filled.Restore,
                        title = "Restaurar",
                        color = VzSecondary,
                        onClick = { onRestore(currentItem) }
                    )
                    VaultAction(
                        icon = Icons.Filled.DeleteForever,
                        title = "Excluir",
                        color = Color(0xFFEF4444),
                        onClick = { showDeleteDialog = true }
                    )
                }
            }
        }
    }

    if (showDeleteDialog && currentItem != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = VzSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Excluir permanentemente?",
                    style = MaterialTheme.typography.titleLarge,
                    color = VzOnBackground
                )
            },
            text = {
                Text(
                    "O arquivo será apagado de vez do cofre. Não dá pra recuperar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VzOnSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete(currentItem)
                }) {
                    Text(
                        "Excluir",
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.SemiBold
                    )
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

@Composable
private fun VaultAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, title, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(title, color = color, fontSize = 11.sp)
    }
}