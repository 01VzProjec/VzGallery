package com.vztechcore.vzgallery.ui.screens.settings.pages

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vztechcore.vzgallery.data.AnimationMode
import com.vztechcore.vzgallery.data.MediaScanner
import com.vztechcore.vzgallery.data.Prefs
import com.vztechcore.vzgallery.data.SnackbarBus
import com.vztechcore.vzgallery.model.MediaItem
import com.vztechcore.vzgallery.ui.anim.bottomBarEnter
import com.vztechcore.vzgallery.ui.anim.bottomBarExit
import com.vztechcore.vzgallery.ui.components.MediaThumb
import com.vztechcore.vzgallery.ui.screens.viewer.ViewerScreen
import com.vztechcore.vzgallery.ui.theme.VzBackground
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzOnSurfaceVariant
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import com.vztechcore.vzgallery.ui.theme.VzSurface
import com.vztechcore.vzgallery.ui.theme.VzSurfaceVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class HiddenMode { ALL, FOLDERS, ITEMS }

private val VIDEO_EXTS = setOf(
    "mp4", "mkv", "mov", "webm", "avi", "3gp", "m4v",
    "flv", "wmv", "mpeg", "mpg", "ts", "m2ts", "ogv"
)

private const val MIN_VALID_SEG = 946_684_800L
private const val MAX_VALID_SEG = 4_102_444_800L

private fun normalizeTimestamp(v: Long): Long {
    if (v <= 0) return -1L
    return when {
        v in MIN_VALID_SEG..MAX_VALID_SEG -> v
        v in (MIN_VALID_SEG * 1000L)..(MAX_VALID_SEG * 1000L) -> v / 1000L
        else -> -1L
    }
}

private fun isSvgByName(name: String): Boolean {
    val ext = name.substringAfterLast('.', "").lowercase()
    return ext == "svg" || ext == "svgz"
}

private fun uriToMediaItem(context: Context, uriString: String): MediaItem {
    val uri = Uri.parse(uriString)
    val uriIndicatesVideo = uriString.contains("/video/") ||
            uriString.contains("/videos/") ||
            VIDEO_EXTS.contains(uriString.substringAfterLast('.', "").lowercase())

    var nome = uriString.substringAfterLast('/').ifBlank { "item" }
    var dataAdicionada = 0L
    var tamanhoBytes = 0L
    var isVideo = uriIndicatesVideo
    var relativePath = ""

    try {
        val projection = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE
        )
        if (!uriIndicatesVideo) {
            projection.add(MediaStore.Images.Media.DATE_TAKEN)
        }
        projection.add(MediaStore.MediaColumns.RELATIVE_PATH)

        context.contentResolver.query(
            uri,
            projection.toTypedArray(),
            null, null, null
        )?.use { c: Cursor ->
            if (c.moveToFirst()) {
                c.getString(c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME))
                    ?.let { nome = it }

                val mimeIdx = c.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                if (mimeIdx >= 0) {
                    val mime = c.getString(mimeIdx) ?: ""
                    if (mime.startsWith("video/")) isVideo = true
                    if (mime.startsWith("image/")) isVideo = false
                }

                val sizeIdx = c.getColumnIndex(MediaStore.MediaColumns.SIZE)
                if (sizeIdx >= 0) tamanhoBytes = c.getLong(sizeIdx)

                val relIdx = c.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                if (relIdx >= 0) {
                    relativePath = c.getString(relIdx)?.replace('\\', '/') ?: ""
                }

                val colTaken = if (!uriIndicatesVideo) {
                    c.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                } else -1
                val colAdded = c.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
                val colMod = c.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)

                val candidates = intArrayOf(colTaken, colAdded, colMod)
                for (col in candidates) {
                    if (col < 0) continue
                    val norm = normalizeTimestamp(c.getLong(col))
                    if (norm > 0) {
                        dataAdicionada = norm
                        break
                    }
                }
            }
        }
    } catch (_: Exception) {}

    return MediaItem(
        id = uriString.hashCode().toLong(),
        uri = uri,
        nome = nome,
        dataAdicionada = dataAdicionada,
        isVideo = isVideo,
        relativePath = relativePath,
        tamanhoBytes = tamanhoBytes
    )
}

private data class HiddenSection(
    val title: String,
    val key: String,
    val items: List<MediaItem>,
    val uris: Set<String> = items.map { it.uri.toString() }.toSet()
)

@Composable
fun AllHiddenScreen(
    initialMode: HiddenMode,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { Prefs(context) }

    val hiddenItems by prefs.hiddenItems.collectAsState(initial = emptySet())
    val hiddenAt by prefs.hiddenAt.collectAsState(initial = emptyMap())
    val hiddenFolders by prefs.hiddenFolders.collectAsState(initial = emptySet())
    val showSvg by prefs.showSvg.collectAsState(initial = false)
    val animationMode by prefs.animationMode.collectAsState(initial = AnimationMode.ANIM_1)
    val showName by prefs.showName.collectAsState(initial = false)
    val showFavoriteBadge by prefs.showFavoriteBadge.collectAsState(initial = true)
    val favoritos by prefs.favorites.collectAsState(initial = emptySet())

    var mode by remember { mutableStateOf(initialMode) }
    var modeMenuOpen by remember { mutableStateOf(false) }

    var loading by remember { mutableStateOf(true) }
    var allItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        allItems = withContext(Dispatchers.IO) {
            MediaScanner(context).loadMedia()
        }
        loading = false
    }

    val gridState = rememberLazyGridState()
    val boundsCache = remember { mutableStateMapOf<String, Rect>() }

    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var viewerOpen by remember { mutableStateOf(false) }
    var viewerIndex by remember { mutableStateOf(0) }

    val selectionMode = selected.isNotEmpty()

    BackHandler {
        if (selectionMode) selected = emptySet()
        else onBack()
    }

    fun restoreSelection() {
        if (selected.isEmpty()) return
        val count = selected.size
        scope.launch {
            prefs.showItems(selected)
            selected = emptySet()
            SnackbarBus.show(
                if (count == 1) "1 item restaurado"
                else "$count itens restaurados"
            )
        }
    }

    val manuallyHidden: List<MediaItem> = remember(hiddenItems, allItems, showSvg) {
        val byUri = allItems.associateBy { it.uri.toString() }
        hiddenItems
            .mapNotNull { uri ->
                val item = byUri[uri]
                if (item != null) {
                    item
                } else {
                    if (!showSvg) {
                        val ext = uri.substringAfterLast('.', "").lowercase()
                        if (ext == "svg" || ext == "svgz") return@mapNotNull null
                    }
                    uriToMediaItem(context, uri)
                }
            }
            .filter { item -> showSvg || !isSvgByName(item.nome) }
    }

    val folderHidden: List<MediaItem> = remember(hiddenFolders, allItems, showSvg) {
        if (hiddenFolders.isEmpty()) {
            emptyList()
        } else {
            allItems
                .filter { item ->
                    val path = item.relativePath
                    hiddenFolders.any { folder -> path.startsWith(folder) }
                }
                .filter { item -> showSvg || !isSvgByName(item.nome) }
        }
    }

    val visibleItems: List<MediaItem> = remember(mode, manuallyHidden, folderHidden) {
        when (mode) {
            HiddenMode.ITEMS -> manuallyHidden
            HiddenMode.FOLDERS -> folderHidden
            HiddenMode.ALL -> (manuallyHidden + folderHidden).distinctBy { it.uri.toString() }
        }
    }

    val sections: List<HiddenSection> = remember(
        mode, visibleItems, hiddenAt, hiddenFolders, manuallyHidden, folderHidden
    ) {
        when (mode) {
            HiddenMode.FOLDERS -> {
                visibleItems
                    .groupBy { it.relativePath.ifBlank { "/" } }
                    .map { (path, items) ->
                        val semBarra = path.trimEnd('/')
                        val nome = semBarra.substringAfterLast('/').ifBlank {
                            semBarra.ifBlank { "Raiz" }
                        }
                        HiddenSection(
                            title = "Pasta $nome",
                            key = "folder::$path",
                            items = items.sortedByDescending { it.dataAdicionada }
                        )
                    }
                    .sortedByDescending { it.items.size }
            }

            HiddenMode.ITEMS -> {
                val groups = mutableMapOf<String, MutableList<MediaItem>>()
                val groupTs = mutableMapOf<String, Long>()

                visibleItems.forEach { item ->
                    val uri = item.uri.toString()
                    val tsMs = hiddenAt[uri] ?: 0L
                    val label = if (tsMs > 0) formatHiddenDate(tsMs) else "Antes"

                    groups.getOrPut(label) { mutableListOf() }.add(item)
                    val current = groupTs[label] ?: 0L
                    if (tsMs > current) groupTs[label] = tsMs
                }

                groups.entries
                    .sortedByDescending { groupTs[it.key] ?: 0L }
                    .map { (label, items) ->
                        HiddenSection(
                            title = label,
                            key = "date::${groupTs[label] ?: 0L}::$label",
                            items = items.sortedByDescending { it.dataAdicionada }
                        )
                    }
            }

            HiddenMode.ALL -> {
                val result = mutableListOf<HiddenSection>()
                if (manuallyHidden.isNotEmpty()) {
                    result.add(
                        HiddenSection(
                            title = "Itens ocultos manualmente",
                            key = "all::manual",
                            items = manuallyHidden.sortedByDescending { it.dataAdicionada }
                        )
                    )
                }
                if (folderHidden.isNotEmpty()) {
                    result.add(
                        HiddenSection(
                            title = "Itens de pastas ocultas",
                            key = "all::folders",
                            items = folderHidden.sortedByDescending { it.dataAdicionada }
                        )
                    )
                }
                result
            }
        }
    }

    val allVisibleUris: Set<String> = remember(visibleItems) {
        visibleItems.map { it.uri.toString() }.toSet()
    }
    val allSelected = allVisibleUris.isNotEmpty() && selected.containsAll(allVisibleUris)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VzBackground)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ===== Top bar =====
            if (selectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { selected = emptySet() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Fechar seleção",
                            tint = VzOnBackground
                        )
                    }

                    Text(
                        text = if (selected.size == 1) "1 item selecionado"
                        else "${selected.size} itens selecionados",
                        style = MaterialTheme.typography.titleLarge,
                        color = VzOnBackground
                    )

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                selected = if (allSelected) {
                                    selected - allVisibleUris
                                } else {
                                    selected + allVisibleUris
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Checklist,
                            contentDescription = "Selecionar tudo",
                            tint = if (allSelected) VzSecondary else VzOnBackground
                        )
                    }
                }
            } else {
                HiddenTopBar(
                    mode = mode,
                    menuOpen = modeMenuOpen,
                    onMenuOpenChange = { modeMenuOpen = it },
                    onBack = onBack,
                    onSelectMode = { newMode -> mode = newMode }
                )
            }

            // ===== Content =====
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
            } else if (visibleItems.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(VzSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VisibilityOff,
                            contentDescription = null,
                            tint = VzSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = when (mode) {
                            HiddenMode.ITEMS -> "Nenhum item oculto"
                            HiddenMode.FOLDERS -> "Nenhum item de pastas ocultas"
                            HiddenMode.ALL -> "Nada oculto ainda"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = VzOnBackground
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = when (mode) {
                            HiddenMode.ITEMS -> "Itens que você ocultar vão aparecer aqui."
                            HiddenMode.FOLDERS -> "Itens de pastas ocultas vão aparecer aqui."
                            HiddenMode.ALL -> "Itens ocultos manualmente e de pastas ocultas aparecem aqui."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = VzOnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        end = 8.dp,
                        top = 8.dp,
                        bottom = if (selectionMode) 100.dp else 24.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sections.forEach { section ->
                        item(
                            key = "header_${section.key}",
                            span = { GridItemSpan(maxLineSpan) }
                        ) {
                            val sectionUris = remember(section) { section.uris }
                            val allSectionSelected = sectionUris.isNotEmpty() &&
                                    selected.containsAll(sectionUris)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 6.dp, start = 6.dp, end = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = section.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = VzOnBackground,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )

                                if (selectionMode) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (allSectionSelected) VzSecondary
                                                else VzSurfaceVariant
                                            )
                                            .clickable(
                                                indication = null,
                                                interactionSource = remember { MutableInteractionSource() }
                                            ) {
                                                selected = if (allSectionSelected) {
                                                    selected - sectionUris
                                                } else {
                                                    selected + sectionUris
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Checklist,
                                            contentDescription = "Selecionar bloco",
                                            tint = if (allSectionSelected) VzBackground
                                            else VzOnSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        items(
                            items = section.items,
                            key = { item -> "${section.key}::${item.uri}" }
                        ) { item ->
                            val uri = item.uri.toString()
                            MediaThumb(
                                item = item,
                                isSelected = uri in selected,
                                isFavorite = uri in favoritos,
                                showName = showName,
                                showFavoriteBadge = showFavoriteBadge,
                                onClick = {
                                    if (selectionMode) {
                                        selected = if (uri in selected) selected - uri
                                        else selected + uri
                                    } else {
                                        val idx = visibleItems.indexOfFirst {
                                            it.uri.toString() == uri
                                        }
                                        if (idx >= 0) {
                                            viewerIndex = idx
                                            viewerOpen = true
                                        }
                                    }
                                },
                                onLongClick = {
                                    selected = selected + uri
                                },
                                onPositioned = { rect -> boundsCache[uri] = rect }
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selectionMode,
            enter = bottomBarEnter(animationMode),
            exit = bottomBarExit(animationMode),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Button(
                    onClick = { restoreSelection() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VzSecondary,
                        contentColor = VzBackground
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (selected.size == 1) "Restaurar item" else "Restaurar ${selected.size} itens",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        if (viewerOpen && visibleItems.isNotEmpty()) {
            ViewerScreen(
                items = visibleItems,
                startIndex = viewerIndex,
                getCurrentBounds = { item -> boundsCache[item.uri.toString()] },
                onClose = { idx ->
                    viewerOpen = false
                    viewerIndex = idx
                },
                onDelete = { item ->
                    val uri = item.uri.toString()
                    scope.launch {
                        val moved = com.vztechcore.vzgallery.data.TrashManager.moveToTrash(item)
                        if (moved != null) {
                            prefs.showItems(setOf(uri))
                            SnackbarBus.showWithAction(
                                text = "Item excluído",
                                actionLabel = "Desfazer",
                                onAction = {
                                    scope.launch {
                                        com.vztechcore.vzgallery.data.TrashManager.restore(moved)
                                        prefs.hideItems(setOf(uri))
                                    }
                                }
                            )
                        }
                    }
                    viewerOpen = false
                },
                onHide = { item ->
                    viewerOpen = false
                    val uri = item.uri.toString()
                    scope.launch {
                        delay(120)
                        prefs.showItems(setOf(uri))
                        SnackbarBus.show("Item restaurado")
                    }
                },
                onShare = { item ->
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = if (item.isVideo) "video/*" else "image/*"
                        putExtra(Intent.EXTRA_STREAM, item.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Enviar via"))
                },
                hiddenMode = true,
                onDeletePermanently = { item ->
                    val uri = item.uri.toString()
                    scope.launch {
                        try {
                            context.contentResolver.delete(item.uri, null, null)
                            prefs.showItems(setOf(uri))
                            SnackbarBus.show("Item apagado")
                        } catch (_: Exception) {}
                    }
                    viewerOpen = false
                }
            )
        }
    }
}

/**
 * Top bar with a clickable title + chevron-down.
 * Tap the title → dropdown opens right below it.
 */
@Composable
private fun HiddenTopBar(
    mode: HiddenMode,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onSelectMode: (HiddenMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button
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

        // Title + chevron (clickable) + dropdown anchored to it
        Box {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onMenuOpenChange(true) }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = when (mode) {
                        HiddenMode.ALL -> "Todos ocultos"
                        HiddenMode.ITEMS -> "Itens ocultos manualmente"
                        HiddenMode.FOLDERS -> "Itens de pastas ocultas"
                    },
                    style = MaterialTheme.typography.displayLarge,
                    color = VzOnBackground,
                    fontSize = 22.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Abrir opções",
                    tint = VzSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { onMenuOpenChange(false) },
                modifier = Modifier.background(VzSurface)
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            "Todos ocultos",
                            color = if (mode == HiddenMode.ALL) VzSecondary else VzOnBackground,
                            fontWeight = if (mode == HiddenMode.ALL) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelectMode(HiddenMode.ALL)
                        onMenuOpenChange(false)
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            "Pastas ocultas",
                            color = if (mode == HiddenMode.FOLDERS) VzSecondary else VzOnBackground,
                            fontWeight = if (mode == HiddenMode.FOLDERS) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelectMode(HiddenMode.FOLDERS)
                        onMenuOpenChange(false)
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            "Itens ocultos manualmente",
                            color = if (mode == HiddenMode.ITEMS) VzSecondary else VzOnBackground,
                            fontWeight = if (mode == HiddenMode.ITEMS) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelectMode(HiddenMode.ITEMS)
                        onMenuOpenChange(false)
                    }
                )
            }
        }
    }
}

private fun formatHiddenDate(tsMs: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = tsMs }
    val today = Calendar.getInstance()

    val sameYear = cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
    val sameDay = sameYear &&
            cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)

    return when {
        sameDay -> "Hoje"
        else -> {
            val pattern = if (sameYear) "d 'de' MMM" else "d 'de' MMM 'de' yyyy"
            val sdf = SimpleDateFormat(pattern, Locale("pt", "BR"))
            sdf.format(Date(tsMs)).replaceFirstChar { it.uppercase() }
        }
    }
}