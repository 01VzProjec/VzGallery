package com.vztechcore.vzgallery.ui.screens.cleanup

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.vztechcore.vzgallery.data.AnimationMode
import com.vztechcore.vzgallery.data.Prefs
import com.vztechcore.vzgallery.data.SnackbarBus
import com.vztechcore.vzgallery.data.StorageAnalyzer
import com.vztechcore.vzgallery.data.TrashManager
import com.vztechcore.vzgallery.model.MediaItem
import com.vztechcore.vzgallery.ui.anim.bottomBarEnter
import com.vztechcore.vzgallery.ui.anim.bottomBarExit
import com.vztechcore.vzgallery.ui.components.SelectionBottomBar
import com.vztechcore.vzgallery.ui.components.SelectionTopBar
import com.vztechcore.vzgallery.ui.components.VideoThumbCache
import com.vztechcore.vzgallery.ui.screens.viewer.ViewerScreen
import com.vztechcore.vzgallery.ui.theme.VzBackground
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzOnSurfaceVariant
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import com.vztechcore.vzgallery.ui.theme.VzSurface
import com.vztechcore.vzgallery.ui.theme.VzSurfaceVariant
import kotlinx.coroutines.launch

@Composable
fun LargestDetailScreen(
    title: String,
    items: List<MediaItem>,
    onBack: () -> Unit,
    onChanged: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { Prefs(context) }

    val animationMode by prefs.animationMode.collectAsState(initial = AnimationMode.ANIM_1)

    val boundsCache = remember { mutableStateMapOf<String, Rect>() }

    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var viewerOpen by remember { mutableStateOf(false) }
    var viewerIndex by remember { mutableStateOf(0) }

    var showDeleteDialog by remember { mutableStateOf(false) }

    val selectionMode = selected.isNotEmpty()
    val allSelected = items.isNotEmpty() && selected.size == items.size

    BackHandler {
        if (selectionMode) selected = emptySet()
        else onBack()
    }

        fun moveSelectionToTrash() {
        val toDelete = items.filter { it.uri.toString() in selected }
        scope.launch {
            val moved = toDelete.mapNotNull { TrashManager.moveToTrash(it) }
            selected = emptySet()
            val okCount = moved.size
            if (okCount > 0) {
                SnackbarBus.showWithAction(
                    text = if (okCount == 1) "1 item excluído" else "$okCount itens excluídos",
                    actionLabel = "Desfazer",
                    onAction = {
                        scope.launch {
                            moved.forEach { TrashManager.restore(it) }
                            onChanged()
                        }
                    }
                )
            }
            onChanged()
        }
        showDeleteDialog = false
    }

    fun deleteSelectionPermanent() {
        val toDelete = items.filter { it.uri.toString() in selected }
        val total = toDelete.size
        scope.launch {
            val deletedUris = mutableSetOf<String>()
            toDelete.forEach { item ->
                try {
                    context.contentResolver.delete(item.uri, null, null)
                    deletedUris.add(item.uri.toString())
                } catch (_: Exception) { /* continua tentando os demais */ }
            }
            selected = emptySet() // sempre sai do modo seleção
            val okCount = deletedUris.size
            val text = when {
                okCount == 0 -> "Nenhum item pôde ser apagado"
                okCount == total -> if (okCount == 1) "1 item apagado" else "$okCount itens apagados"
                else -> "$okCount de $total itens apagados"
            }
            SnackbarBus.show(text)
            onChanged()
        }
        showDeleteDialog = false
    }

    fun shareSelection() {
        val uris = selected.map { Uri.parse(it) as android.os.Parcelable }
        val hasVideo = items.any { it.isVideo && it.uri.toString() in selected }
        val hasImage = items.any { !it.isVideo && it.uri.toString() in selected }
        val type = when {
            hasVideo && hasImage -> "*/*"
            hasVideo -> "video/*"
            else -> "image/*"
        }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            this.type = type
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Enviar via"))
    }

    Box(modifier = Modifier.fillMaxSize().background(VzBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            if (selectionMode) {
                SelectionTopBar(
                    selectedCount = selected.size,
                    allSelected = allSelected,
                    onClose = { selected = emptySet() },
                    onSelectAll = {
                        selected = if (allSelected) emptySet()
                        else items.map { it.uri.toString() }.toSet()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(50))
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = VzOnBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${items.size} itens · ${StorageAnalyzer.formatBytes(items.sumOf { it.tamanhoBytes })}",
                            color = VzOnSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 4.dp,
                    bottom = if (selectionMode) 110.dp else 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(items, key = { it.uri.toString() }) { item ->
                    val uri = item.uri.toString()
                    val isSelected = uri in selected

                    LargestDetailRow(
                        item = item,
                        isSelected = isSelected,
                        onClick = {
                            if (selectionMode) {
                                selected = if (uri in selected) selected - uri
                                else selected + uri
                            } else {
                                val idx = items.indexOfFirst { it.uri.toString() == uri }
                                if (idx >= 0) {
                                    viewerIndex = idx
                                    viewerOpen = true
                                }
                            }
                        },
                        onLongClick = { selected = selected + uri },
                        onPositioned = { rect -> boundsCache[uri] = rect }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = selectionMode,
            enter = bottomBarEnter(animationMode),
            exit = bottomBarExit(animationMode),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            SelectionBottomBar(
                onDelete = { showDeleteDialog = true },
                onHide = { /* not used */ },
                onShare = { shareSelection() }
            )
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
                    scope.launch {
                        val moved = TrashManager.moveToTrash(item)
                        if (moved != null) {
                            SnackbarBus.showWithAction(
                                text = "Item excluído",
                                actionLabel = "Desfazer",
                                onAction = {
                                    scope.launch {
                                        TrashManager.restore(moved)
                                        onChanged()
                                    }
                                }
                            )
                        }
                    }
                    viewerOpen = false
                    onChanged()
                },
                onHide = { item ->
                    scope.launch { prefs.hideItems(setOf(item.uri.toString())) }
                    viewerOpen = false
                },
                onShare = { item ->
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = if (item.isVideo) "video/*" else "image/*"
                        putExtra(Intent.EXTRA_STREAM, item.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Enviar via"))
                },
                onDeletePermanently = { item ->
                    scope.launch {
                        try {
                            context.contentResolver.delete(item.uri, null, null)
                            SnackbarBus.show("Item apagado")
                        } catch (_: Exception) {}
                    }
                    viewerOpen = false
                    onChanged()
                }
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = VzSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = if (selected.size == 1) "Excluir 1 item?" else "Excluir ${selected.size} itens?",
                    style = MaterialTheme.typography.titleLarge,
                    color = VzOnBackground
                )
            },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Outlined.RestoreFromTrash,
                            contentDescription = null,
                            tint = VzSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Lixeira",
                            color = VzSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "O item sai do celular agora, mas fica guardado dentro do VzGallery pelas próximas 24 horas. Você pode restaurar a qualquer momento. Durante esse período, ele ocupa espaço no app — o que aumenta o tamanho do VzGallery. Você pode esvaziar a lixeira quando quiser pra liberar esse espaço.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VzOnSurfaceVariant,
                        fontSize = 12.sp
                    )

                    Spacer(Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Filled.DeleteForever,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Excluir permanentemente",
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "O item é apagado de vez. Não dá pra recuperar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VzOnSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { deleteSelectionPermanent() }) {
                    Text("Excluir", color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancelar", color = VzOnSurfaceVariant)
                    }
                    TextButton(onClick = { moveSelectionToTrash() }) {
                        Text("Lixeira", color = VzSecondary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LargestDetailRow(
    item: MediaItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPositioned: (Rect) -> Unit
) {
    val radius = RoundedCornerShape(14.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(radius)
            .background(if (isSelected) VzSecondary.copy(alpha = 0.15f) else VzSurface)
            .combinedClickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(VzSurfaceVariant)
        ) {
            MediaPreview(item = item)
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.nome,
                color = VzOnBackground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.relativePath.trimEnd('/').ifBlank { "/" },
                color = VzOnSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = StorageAnalyzer.formatBytes(item.tamanhoBytes),
                color = VzSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MediaPreview(item: MediaItem) {
    val context = LocalContext.current

    if (item.isVideo) {
        val bmp by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, item.uri) {
            value = VideoThumbCache.get(context, item.uri, 200)
        }
        bmp?.let {
            androidx.compose.foundation.Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
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