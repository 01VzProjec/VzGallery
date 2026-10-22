package com.vztechcore.vzgallery.ui.screens.stats

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import coil.size.Precision
import com.vztechcore.vzgallery.data.MediaScanner
import com.vztechcore.vzgallery.data.Prefs
import com.vztechcore.vzgallery.model.MediaItem
import com.vztechcore.vzgallery.ui.screens.stats.components.ChartLegend
import com.vztechcore.vzgallery.ui.screens.stats.components.ChartSlice
import com.vztechcore.vzgallery.ui.screens.stats.components.DonutChart
import com.vztechcore.vzgallery.ui.screens.stats.components.HorizontalBarChart
import com.vztechcore.vzgallery.ui.screens.stats.components.MonthlyBarChart
import com.vztechcore.vzgallery.ui.theme.VzBackground
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzOnSurfaceVariant
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import com.vztechcore.vzgallery.ui.theme.VzSurface
import com.vztechcore.vzgallery.ui.theme.VzSurfaceVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val FORMAT_PALETTE = listOf(
    Color(0xFF38BDF8),
    Color(0xFF1E40AF),
    Color(0xFF10B981),
    Color(0xFFF59E0B),
    Color(0xFFEF4444),
    Color(0xFF8B5CF6),
    Color(0xFFEC4899),
    Color(0xFF64748B),
)

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

@Composable
fun StatsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scanner = remember { MediaScanner(context) }
    val prefs = remember { Prefs(context) }

    val mostrarSvg by prefs.showSvg.collectAsState(initial = false)

    var allItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    BackHandler { onBack() }

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
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = VzOnBackground
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Estatísticas",
                style = MaterialTheme.typography.displayLarge,
                color = VzOnBackground,
                fontSize = 28.sp
            )
        }

        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = VzSecondary,
                    trackColor = VzSurfaceVariant
                )
            }
        } else {
            val pOverview = animateOnMount(delayMs = 0L,   durationMs = 1300)
            val pDonut    = animateOnMount(delayMs = 120L, durationMs = 1100)
            val pFormats  = animateOnMount(delayMs = 300L, durationMs = 900)
            val pMonths   = animateOnMount(delayMs = 420L, durationMs = 1000)
            val pLargest  = animateOnMount(delayMs = 560L, durationMs = 800)
            val pFolders  = animateOnMount(delayMs = 680L, durationMs = 800)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OverviewCard(items = items, progress = pOverview)

                Spacer(Modifier.height(20.dp))

                SectionTitle("Composição")
                SectionCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val fotos = items.count { !it.isVideo }
                        val videos = items.count { it.isVideo }
                        val slices = listOf(
                            ChartSlice("Fotos", fotos, FORMAT_PALETTE[0]),
                            ChartSlice("Vídeos", videos, FORMAT_PALETTE[1]),
                        )

                        DonutChart(
                            slices = slices,
                            size = 140.dp,
                            progress = pDonut
                        )

                        Spacer(Modifier.width(20.dp))

                        ChartLegend(
                            slices = slices,
                            modifier = Modifier.weight(1f),
                            progress = pDonut
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                SectionTitle("Por formato")
                SectionCard {
                    val formatos = items
                        .groupBy { it.nome.substringAfterLast('.', "").uppercase().ifBlank { "?" } }
                        .map { (ext, list) -> ext to list.size }
                        .sortedByDescending { it.second }
                        .take(8)
                        .mapIndexed { idx, (ext, count) ->
                            ChartSlice(
                                label = ext,
                                value = count,
                                color = FORMAT_PALETTE[idx % FORMAT_PALETTE.size]
                            )
                        }
                    Box(modifier = Modifier.padding(20.dp)) {
                        HorizontalBarChart(slices = formatos, progress = pFormats)
                    }
                }

                Spacer(Modifier.height(20.dp))

                SectionTitle("Últimos 12 meses")
                SectionCard {
                    val meses = computeLast12Months(items)
                    Box(modifier = Modifier.padding(20.dp)) {
                        MonthlyBarChart(values = meses, progress = pMonths)
                    }
                }

                Spacer(Modifier.height(20.dp))

                SectionTitle("Maiores arquivos")
                SectionCard {
                    val maiores = items
                        .sortedByDescending { it.tamanhoBytes }
                        .take(10)
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        maiores.forEachIndexed { idx, item ->
                            LargestFileRow(
                                idx = idx,
                                item = item,
                                total = maiores.size,
                                progress = pLargest
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                SectionTitle("Pastas que mais ocupam")
                SectionCard {
                    val pastas = items
                        .groupBy { it.relativePath.ifBlank { "/" } }
                        .map { (path, list) ->
                            val nome = path.trimEnd('/').substringAfterLast('/').ifBlank { path }
                            Triple(nome, list.sumOf { it.tamanhoBytes }, list.size)
                        }
                        .sortedByDescending { it.second }
                        .take(10)

                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        pastas.forEachIndexed { idx, (nome, size, count) ->
                            FolderRow(
                                idx = idx,
                                nome = nome,
                                size = size,
                                count = count,
                                total = pastas.size,
                                progress = pFolders
                            )
                        }
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun animateOnMount(
    delayMs: Long = 0L,
    durationMs: Int = 900
): Float {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (delayMs > 0) delay(delayMs)
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMs, easing = FastOutSlowInEasing)
        )
    }
    return anim.value
}

@Composable
private fun OverviewCard(items: List<MediaItem>, progress: Float) {
    val totalFotos = items.count { !it.isVideo }
    val totalVideos = items.count { it.isVideo }
    val totalBytes = items.sumOf { it.tamanhoBytes }
    val animatedBytes = (totalBytes * progress).toLong()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(VzSurface)
            .padding(20.dp)
    ) {
        Text(
            text = "Visão geral",
            color = VzSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = formatBytes(animatedBytes),
            color = VzOnBackground,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "$totalFotos ${if (totalFotos == 1) "foto" else "fotos"} · $totalVideos ${if (totalVideos == 1) "vídeo" else "vídeos"}",
            color = VzOnSurfaceVariant,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun LargestFileRow(
    idx: Int,
    item: MediaItem,
    total: Int,
    progress: Float
) {
    val itemP = ((progress * (total + 2f)) - idx).coerceIn(0f, 1f)
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .graphicsLayer {
                alpha = itemP
                translationY = (1f - itemP) * 16f
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(VzSurfaceVariant),
            contentAlignment = Alignment.Center
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
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(3.dp)
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
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(3.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
            )
            Text(
                text = "${idx + 1}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.nome,
                color = VzOnBackground,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.relativePath.trimEnd('/'),
                color = VzOnSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = formatBytes(item.tamanhoBytes),
            color = VzSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FolderRow(
    idx: Int,
    nome: String,
    size: Long,
    count: Int,
    total: Int,
    progress: Float
) {
    val itemP = ((progress * (total + 2f)) - idx).coerceIn(0f, 1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .graphicsLayer {
                alpha = itemP
                translationY = (1f - itemP) * 16f
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${idx + 1}",
            color = VzSecondary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.width(24.dp)
        )

        Icon(
            imageVector = Icons.Filled.Folder,
            contentDescription = null,
            tint = VzSecondary.copy(alpha = 0.85f),
            modifier = Modifier.size(20.dp)
        )

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nome.ifBlank { "Raiz" },
                color = VzOnBackground,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$count ${if (count == 1) "item" else "itens"}",
                color = VzOnSurfaceVariant,
                fontSize = 10.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = formatBytes(size),
            color = VzSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = VzOnBackground,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
    )
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(VzSurface)
    ) {
        content()
    }
}

private fun computeLast12Months(items: List<MediaItem>): List<Pair<String, Int>> {
    val agora = Calendar.getInstance()
    val resultado = mutableListOf<Pair<String, Int>>()

    for (i in 11 downTo 0) {
        val cal = Calendar.getInstance()
        cal.time = agora.time
        cal.add(Calendar.MONTH, -i)

        val ano = cal.get(Calendar.YEAR)
        val mes = cal.get(Calendar.MONTH)

        val inicio = Calendar.getInstance().apply {
            set(Calendar.YEAR, ano)
            set(Calendar.MONTH, mes)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val fim = Calendar.getInstance().apply {
            set(Calendar.YEAR, ano)
            set(Calendar.MONTH, mes)
            set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val count = items.count { item ->
            val ms = item.dataAdicionada * 1000
            ms in inicio..fim
        }

        val fmt = SimpleDateFormat("MMM", Locale("pt", "BR"))
        val label = fmt.format(Date(inicio)).replaceFirstChar { it.uppercase() }

        resultado.add(label to count)
    }

    return resultado
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