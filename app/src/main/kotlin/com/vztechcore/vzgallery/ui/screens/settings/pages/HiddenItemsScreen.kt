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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vztechcore.vzgallery.data.AnimationMode
import com.vztechcore.vzgallery.data.Prefs
import com.vztechcore.vzgallery.data.SnackbarBus
import com.vztechcore.vzgallery.data.TrashManager
import com.vztechcore.vzgallery.model.MediaItem
import com.vztechcore.vzgallery.ui.anim.bottomBarEnter
import com.vztechcore.vzgallery.ui.anim.bottomBarExit
import com.vztechcore.vzgallery.ui.components.MediaThumb
import com.vztechcore.vzgallery.ui.screens.settings.SettingsTopBar
import com.vztechcore.vzgallery.ui.screens.viewer.ViewerScreen
import com.vztechcore.vzgallery.ui.theme.VzBackground
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzOnSurfaceVariant
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import com.vztechcore.vzgallery.ui.theme.VzSurfaceVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

private fun uriToMediaItem(context: Context, uriString: String): MediaItem {
    val uri = Uri.parse(uriString)

    val uriIndicatesVideo = uriString.contains("/video/") ||
            uriString.contains("/videos/") ||
            VIDEO_EXTS.contains(uriString.substringAfterLast('.', "").lowercase())

    var nome = uriString.substringAfterLast('/').ifBlank { "item" }
    var dataAdicionada = 0L
    var tamanhoBytes = 0L
    var isVideo = uriIndicatesVideo

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

        context.contentResolver.query(
            uri,
            projection.toTypedArray(),
            null,
            null,
            null
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
        tamanhoBytes = tamanhoBytes
    )
}

@Composable
fun HiddenItemsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { Prefs(context) }

    val hiddenItems by prefs.hiddenItems.collectAsState(initial = emptySet())
    val animationMode by prefs.animationMode.collectAsState(initial = AnimationMode.ANIM_1)
    val showName by prefs.showName.collectAsState(initial = false)
    val showFavoriteBadge by prefs.showFavoriteBadge.collectAsState(initial = true)
    val favoritos by prefs.favorites.collectAsState(initial = emptySet())

    var items by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    LaunchedEffect(hiddenItems) {
        items = withContext(Dispatchers.IO) {
            hiddenItems.map { uriToMediaItem(context, it) }
        }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VzBackground)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

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

                    Spacer(Modifier.size(42.dp))
                }
            } else {
                SettingsTopBar(title = "Itens ocultos", onBack = onBack)
            }

            if (items.isEmpty()) {
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
                        text = "Nenhum item oculto",
                        style = MaterialTheme.typography.titleLarge,
                        color = VzOnBackground
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Itens que você ocultar da galeria vão aparecer aqui.",
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
                    items(items, key = { it.uri.toString() }) { item ->
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
                                    val idx = items.indexOfFirst {
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

        if (viewerOpen && items.isNotEmpty()) {
            ViewerScreen(
                items = items,
                startIndex = viewerIndex,
                getCurrentBounds = { item -> boundsCache[item.uri.toString()] },
                onClose = { idx ->
                    viewerOpen = false
                    viewerIndex = idx
                },
                onDelete = { item ->
                    val uri = item.uri.toString()
                    scope.launch {
                        val moved = TrashManager.moveToTrash(item)
                        if (moved != null) {
                            prefs.showItems(setOf(uri))
                            SnackbarBus.showWithAction(
                                text = "Item excluído",
                                actionLabel = "Desfazer",
                                onAction = {
                                    scope.launch {
                                        TrashManager.restore(moved)
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
                hiddenMode = true
            )
        }
    }
}