package com.vztechcore.vzgallery.ui.screens.wrapped

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import coil.size.Precision
import com.vztechcore.vzgallery.data.MediaScanner
import com.vztechcore.vzgallery.data.Prefs
import com.vztechcore.vzgallery.model.MediaItem as VzMediaItem
import com.vztechcore.vzgallery.ui.theme.VzBackground
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

private const val TOTAL_SLIDES = 9
private val THUMB_SIZE = 100.dp
private val THUMB_GAP = 6.dp

/**
 * Coil frame millis: pega um frame em ~25% do vídeo (ou 1s) pra evitar
 * frames pretos do começo.
 */
private fun frameMillisFor(durationMs: Long?): Long {
    return if ((durationMs ?: 0L) > 3000L) {
        (durationMs!! / 4L).coerceAtLeast(500L)
    } else {
        1000L
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WrappedScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scanner = remember { MediaScanner(context) }
    val prefs = remember { Prefs(context) }

    val mostrarSvg by prefs.showSvg.collectAsState(initial = false)
    val favoritos by prefs.favorites.collectAsState(initial = emptySet())

    var allItems by remember { mutableStateOf<List<VzMediaItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        allItems = withContext(Dispatchers.IO) { scanner.loadMedia() }
        loading = false
    }

    val items = remember(allItems, mostrarSvg) {
        if (mostrarSvg) allItems
        else allItems.filterNot {
            it.nome.substringAfterLast('.', "").lowercase() in setOf("svg", "svgz")
        }
    }

    val pagerState = rememberPagerState { TOTAL_SLIDES }

    BackHandler { onBack() }

    if (loading) {
        Box(
            modifier = Modifier.fillMaxSize().background(VzBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = VzSecondary)
        }
        return
    }

    if (items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().background(VzBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Sem itens pra mostrar ainda",
                color = Color.White,
                fontSize = 16.sp
            )
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true
        ) { page ->
            when (page) {
                0 -> SlideIntro(items)
                1 -> SlideCount(items)
                2 -> SlideOldestPhoto(items)
                3 -> SlideOldestVideo(items)
                4 -> SlideTopMonth(items)
                5 -> SlideTopFolder(items)
                6 -> SlideFavorites(favoritos = favoritos, items = items)
                7 -> SlideSize(items)
                else -> SlideFinal()
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(TOTAL_SLIDES) { idx ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(
                            if (idx <= pagerState.currentPage) Color.White
                            else Color.White.copy(alpha = 0.3f)
                        )
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 8.dp, top = 26.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Voltar",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun MarqueeStrip(
    items: List<VzMediaItem>,
    modifier: Modifier = Modifier,
    speedPxPerSecond: Float = 60f,
    thumbSize: Dp = THUMB_SIZE,
    reverse: Boolean = false
) {
    if (items.isEmpty()) {
        Box(
            modifier = modifier.background(Color.Black.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "—",
                color = Color.White.copy(alpha = 0.2f),
                fontSize = 12.sp
            )
        }
        return
    }

    val virtualCount = Int.MAX_VALUE
    val startIndex = virtualCount / 2

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)

    LaunchedEffect(items, reverse, speedPxPerSecond) {
        var lastFrame = withFrameNanos { it }
        val direction = if (reverse) -1f else 1f
        while (true) {
            val now = withFrameNanos { it }
            val dtSec = (now - lastFrame) / 1_000_000_000f
            val delta = speedPxPerSecond * direction * dtSec
            listState.scrollBy(delta)
            lastFrame = now
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(THUMB_GAP),
        userScrollEnabled = false
    ) {
        items(
            count = virtualCount,
            key = { it }
        ) { index ->
            val item = items[index % items.size]
            StripThumb(item = item, size = thumbSize)
        }
    }
}

@Composable
private fun StripThumb(item: VzMediaItem, size: Dp = THUMB_SIZE) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.4f))
    ) {
        if (item.isVideo) {
            val frameMs = frameMillisFor(item.duracaoMs)
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.uri)
                    .videoFrameMillis(frameMs)
                    .size(300)
                    .precision(Precision.INEXACT)
                    .memoryCacheKey("${item.uri}@$frameMs")
                    .crossfade(200)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.uri)
                    .size(300)
                    .precision(Precision.INEXACT)
                    .crossfade(200)
                    .memoryCacheKey(item.uri.toString())
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun randomSlice(items: List<VzMediaItem>, seed: Int): List<VzMediaItem> {
    if (items.isEmpty()) return emptyList()
    return items.shuffled(Random(seed.toLong()))
}

@Composable
private fun PhotoMosaicBackground(
    items: List<VzMediaItem>,
    modifier: Modifier = Modifier,
    blurRadius: Int = 25,
    darken: Float = 0.55f
) {
    if (items.isEmpty()) {
        Box(modifier.background(Color.Black))
        return
    }

    val filled = remember(items) {
        val base = items.shuffled()
        val result = mutableListOf<VzMediaItem>()
        var i = 0
        while (result.size < 36) {
            result.add(base[i % base.size])
            i++
        }
        result
    }

    Box(modifier.clipToBounds()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        Modifier.blur(blurRadius.dp)
                    else Modifier
                )
        ) {
            for (row in 0 until 6) {
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    for (col in 0 until 6) {
                        val item = filled[row * 6 + col]
                        StripThumbInternal(item = item)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = darken))
        )
    }
}

@Composable
private fun RowScope.StripThumbInternal(item: VzMediaItem) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
    ) {
        if (item.isVideo) {
            val frameMs = frameMillisFor(item.duracaoMs)
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.uri)
                    .videoFrameMillis(frameMs)
                    .size(200)
                    .precision(Precision.INEXACT)
                    .memoryCacheKey("${item.uri}@$frameMs")
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.uri)
                    .size(200)
                    .precision(Precision.INEXACT)
                    .memoryCacheKey(item.uri.toString())
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun CircleStatBadge(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 48.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun SlideIntro(items: List<VzMediaItem>) {
    Box(Modifier.fillMaxSize()) {
        PhotoMosaicBackground(
            items = items,
            modifier = Modifier.fillMaxSize(),
            blurRadius = 30,
            darken = 0.6f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "VzWrapped",
                color = Color.White,
                fontSize = 54.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Uma retrospectiva da sua galeria",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Deslize pra avançar →",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "por Vtzin x · VzTechCore",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

@Composable
private fun SlideCount(items: List<VzMediaItem>) {
    val fotos = remember(items) { items.filter { !it.isVideo } }
    val videos = remember(items) { items.filter { it.isVideo } }

    val fotos1 = remember(fotos) { randomSlice(fotos, 1) }
    val videos1 = remember(videos) { randomSlice(videos, 2) }
    val fotos2 = remember(fotos) { randomSlice(fotos, 3) }
    val videos2 = remember(videos) { randomSlice(videos, 4) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 60.dp)
        ) {
            MarqueeStrip(
                items = fotos1,
                modifier = Modifier.fillMaxWidth().height(THUMB_SIZE),
                speedPxPerSecond = 55f
            )
            Spacer(Modifier.height(6.dp))
            MarqueeStrip(
                items = videos1,
                modifier = Modifier.fillMaxWidth().height(THUMB_SIZE),
                speedPxPerSecond = 70f,
                reverse = true
            )

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Você guardou",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircleStatBadge(
                        value = fotos.size.toString(),
                        label = if (fotos.size == 1) "foto" else "fotos"
                    )
                    CircleStatBadge(
                        value = videos.size.toString(),
                        label = if (videos.size == 1) "vídeo" else "vídeos"
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            MarqueeStrip(
                items = fotos2,
                modifier = Modifier.fillMaxWidth().height(THUMB_SIZE),
                speedPxPerSecond = 65f
            )
            Spacer(Modifier.height(6.dp))
            MarqueeStrip(
                items = videos2,
                modifier = Modifier.fillMaxWidth().height(THUMB_SIZE),
                speedPxPerSecond = 60f,
                reverse = true
            )
        }
    }
}

@Composable
private fun SlideOldestPhoto(items: List<VzMediaItem>) {
    val oldest = remember(items) {
        items.filter { !it.isVideo }.minByOrNull { it.dataAdicionada }
    }

    if (oldest == null) {
        Box(Modifier.fillMaxSize().background(Color.Black))
        return
    }

    Box(Modifier.fillMaxSize()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(oldest.uri)
                .size(1200)
                .precision(Precision.INEXACT)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Sua memória mais antiga",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(oldest.uri)
                        .size(800)
                        .precision(Precision.INEXACT)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = formatDate(oldest.dataAdicionada),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = oldest.nome,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun SlideOldestVideo(items: List<VzMediaItem>) {
    val oldest = remember(items) {
        items.filter { it.isVideo }.minByOrNull { it.dataAdicionada }
    }

    if (oldest == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Você ainda não tem vídeos",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp
            )
        }
        return
    }

    val context = LocalContext.current
    val frameMs = frameMillisFor(oldest.duracaoMs)

    Box(Modifier.fillMaxSize()) {
        // Background thumbnail (25% frame)
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(oldest.uri)
                .videoFrameMillis(frameMs)
                .size(1200)
                .precision(Precision.INEXACT)
                .memoryCacheKey("${oldest.uri}@$frameMs")
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Seu vídeo mais antigo",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxHeight(0.6f)
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black)
            ) {
                MutedVideoPlayer(uri = oldest.uri)
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = formatDate(oldest.dataAdicionada),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = oldest.nome,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun MutedVideoPlayer(uri: android.net.Uri) {
    val context = LocalContext.current

    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try { exoPlayer.release() } catch (_: Exception) {}
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                setShutterBackgroundColor(android.graphics.Color.BLACK)
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun SlideTopMonth(items: List<VzMediaItem>) {
    val (mesLabel, mesItems) = remember(items) {
        val grupos = items.groupBy { item ->
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = item.dataAdicionada * 1000
            "${cal.get(java.util.Calendar.YEAR)}-${cal.get(java.util.Calendar.MONTH)}"
        }
        val melhor = grupos.maxByOrNull { it.value.size }
        if (melhor == null) {
            "—" to emptyList<VzMediaItem>()
        } else {
            val parts = melhor.key.split("-")
            val ano = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val mes = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val cal = java.util.Calendar.getInstance()
            cal.set(ano, mes, 1)
            val fmt = SimpleDateFormat("MMMM 'de' yyyy", Locale("pt", "BR"))
            val label = fmt.format(cal.time).replaceFirstChar { it.uppercase() }
            label to melhor.value
        }
    }

    val fotos = remember(mesItems) { mesItems.filter { !it.isVideo } }
    val videos = remember(mesItems) { mesItems.filter { it.isVideo } }

    val fotos1 = remember(fotos) { randomSlice(fotos, 11) }
    val videos1 = remember(videos) { randomSlice(videos, 12) }
    val fotos2 = remember(fotos) { randomSlice(fotos, 13) }
    val videos2 = remember(videos) { randomSlice(videos, 14) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 60.dp)
        ) {
            MarqueeStrip(
                items = fotos1,
                modifier = Modifier.fillMaxWidth().height(THUMB_SIZE),
                speedPxPerSecond = 55f
            )
            Spacer(Modifier.height(6.dp))
            MarqueeStrip(
                items = videos1,
                modifier = Modifier.fillMaxWidth().height(THUMB_SIZE),
                speedPxPerSecond = 70f,
                reverse = true
            )

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Seu mês mais produtivo",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = mesLabel,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircleStatBadge(
                        value = fotos.size.toString(),
                        label = if (fotos.size == 1) "foto" else "fotos"
                    )
                    CircleStatBadge(
                        value = videos.size.toString(),
                        label = if (videos.size == 1) "vídeo" else "vídeos"
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            MarqueeStrip(
                items = fotos2,
                modifier = Modifier.fillMaxWidth().height(THUMB_SIZE),
                speedPxPerSecond = 65f
            )
            Spacer(Modifier.height(6.dp))
            MarqueeStrip(
                items = videos2,
                modifier = Modifier.fillMaxWidth().height(THUMB_SIZE),
                speedPxPerSecond = 60f,
                reverse = true
            )
        }
    }
}

@Composable
private fun SlideTopFolder(items: List<VzMediaItem>) {
    val (pasta, pastaItems) = remember(items) {
        val grupos = items.groupBy { it.relativePath.ifBlank { "/" } }
        val melhor = grupos.maxByOrNull { it.value.size }
        if (melhor == null) {
            "—" to emptyList<VzMediaItem>()
        } else {
            val semBarra = melhor.key.trimEnd('/')
            val nome = semBarra.substringAfterLast('/').ifBlank { semBarra.ifBlank { "Raiz" } }
            nome to melhor.value
        }
    }

    val amostras = remember(pastaItems) {
        pastaItems.shuffled().take(4)
    }

    Box(Modifier.fillMaxSize()) {
        PhotoMosaicBackground(
            items = pastaItems,
            modifier = Modifier.fillMaxSize(),
            blurRadius = 25,
            darken = 0.6f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Sua pasta favorita",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = pasta,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        amostras.getOrNull(0)?.let { CellImage(it, Modifier.weight(1f).fillMaxHeight()) }
                            ?: Box(Modifier.weight(1f).fillMaxHeight().background(Color.Black))
                        amostras.getOrNull(1)?.let { CellImage(it, Modifier.weight(1f).fillMaxHeight()) }
                            ?: Box(Modifier.weight(1f).fillMaxHeight().background(Color.Black))
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        amostras.getOrNull(2)?.let { CellImage(it, Modifier.weight(1f).fillMaxHeight()) }
                            ?: Box(Modifier.weight(1f).fillMaxHeight().background(Color.Black))
                        amostras.getOrNull(3)?.let { CellImage(it, Modifier.weight(1f).fillMaxHeight()) }
                            ?: Box(Modifier.weight(1f).fillMaxHeight().background(Color.Black))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = "${pastaItems.size} ${if (pastaItems.size == 1) "item" else "itens"} guardados",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun CellImage(item: VzMediaItem, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
    ) {
        if (item.isVideo) {
            val frameMs = frameMillisFor(item.duracaoMs)
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.uri)
                    .videoFrameMillis(frameMs)
                    .size(400)
                    .precision(Precision.INEXACT)
                    .memoryCacheKey("${item.uri}@$frameMs")
                    .crossfade(200)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.uri)
                    .size(400)
                    .precision(Precision.INEXACT)
                    .crossfade(200)
                    .memoryCacheKey(item.uri.toString())
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun SlideFavorites(
    favoritos: Set<String>,
    items: List<VzMediaItem>
) {
    val favItems = remember(favoritos, items) {
        items.filter { it.uri.toString() in favoritos }
    }

    when {
        favItems.isEmpty() -> SlideFavoritesEmpty()
        favItems.size in 1..3 -> SlideFavorites1to3(favItems)
        favItems.size in 4..9 -> SlideFavorites4to9(favItems)
        favItems.size in 10..39 -> SlideFavorites10to39(favItems)
        else -> SlideFavorites40plus(favItems)
    }
}

@Composable
private fun SlideFavoritesEmpty() {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "E você favoritou",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 18.sp
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "0",
                color = Color.White,
                fontSize = 120.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Nem começou ainda",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun SlideFavorites1to3(favItems: List<VzMediaItem>) {
    val destaque = favItems.first()

    Box(Modifier.fillMaxSize()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(destaque.uri)
                .size(1200)
                .precision(Precision.INEXACT)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Você favoritou",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp
            )
            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black)
            ) {
                StripThumb(item = destaque, size = 220.dp)
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "${favItems.size} ${if (favItems.size == 1) "item" else "itens"}",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SlideFavorites4to9(favItems: List<VzMediaItem>) {
    val amostras = remember(favItems) { favItems.shuffled().take(4) }

    Box(Modifier.fillMaxSize()) {
        PhotoMosaicBackground(
            items = favItems,
            modifier = Modifier.fillMaxSize(),
            blurRadius = 25,
            darken = 0.6f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Você favoritou",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp
            )
            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        amostras.getOrNull(0)?.let { CellImage(it, Modifier.weight(1f).fillMaxHeight()) }
                            ?: Box(Modifier.weight(1f).fillMaxHeight().background(Color.Black))
                        amostras.getOrNull(1)?.let { CellImage(it, Modifier.weight(1f).fillMaxHeight()) }
                            ?: Box(Modifier.weight(1f).fillMaxHeight().background(Color.Black))
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        amostras.getOrNull(2)?.let { CellImage(it, Modifier.weight(1f).fillMaxHeight()) }
                            ?: Box(Modifier.weight(1f).fillMaxHeight().background(Color.Black))
                        amostras.getOrNull(3)?.let { CellImage(it, Modifier.weight(1f).fillMaxHeight()) }
                            ?: Box(Modifier.weight(1f).fillMaxHeight().background(Color.Black))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = "${favItems.size} ${if (favItems.size == 1) "item" else "itens"}",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SlideFavorites10to39(favItems: List<VzMediaItem>) {
    val strip = remember(favItems) { randomSlice(favItems, 21) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Você favoritou",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = favItems.size.toString(),
                    color = Color.White,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 72.sp
                )
                Text(
                    text = if (favItems.size == 1) "item" else "itens",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 18.sp
                )
            }

            Spacer(Modifier.height(28.dp))

            MarqueeStrip(
                items = strip,
                modifier = Modifier.fillMaxWidth().height(THUMB_SIZE * 1.4f),
                speedPxPerSecond = 55f,
                thumbSize = THUMB_SIZE * 1.4f
            )

            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun SlideFavorites40plus(favItems: List<VzMediaItem>) {
    val strip1 = remember(favItems) { randomSlice(favItems, 31) }
    val strip2 = remember(favItems) { randomSlice(favItems, 32) }
    val strip3 = remember(favItems) { randomSlice(favItems, 33) }
    val strip4 = remember(favItems) { randomSlice(favItems, 34) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 60.dp)
        ) {
            MarqueeStrip(
                items = strip1,
                modifier = Modifier.fillMaxWidth().height(THUMB_SIZE),
                speedPxPerSecond = 55f
            )
            Spacer(Modifier.height(6.dp))
            MarqueeStrip(
                items = strip2,
                modifier = Modifier.fillMaxWidth().height(THUMB_SIZE),
                speedPxPerSecond = 70f,
                reverse = true
            )

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Você favoritou",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = favItems.size.toString(),
                    color = Color.White,
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 60.sp
                )
                Text(
                    text = "itens",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.weight(1f))

            MarqueeStrip(
                items = strip3,
                modifier = Modifier.fillMaxWidth().height(THUMB_SIZE),
                speedPxPerSecond = 65f
            )
            Spacer(Modifier.height(6.dp))
            MarqueeStrip(
                items = strip4,
                modifier = Modifier.fillMaxWidth().height(THUMB_SIZE),
                speedPxPerSecond = 60f,
                reverse = true
            )
        }
    }
}

@Composable
private fun SlideSize(items: List<VzMediaItem>) {
    val totalBytes = remember(items) { items.sumOf { it.tamanhoBytes } }

    Box(Modifier.fillMaxSize()) {
        PhotoMosaicBackground(
            items = items,
            modifier = Modifier.fillMaxSize(),
            blurRadius = 35,
            darken = 0.75f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Tudo isso ocupa",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 16.sp
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = formatBytes(totalBytes),
                color = Color.White,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 64.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "no seu celular",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun SlideFinal() {
    val gradiente = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF000000),
            Color(0xFF0A1229),
            Color(0xFF1E40AF)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradiente)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(80.dp))

            Text(
                text = "Obrigado",
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "por usar o VzGallery",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))

            Text(
                text = "Durante meus projetos mobile, eu enfrentava um " +
                    "problema recorrente: fotos aleatórias apareciam na " +
                    "galeria do celular sem eu ter salvo nada.",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Foi daí que veio a ideia de construir um app de " +
                    "galeria próprio — com controle total sobre o que " +
                    "aparece e o que fica escondido.",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(40.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "DESENVOLVIDO POR",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Vtzin x",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "VzTechCore",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "v1.0 — MVP",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(30.dp))

            Text(
                text = "Deslize de volta pra rever\nou toque na seta pra sair",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            Spacer(Modifier.height(60.dp))
        }
    }
}

private fun formatDate(dataAdicionada: Long): String {
    val fmt = SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("pt", "BR"))
    return fmt.format(Date(dataAdicionada * 1000)).replaceFirstChar { it.uppercase() }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.0f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.2f GB".format(gb)
}