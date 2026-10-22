package com.vztechcore.vzgallery.ui.screens.library

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.vztechcore.vzgallery.data.AlbumType
import com.vztechcore.vzgallery.data.AnimationMode
import com.vztechcore.vzgallery.data.MediaScanner
import com.vztechcore.vzgallery.data.Prefs
import com.vztechcore.vzgallery.data.SnackbarBus
import com.vztechcore.vzgallery.data.TimeFilter
import com.vztechcore.vzgallery.data.TrashManager
import com.vztechcore.vzgallery.model.MediaItem
import com.vztechcore.vzgallery.model.ehVideo
import com.vztechcore.vzgallery.ui.anim.ScreenSlideDirection
import com.vztechcore.vzgallery.ui.anim.depthDirection
import com.vztechcore.vzgallery.ui.anim.screenEnter
import com.vztechcore.vzgallery.ui.anim.screenExit
import com.vztechcore.vzgallery.ui.screens.library.components.RecentItemCard
import com.vztechcore.vzgallery.ui.screens.search.SearchAlbumScreen
import com.vztechcore.vzgallery.ui.screens.settings.pages.AllHiddenScreen
import com.vztechcore.vzgallery.ui.screens.settings.pages.HiddenMode
import com.vztechcore.vzgallery.ui.screens.settings.pages.TrashScreen
import com.vztechcore.vzgallery.ui.screens.viewer.ViewerScreen
import com.vztechcore.vzgallery.ui.theme.VzBackground
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzOnSurfaceVariant
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import com.vztechcore.vzgallery.ui.theme.VzSurface
import com.vztechcore.vzgallery.ui.theme.VzSurfaceVariant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

private enum class LibraryPage(val depth: Int) {
    Albums(0),
    Detail(1),
    FolderAlbum(1),
    Hidden(1),
    Trash(1)
}

private data class FolderAlbumInfo(
    val relativePath: String,
    val displayName: String,
    val itemCount: Int,
    val cover: MediaItem?
)

private fun frameMillisFor(durationMs: Long?): Long {
    return if ((durationMs ?: 0L) > 3000L) {
        (durationMs!! / 4L).coerceAtLeast(500L)
    } else {
        1000L
    }
}

@Composable
fun LibraryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val animationMode by prefs.animationMode.collectAsState(initial = AnimationMode.ANIM_1)
    val scanner = remember { MediaScanner(context) }

    var allItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var loadingGlobal by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        allItems = scanner.loadMedia()
        loadingGlobal = false
    }

    var currentPage by remember { mutableStateOf(LibraryPage.Albums) }
    var currentAlbum by remember { mutableStateOf<AlbumType?>(null) }
    var currentFolderPath by remember { mutableStateOf<String?>(null) }

    AnimatedContent(
        targetState = currentPage,
        transitionSpec = {
            val dir = depthDirection(initialState.depth, targetState.depth)
            screenEnter(animationMode, dir) togetherWith screenExit(animationMode, dir)
        },
        label = "library_page_transition"
    ) { page ->
        when (page) {
            LibraryPage.Albums -> AlbumsListScreen(
                onBack = onBack,
                allItems = allItems,
                loading = loadingGlobal,
                onOpenAlbum = { album ->
                    currentAlbum = album
                    currentPage = LibraryPage.Detail
                },
                onOpenFolder = { path ->
                    currentFolderPath = path
                    currentPage = LibraryPage.FolderAlbum
                },
                onOpenHidden = { currentPage = LibraryPage.Hidden },
                onOpenTrash = { currentPage = LibraryPage.Trash }
            )
            LibraryPage.Detail -> {
                val album = currentAlbum
                if (album != null) {
                    AlbumDetailScreen(
                        album = album,
                        onBack = {
                            currentAlbum = null
                            currentPage = LibraryPage.Albums
                        }
                    )
                } else {
                    currentPage = LibraryPage.Albums
                }
            }
            LibraryPage.FolderAlbum -> {
                val path = currentFolderPath
                if (path != null) {
                    SearchAlbumScreen(
                        albumPath = path,
                        query = "",
                        allItems = allItems,
                        onBack = {
                            currentFolderPath = null
                            currentPage = LibraryPage.Albums
                        }
                    )
                } else {
                    currentPage = LibraryPage.Albums
                }
            }
            LibraryPage.Hidden -> {
                AllHiddenScreen(
                    initialMode = HiddenMode.ALL,
                    onBack = { currentPage = LibraryPage.Albums }
                )
            }
            LibraryPage.Trash -> {
                TrashScreen(
                    onBack = { currentPage = LibraryPage.Albums }
                )
            }
        }
    }
}

@Composable
private fun AlbumsListScreen(
    onBack: () -> Unit,
    allItems: List<MediaItem>,
    loading: Boolean,
    onOpenAlbum: (AlbumType) -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenHidden: () -> Unit,
    onOpenTrash: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { Prefs(context) }

    val mostrarSvg by prefs.showSvg.collectAsState(initial = false)
    val itensOcultos by prefs.hiddenItems.collectAsState(initial = emptySet())
    val pastasOcultas by prefs.hiddenFolders.collectAsState(initial = emptySet())
    val favoritos by prefs.favorites.collectAsState(initial = emptySet())
    val albunsFavoritos by prefs.favoriteAlbums.collectAsState(initial = emptySet())
    val animationMode by prefs.animationMode.collectAsState(initial = AnimationMode.ANIM_1)

    val trashItems by TrashManager.items.collectAsState()

    var covers by remember { mutableStateOf<Map<AlbumType, MediaItem?>>(emptyMap()) }

    var selectedFilter by remember { mutableStateOf(TimeFilter.LAST_WEEK) }
    var viewerItem by remember { mutableStateOf<MediaItem?>(null) }

    BackHandler {
        if (viewerItem != null) viewerItem = null
        else onBack()
    }

    val baseItems = remember(allItems, mostrarSvg) {
        if (mostrarSvg) allItems else allItems.filterNot { isSvg(it) }
    }

    val filteredItems = remember(baseItems, itensOcultos, pastasOcultas) {
        var f = baseItems.filterNot { it.uri.toString() in itensOcultos }
        if (pastasOcultas.isNotEmpty()) {
            f = f.filterNot { item ->
                pastasOcultas.any { pasta -> item.relativePath.startsWith(pasta) }
            }
        }
        f
    }

    val hiddenCount = remember(baseItems, itensOcultos, pastasOcultas) {
        val manualCount = baseItems.count { it.uri.toString() in itensOcultos }
        val folderCount = if (pastasOcultas.isEmpty()) {
            0
        } else {
            baseItems.count { item ->
                item.uri.toString() !in itensOcultos &&
                    pastasOcultas.any { pasta -> item.relativePath.startsWith(pasta) }
            }
        }
        manualCount + folderCount
    }

    val counts = remember(filteredItems, favoritos) {
        AlbumType.entries.associateWith { filterForAlbum(filteredItems, it, favoritos).size }
    }

    LaunchedEffect(filteredItems, favoritos) {
        val map = mutableMapOf<AlbumType, MediaItem?>()
        for (album in AlbumType.entries) {
            val albumItems = filterForAlbum(filteredItems, album, favoritos)
            val customUri = prefs.customCover(album).first()
            val cover = if (customUri != null) {
                albumItems.firstOrNull { it.uri.toString() == customUri }
                    ?: albumItems.firstOrNull()
            } else {
                albumItems.firstOrNull()
            }
            map[album] = cover
        }
        covers = map
    }

    // First hidden item for the "Ocultos" background thumbnail
    val hiddenCoverUri: Uri? = remember(baseItems, itensOcultos, pastasOcultas) {
        val hiddenItem = baseItems.firstOrNull { item ->
            item.uri.toString() in itensOcultos ||
                pastasOcultas.any { pasta -> item.relativePath.startsWith(pasta) }
        }
        hiddenItem?.uri
    }

    // First trash item for the "Lixeira" background thumbnail
    val trashCoverUri: Uri? = remember(trashItems) {
        trashItems.firstOrNull()?.let { Uri.fromFile(File(it.trashFile)) }
    }

    val folderAlbums = remember(filteredItems, albunsFavoritos) {
        albunsFavoritos.mapNotNull { path ->
            val items = filteredItems.filter { it.relativePath == path }
            if (items.isEmpty()) return@mapNotNull null
            val semBarra = path.trimEnd('/')
            val nome = semBarra.substringAfterLast('/').ifBlank { semBarra.ifBlank { "Raiz" } }
            FolderAlbumInfo(
                relativePath = path,
                displayName = nome,
                itemCount = items.size,
                cover = items.firstOrNull()
            )
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
                text = "Biblioteca",
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                // Row 1: Fotos / Favoritos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AlbumCard(
                        album = AlbumType.ALL_PHOTOS,
                        cover = covers[AlbumType.ALL_PHOTOS],
                        count = counts[AlbumType.ALL_PHOTOS] ?: 0,
                        onClick = { onOpenAlbum(AlbumType.ALL_PHOTOS) },
                        modifier = Modifier.weight(1f)
                    )
                    AlbumCard(
                        album = AlbumType.FAVORITES,
                        cover = covers[AlbumType.FAVORITES],
                        count = counts[AlbumType.FAVORITES] ?: 0,
                        onClick = { onOpenAlbum(AlbumType.FAVORITES) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))

                // Row 2: Câmera / Vídeos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AlbumCard(
                        album = AlbumType.CAMERA,
                        cover = covers[AlbumType.CAMERA],
                        count = counts[AlbumType.CAMERA] ?: 0,
                        onClick = { onOpenAlbum(AlbumType.CAMERA) },
                        modifier = Modifier.weight(1f)
                    )
                    AlbumCard(
                        album = AlbumType.ALL_VIDEOS,
                        cover = covers[AlbumType.ALL_VIDEOS],
                        count = counts[AlbumType.ALL_VIDEOS] ?: 0,
                        onClick = { onOpenAlbum(AlbumType.ALL_VIDEOS) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))

                // Row 3: Ocultos / Lixeira
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SpecialCard(
                        icon = Icons.Outlined.VisibilityOff,
                        title = "Ocultos",
                        count = hiddenCount,
                        countLabel = { if (it == 1) "1 item" else "$it itens" },
                        coverUri = hiddenCoverUri,
                        onClick = onOpenHidden,
                        modifier = Modifier.weight(1f)
                    )
                    SpecialCard(
                        icon = Icons.Outlined.Delete,
                        title = "Lixeira",
                        count = trashItems.size,
                        countLabel = { if (it == 1) "1 item" else "$it itens" },
                        coverUri = trashCoverUri,
                        onClick = onOpenTrash,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (folderAlbums.isNotEmpty()) {
                    Spacer(Modifier.height(28.dp))
                    Text(
                        text = "Álbuns Favoritos",
                        style = MaterialTheme.typography.titleLarge,
                        color = VzOnBackground,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    )
                    Spacer(Modifier.height(10.dp))

                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(1),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(folderAlbums, key = { it.relativePath }) { album ->
                            FolderAlbumSquare(
                                album = album,
                                onClick = { onOpenFolder(album.relativePath) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                Text(
                    text = "Recentes",
                    style = MaterialTheme.typography.titleLarge,
                    color = VzOnBackground,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp
                )

                Spacer(Modifier.height(10.dp))

                TimeFilterChips(
                    selected = selectedFilter,
                    onSelect = { selectedFilter = it }
                )

                Spacer(Modifier.height(14.dp))
            }

            AnimatedContent(
                targetState = selectedFilter,
                transitionSpec = {
                    val dir = if (targetState.ordinal > initialState.ordinal)
                        ScreenSlideDirection.RIGHT
                    else
                        ScreenSlideDirection.LEFT
                    screenEnter(animationMode, dir) togetherWith screenExit(animationMode, dir)
                },
                label = "filter_transition",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) { filter ->
                val itemsForFilter = remember(filteredItems, filter) {
                    val startSec = filter.startTimeMs() / 1000
                    filteredItems
                        .filter { it.dataAdicionada >= startSec }
                        .sortedByDescending { it.dataAdicionada }
                }

                if (itemsForFilter.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum item nesse período",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VzOnSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(itemsForFilter, key = { _, it -> it.uri.toString() }) { index, item ->
                            RecentItemCard(
                                item = item,
                                reverse = index % 2 == 1,
                                onClick = { viewerItem = item }
                            )
                        }
                    }
                }
            }
        }
    }

    val currentViewerItem = viewerItem
    if (currentViewerItem != null) {
        ViewerScreen(
            items = listOf(currentViewerItem),
            startIndex = 0,
            getCurrentBounds = { null },
            onClose = { viewerItem = null },
            onDelete = { item ->
                scope.launch {
                    val moved = TrashManager.moveToTrash(item)
                    if (moved != null) {
                        SnackbarBus.showWithAction(
                            text = "Item excluído",
                            actionLabel = "Desfazer",
                            onAction = {
                                scope.launch { TrashManager.restore(moved) }
                            }
                        )
                    }
                }
                viewerItem = null
            },
            onHide = { item ->
                scope.launch {
                    prefs.hideItems(setOf(item.uri.toString()))
                    SnackbarBus.show("Item ocultado")
                }
                viewerItem = null
            },
            onShare = { item ->
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = if (item.ehVideo) "video/*" else "image/*"
                    putExtra(android.content.Intent.EXTRA_STREAM, item.uri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    android.content.Intent.createChooser(intent, "Enviar via")
                )
            },
            onDeletePermanently = { item ->
                scope.launch {
                    try {
                        context.contentResolver.delete(item.uri, null, null)
                        SnackbarBus.show("Item apagado")
                    } catch (_: Exception) {}
                }
                viewerItem = null
            }
        )
    }
}

@Composable
private fun FolderAlbumSquare(
    album: FolderAlbumInfo,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(VzSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (album.cover != null) {
                CoverImage(item = album.cover, sizePx = 300)
            } else {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    tint = VzSecondary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = album.displayName,
            style = MaterialTheme.typography.bodyMedium,
            color = VzOnBackground,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "${album.itemCount} ${if (album.itemCount == 1) "item" else "itens"}",
            style = MaterialTheme.typography.bodyMedium,
            color = VzSecondary,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun TimeFilterChips(
    selected: TimeFilter,
    onSelect: (TimeFilter) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TimeFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) VzSecondary else VzSurface)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onSelect(filter) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = filter.label,
                    color = if (isSelected) VzBackground else VzOnBackground,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 13.sp
                )
            }
        }
    }
}

/**
 * Standard album card (Fotos / Favoritos / Câmera / Vídeos).
 * Now 68.dp tall (was 84.dp) — thinner.
 */
@Composable
private fun AlbumCard(
    album: AlbumType,
    cover: MediaItem?,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(68.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(VzSurface)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(VzSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (cover != null) {
                CoverImage(item = cover, sizePx = 200)
            } else {
                Icon(
                    imageVector = album.icon,
                    contentDescription = null,
                    tint = VzSecondary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.title,
                style = MaterialTheme.typography.bodyLarge,
                color = VzOnBackground,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = labelForAlbum(album, count),
                style = MaterialTheme.typography.bodyMedium,
                color = VzSecondary,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Special card (Ocultos / Lixeira) — 68.dp tall.
 * The square box has the thumbnail (blurred) behind the icon.
 * The rectangle background stays normal.
 */
@Composable
private fun SpecialCard(
    icon: ImageVector,
    title: String,
    count: Int,
    countLabel: (Int) -> String,
    coverUri: Uri?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Row(
        modifier = modifier
            .height(68.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(VzSurface)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Square with blurred thumbnail behind the icon
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(VzSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (coverUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(coverUri)
                        .size(200)
                        .precision(Precision.INEXACT)
                        .memoryCacheKey(coverUri.toString())
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(4.dp)
                )
                // Dark overlay so the icon stands out
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                )
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VzSecondary,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = VzOnBackground,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = countLabel(count),
                style = MaterialTheme.typography.bodyMedium,
                color = VzSecondary,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Unified cover rendering (image or video) via Coil.
 * Videos → 25% frame.
 */
@Composable
private fun CoverImage(item: MediaItem, sizePx: Int) {
    val context = LocalContext.current

    val request = if (item.ehVideo) {
        val frameMs = frameMillisFor(item.duracaoMs)
        ImageRequest.Builder(context)
            .data(item.uri)
            .videoFrameMillis(frameMs)
            .size(sizePx)
            .precision(Precision.INEXACT)
            .memoryCacheKey("${item.uri}@$frameMs")
            .build()
    } else {
        ImageRequest.Builder(context)
            .data(item.uri)
            .size(sizePx)
            .precision(Precision.INEXACT)
            .crossfade(120)
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

private fun labelForAlbum(album: AlbumType, count: Int): String {
    return when (album) {
        AlbumType.ALL_PHOTOS -> if (count == 1) "1 foto" else "$count fotos"
        AlbumType.FAVORITES -> if (count == 1) "1 favorito" else "$count favoritos"
        AlbumType.CAMERA -> if (count == 1) "1 item" else "$count itens"
        AlbumType.ALL_VIDEOS -> if (count == 1) "1 vídeo" else "$count vídeos"
    }
}

internal fun isSvg(item: MediaItem): Boolean {
    val ext = item.nome.substringAfterLast('.', "").lowercase()
    return ext == "svg" || ext == "svgz"
}

internal fun filterForAlbum(
    items: List<MediaItem>,
    album: AlbumType,
    favorites: Set<String> = emptySet()
): List<MediaItem> {
    return when (album) {
        AlbumType.ALL_PHOTOS -> items.filterNot { it.ehVideo }
        AlbumType.FAVORITES -> items.filter { it.uri.toString() in favorites }
        AlbumType.CAMERA -> items.filter {
            val p = it.relativePath.lowercase()
            p.contains("dcim/camera") || p.endsWith("camera/") || p.startsWith("camera")
        }
        AlbumType.ALL_VIDEOS -> items.filter { it.ehVideo }
    }
}