package com.vztechcore.vzgallery.ui.screens.viewer

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val DEBOUNCE_SEEK_MS = 30L

private fun formatarTempo(ms: Long): String {
    if (ms <= 0) return "00:00"
    val s = ms / 1000
    val m = s / 60
    val seg = s % 60
    return "%02d:%02d".format(m, seg)
}

@Composable
fun VideoScrubber(
    uri: Uri,
    duracaoMs: Long,
    posicaoMs: Long,
    onSeek: (Long) -> Unit,
    onSeekStart: () -> Unit,
    onSeekEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val numFrames = remember(duracaoMs) { VideoFramesCache.frameCountFor(duracaoMs) }

    var frames by remember(uri) {
        mutableStateOf(
            VideoFramesCache.peek(uri) ?: List(numFrames) { null }
        )
    }

    LaunchedEffect(uri, duracaoMs) {
        if (VideoFramesCache.peek(uri) == null) {
            frames = VideoFramesCache.get(context, uri, duracaoMs)
        }
    }

    var larguraPx by remember { mutableStateOf(0f) }
    var scrubbing by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf(0f) }
    var ultimoSeekMs by remember { mutableStateOf(0L) }

    val progressoDoPlayer = if (duracaoMs > 0) {
        (posicaoMs.toFloat() / duracaoMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val progresso = if (scrubbing) dragProgress else progressoDoPlayer

    val thumbScale by animateFloatAsState(
        targetValue = if (scrubbing) 1.4f else 1f,
        animationSpec = tween(120),
        label = "thumb_scale"
    )

    fun dispararSeek(p: Float) {
        val agora = System.currentTimeMillis()
        if (agora - ultimoSeekMs >= DEBOUNCE_SEEK_MS) {
            ultimoSeekMs = agora
            onSeek((p * duracaoMs).toLong())
        }
    }

    // Card de preview: largura fixa, posição X segue a bolinha
    val cardWidthDp = 120.dp
    val cardWidthPx = with(density) { cardWidthDp.toPx() }
    val cardLeftPx = (progresso * larguraPx - cardWidthPx / 2)
        .coerceIn(0f, (larguraPx - cardWidthPx).coerceAtLeast(0f))

    // Tempo atual durante scrub
    val tempoAtualMs = (progresso * duracaoMs).toLong()
    val frameIdx = (progresso * (frames.size - 1))
        .roundToInt()
        .coerceIn(0, (frames.size - 1).coerceAtLeast(0))
    val frameBmp = frames.getOrNull(frameIdx)

    Column(modifier = modifier.fillMaxWidth()) {

        Box(modifier = Modifier.fillMaxWidth()) {

            // ===== SLIDER =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .onSizeChanged { larguraPx = it.width.toFloat() }
                    .pointerInput(duracaoMs, larguraPx) {
                        detectTapGestures { offset ->
                            if (larguraPx > 0f && duracaoMs > 0) {
                                val p = (offset.x / larguraPx).coerceIn(0f, 1f)
                                dragProgress = p
                                scrubbing = true
                                onSeekStart()
                                onSeek((p * duracaoMs).toLong())
                                onSeekEnd()
                                scope.launch {
                                    delay(500)
                                    scrubbing = false
                                }
                            }
                        }
                    }
                    .pointerInput(duracaoMs, larguraPx) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                if (larguraPx > 0f && duracaoMs > 0) {
                                    scrubbing = true
                                    dragProgress = (startOffset.x / larguraPx).coerceIn(0f, 1f)
                                    onSeekStart()
                                    dispararSeek(dragProgress)
                                }
                            },
                            onDragEnd = {
                                onSeekEnd()
                                scope.launch {
                                    delay(500)
                                    scrubbing = false
                                }
                            },
                            onDragCancel = {
                                onSeekEnd()
                                scope.launch {
                                    delay(500)
                                    scrubbing = false
                                }
                            },
                            onDrag = { change, _ ->
                                if (larguraPx > 0f && duracaoMs > 0) {
                                    val p = (change.position.x / larguraPx).coerceIn(0f, 1f)
                                    dragProgress = p
                                    dispararSeek(p)
                                    change.consume()
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.20f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progresso)
                            .clip(RoundedCornerShape(50))
                            .background(Brush.horizontalGradient(listOf(VzSecondary, VzSecondary)))
                    )
                }

                if (larguraPx > 0f) {
                    val thumbSizePx = with(density) { 16.dp.toPx() }
                    val thumbOffsetPx = (progresso * larguraPx - thumbSizePx / 2)
                        .coerceIn(0f, larguraPx - thumbSizePx)

                    Box(
                        modifier = Modifier
                            .offset(x = with(density) { thumbOffsetPx.toDp() })
                            .size(with(density) { thumbSizePx.toDp() })
                            .graphicsLayer {
                                scaleX = thumbScale
                                scaleY = thumbScale
                            }
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }

            // ===== CARD DE PREVIEW (flutua acima do slider) =====
            if (scrubbing && duracaoMs > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = with(density) { cardLeftPx.toDp() })
                        .graphicsLayer {
                            translationY = -cardWidthPx
                        }
                        .width(cardWidthDp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.92f))
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .background(Color.Black)
                        ) {
                            frameBmp?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = formatarTempo(tempoAtualMs),
                                color = VzOnBackground,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // ===== TEMPO: XX:XX • YY:YY =====
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${formatarTempo(tempoAtualMs)} • ${formatarTempo(duracaoMs)}",
            color = VzOnBackground.copy(alpha = 0.85f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        )
    }
}