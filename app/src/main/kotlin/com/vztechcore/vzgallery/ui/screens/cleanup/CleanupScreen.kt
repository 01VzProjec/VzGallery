package com.vztechcore.vzgallery.ui.screens.cleanup

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import coil.size.Precision
import com.vztechcore.vzgallery.data.MediaScanner
import com.vztechcore.vzgallery.data.StorageAnalyzer
import com.vztechcore.vzgallery.model.MediaItem
import com.vztechcore.vzgallery.ui.theme.VzBackground
import com.vztechcore.vzgallery.ui.theme.VzError
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzOnSurfaceVariant
import com.vztechcore.vzgallery.ui.theme.VzPrimary
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import com.vztechcore.vzgallery.ui.theme.VzSurface
import com.vztechcore.vzgallery.ui.theme.VzSurfaceVariant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class CleanupPhase { Analyzing, Results }

private enum class CleanupPage {
    Main,
    ScreenshotDetail,
    PhotoDupDetail,
    VideoDupDetail,
    SvgDupDetail,
}

private fun frameMillisFor(durationMs: Long?): Long {
    return if ((durationMs ?: 0L) > 3000L) {
        (durationMs!! / 4L).coerceAtLeast(500L)
    } else {
        1000L
    }
}

@Composable
fun CleanupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scanner = remember { MediaScanner(context) }
    val scope = rememberCoroutineScope()

    var phase by remember { mutableStateOf(CleanupPhase.Analyzing) }
    var progress by remember { mutableStateOf(0f) }
    var currentItem by remember { mutableStateOf<MediaItem?>(null) }
    var result by remember { mutableStateOf<StorageAnalyzer.AnalysisResult?>(null) }

    var page by remember { mutableStateOf(CleanupPage.Main) }

    // Skip flag + phase-done flag for the bottom button
    var svgSkipped by remember { mutableStateOf(false) }
    var svgPhaseDone by remember { mutableStateOf(false) }

    var analysisJob by remember { mutableStateOf<Job?>(null) }

    fun stopAndExit() {
        onBack()
        analysisJob?.cancel()
    }

    suspend fun runAnalysis() {
        phase = CleanupPhase.Analyzing
        progress = 0f
        currentItem = null
        svgSkipped = false
        svgPhaseDone = false

        val all = withContext(Dispatchers.IO) { scanner.loadMedia() }

        delay(300)

        try {
            val res = StorageAnalyzer.analyze(
                context = context,
                items = all,
                shouldSkipSvg = { svgSkipped },
                onSvgPhaseEnd = { svgPhaseDone = true },
                onProgress = { processed, total, item ->
                    progress = (processed.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f)
                    currentItem = item
                }
            )
            progress = 1f
            delay(400)
            result = res
            phase = CleanupPhase.Results
        } catch (_: CancellationException) {
            // User stopped
        }
    }

    BackHandler {
        when (page) {
            CleanupPage.Main -> {
                if (phase == CleanupPhase.Analyzing) stopAndExit()
                else onBack()
            }
            else -> page = CleanupPage.Main
        }
    }

    LaunchedEffect(Unit) {
        analysisJob = scope.launch { runAnalysis() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val res = result
        when (page) {
            CleanupPage.ScreenshotDetail -> {
                if (res != null) {
                    LargestDetailScreen(
                        title = "Capturas de tela",
                        items = res.screenshots,
                        onBack = { page = CleanupPage.Main },
                        onChanged = {
                            analysisJob = scope.launch { runAnalysis() }
                        }
                    )
                }
            }
            CleanupPage.PhotoDupDetail -> {
                if (res != null) {
                    DuplicateDetailScreen(
                        title = "Fotos duplicadas",
                        groups = res.duplicatePhotos,
                        onBack = { page = CleanupPage.Main },
                        onChanged = {
                            analysisJob = scope.launch { runAnalysis() }
                        }
                    )
                }
            }
            CleanupPage.VideoDupDetail -> {
                if (res != null) {
                    DuplicateDetailScreen(
                        title = "Vídeos duplicados",
                        groups = res.duplicateVideos,
                        onBack = { page = CleanupPage.Main },
                        onChanged = {
                            analysisJob = scope.launch { runAnalysis() }
                        }
                    )
                }
            }
            CleanupPage.SvgDupDetail -> {
                if (res != null) {
                    DuplicateDetailScreen(
                        title = "SVGs duplicados",
                        groups = res.duplicateSvgs,
                        onBack = { page = CleanupPage.Main },
                        onChanged = {
                            analysisJob = scope.launch { runAnalysis() }
                        }
                    )
                }
            }
            CleanupPage.Main -> {
                when (phase) {
                    CleanupPhase.Analyzing -> AnalyzingView(
                        progress = progress,
                        currentItem = currentItem,
                        svgSkipped = svgSkipped,
                        svgPhaseDone = svgPhaseDone,
                        onBack = { stopAndExit() },
                        onSkipSvg = { svgSkipped = true },
                        onStop = { stopAndExit() }
                    )
                    CleanupPhase.Results -> {
                        if (res != null) {
                            ResultsView(
                                result = res,
                                onBack = onBack,
                                onOpenScreenshots = { page = CleanupPage.ScreenshotDetail },
                                onOpenPhotoDuplicates = { page = CleanupPage.PhotoDupDetail },
                                onOpenVideoDuplicates = { page = CleanupPage.VideoDupDetail },
                                onOpenSvgDuplicates = { page = CleanupPage.SvgDupDetail }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// PHASE 1 — ANALYZING
// ============================================================
@Composable
private fun AnalyzingView(
    progress: Float,
    currentItem: MediaItem?,
    svgSkipped: Boolean,
    svgPhaseDone: Boolean,
    onBack: () -> Unit,
    onSkipSvg: () -> Unit,
    onStop: () -> Unit
) {
    val accentColor = VzSecondary
    val bgColor = VzOnBackground
    val subColor = VzOnSurfaceVariant
    val context = LocalContext.current

    // ===== Button logic =====
    //   - While SVG phase is running and user hasn't skipped yet: "Ignorar SVGs"
    //   - Once SVG phase is done (or user skipped): "Parar Verificação"
    val showSkipButton = !svgSkipped && !svgPhaseDone
    val buttonLabel = if (showSkipButton) "Ignorar SVGs" else "Parar Verificação"
    val buttonColor = if (showSkipButton) VzSurfaceVariant else VzError
    val buttonContentColor = if (showSkipButton) VzOnBackground else Color.White
    val buttonAction = if (showSkipButton) onSkipSvg else onStop

    val infinite = rememberInfiniteTransition(label = "scan")
    val scanPos by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_pos"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Background blur
        if (currentItem != null) {
            val request = if (currentItem.isVideo) {
                val frameMs = frameMillisFor(currentItem.duracaoMs)
                ImageRequest.Builder(context)
                    .data(currentItem.uri)
                    .videoFrameMillis(frameMs)
                    .size(600)
                    .precision(Precision.INEXACT)
                    .memoryCacheKey("${currentItem.uri}@$frameMs")
                    .build()
            } else {
                ImageRequest.Builder(context)
                    .data(currentItem.uri)
                    .size(600)
                    .precision(Precision.INEXACT)
                    .memoryCacheKey(currentItem.uri.toString())
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(40.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
        )

        // Scan line
        Canvas(modifier = Modifier.fillMaxSize()) {
            val y = size.height * scanPos
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, accentColor.copy(alpha = 0.9f), Color.Transparent),
                    startX = 0f, endX = size.width
                ),
                topLeft = Offset(0f, y - 2f),
                size = Size(size.width, 3f)
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(accentColor.copy(alpha = 0.15f), Color.Transparent),
                    startY = y, endY = y + 80f
                ),
                topLeft = Offset(0f, y),
                size = Size(size.width, 80f)
            )
        }

        // Back button
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 8.dp, top = 8.dp)
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = bgColor)
        }

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(VzSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (currentItem != null) {
                    val request = if (currentItem.isVideo) {
                        val frameMs = frameMillisFor(currentItem.duracaoMs)
                        ImageRequest.Builder(context)
                            .data(currentItem.uri)
                            .videoFrameMillis(frameMs)
                            .size(400)
                            .precision(Precision.INEXACT)
                            .memoryCacheKey("${currentItem.uri}@$frameMs")
                            .build()
                    } else {
                        ImageRequest.Builder(context)
                            .data(currentItem.uri)
                            .size(400)
                            .precision(Precision.INEXACT)
                            .memoryCacheKey(currentItem.uri.toString())
                            .build()
                    }
                    AsyncImage(
                        model = request,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Outlined.PhotoLibrary, null,
                        tint = accentColor.copy(alpha = pulse),
                        modifier = Modifier.size(72.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
            Text(
                text = "Analisando sua galeria...",
                color = bgColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${(progress * 100).toInt()}%",
                color = accentColor, fontSize = 42.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(50))
                        .background(accentColor)
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = currentItem?.nome ?: "Preparando...",
                color = subColor, fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        // Bottom action button
        Button(
            onClick = buttonAction,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonColor,
                contentColor = buttonContentColor
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = buttonLabel,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}

// ============================================================
// PHASE 2 — RESULTS
// ============================================================
@Composable
private fun ResultsView(
    result: StorageAnalyzer.AnalysisResult,
    onBack: () -> Unit,
    onOpenScreenshots: () -> Unit,
    onOpenPhotoDuplicates: () -> Unit,
    onOpenVideoDuplicates: () -> Unit,
    onOpenSvgDuplicates: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VzBackground)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = VzOnBackground)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Liberar espaço",
                style = MaterialTheme.typography.displayLarge,
                color = VzOnBackground,
                fontSize = 28.sp
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DonutCard(
                    cantFreeBytes = result.cantFreeBytes,
                    canFreeBytes = result.canFreeBytes
                )
            }

            item {
                CleanupSection(
                    title = "Capturas de tela - Screenshots",
                    unitLabel = "fotos",
                    itemCount = result.screenshots.size,
                    bytesInUse = result.screenshotBytes,
                    items = result.screenshots,
                    onClick = onOpenScreenshots
                )
            }

            item {
                CleanupSection(
                    title = "Fotos duplicadas",
                    unitLabel = "fotos",
                    itemCount = result.duplicatePhotos.sumOf { it.items.size },
                    bytesInUse = result.duplicatePhotos.flatMap { it.items }.sumOf { it.tamanhoBytes },
                    items = result.duplicatePhotos.flatMap { it.items },
                    onClick = onOpenPhotoDuplicates
                )
            }

            item {
                CleanupSection(
                    title = "Vídeos duplicados",
                    unitLabel = "vídeos",
                    itemCount = result.duplicateVideos.sumOf { it.items.size },
                    bytesInUse = result.duplicateVideos.flatMap { it.items }.sumOf { it.tamanhoBytes },
                    items = result.duplicateVideos.flatMap { it.items },
                    onClick = onOpenVideoDuplicates
                )
            }

            item {
                CleanupSection(
                    title = "SVGs duplicados",
                    unitLabel = "svgs",
                    itemCount = result.duplicateSvgs.sumOf { it.items.size },
                    bytesInUse = result.duplicateSvgs.flatMap { it.items }.sumOf { it.tamanhoBytes },
                    items = result.duplicateSvgs.flatMap { it.items },
                    onClick = onOpenSvgDuplicates
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ============================================================
// DONUT CARD
// ============================================================
@Composable
private fun DonutCard(
    cantFreeBytes: Long,
    canFreeBytes: Long
) {
    val total = (cantFreeBytes + canFreeBytes).coerceAtLeast(1L)
    val cantFreePct = cantFreeBytes.toFloat() / total
    val canFreePct = canFreeBytes.toFloat() / total

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1400),
        label = "donut_progress"
    )

    val (valueStr, unitStr) = StorageAnalyzer.splitBytes(canFreeBytes)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(VzSurface)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StorageDonut(
                cantFreePct = cantFreePct,
                canFreePct = canFreePct,
                progress = animProgress,
                centerValue = valueStr,
                centerUnit = unitStr,
                size = 180.dp
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendPill(
                    color = VzPrimary,
                    label = "Utilizáveis",
                    value = StorageAnalyzer.formatBytes(cantFreeBytes)
                )
                LegendPill(
                    color = VzError,
                    label = "Liberáveis",
                    value = StorageAnalyzer.formatBytes(canFreeBytes)
                )
            }
        }
    }
}

@Composable
private fun StorageDonut(
    cantFreePct: Float,
    canFreePct: Float,
    progress: Float,
    centerValue: String,
    centerUnit: String,
    size: Dp
) {
    val usableColor = VzPrimary
    val freeableColor = VzError
    val textColor = VzOnBackground
    val subColor = VzOnSurfaceVariant

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sw = 24.dp.toPx()
            val radius = (this.size.minDimension - sw) / 2
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2, radius * 2)

            val target = 360f * progress.coerceIn(0f, 1f)

            val usableSweep = (cantFreePct * 360f).coerceAtMost(target)
            drawArc(
                color = usableColor,
                startAngle = -90f,
                sweepAngle = usableSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = sw, cap = StrokeCap.Butt)
            )

            val freeableStart = cantFreePct * 360f - 90f
            val freeableSweep = (canFreePct * 360f)
                .coerceAtMost((target - cantFreePct * 360f).coerceAtLeast(0f))
            if (freeableSweep > 0f) {
                drawArc(
                    color = freeableColor,
                    startAngle = freeableStart,
                    sweepAngle = freeableSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = sw, cap = StrokeCap.Butt)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = centerValue,
                    color = textColor,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 34.sp
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = centerUnit,
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Text(
                text = "podem ser liberados",
                color = subColor,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LegendPill(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                color = VzOnBackground,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                color = VzOnSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

// ============================================================
// SECTION
// ============================================================
@Composable
private fun CleanupSection(
    title: String,
    unitLabel: String,
    itemCount: Int,
    bytesInUse: Long,
    items: List<MediaItem>,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(VzSurface)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(16.dp)
    ) {
        Text(
            text = title,
            color = VzOnBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "$itemCount $unitLabel no total  |  ${StorageAnalyzer.formatBytes(bytesInUse)} em uso",
            color = VzOnSurfaceVariant,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (items.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            LazyHorizontalGrid(
                rows = GridCells.Fixed(1),
                userScrollEnabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(items, key = { it.uri.toString() }) { item ->
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(VzSurfaceVariant)
                    ) {
                        MediaPreview(item = item)
                    }
                }
            }
        } else {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Nada por aqui",
                color = VzOnSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun MediaPreview(item: MediaItem) {
    val context = LocalContext.current

    val request = if (item.isVideo) {
        val frameMs = frameMillisFor(item.duracaoMs)
        ImageRequest.Builder(context)
            .data(item.uri)
            .videoFrameMillis(frameMs)
            .size(200)
            .precision(Precision.INEXACT)
            .memoryCacheKey("${item.uri}@$frameMs")
            .build()
    } else {
        ImageRequest.Builder(context)
            .data(item.uri)
            .size(200)
            .precision(Precision.INEXACT)
            .memoryCacheKey(item.uri.toString())
            .build()
    }

    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}