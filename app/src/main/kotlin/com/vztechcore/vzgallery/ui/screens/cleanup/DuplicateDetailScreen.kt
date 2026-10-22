package com.vztechcore.vzgallery.ui.screens.cleanup

import android.net.Uri
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
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vztechcore.vzgallery.data.AnimationMode
import com.vztechcore.vzgallery.data.Prefs
import com.vztechcore.vzgallery.data.SnackbarBus
import com.vztechcore.vzgallery.data.StorageAnalyzer
import com.vztechcore.vzgallery.data.TrashManager
import com.vztechcore.vzgallery.model.MediaItem
import com.vztechcore.vzgallery.ui.anim.bottomBarEnter
import com.vztechcore.vzgallery.ui.anim.bottomBarExit
import com.vztechcore.vzgallery.ui.components.MediaThumb
import com.vztechcore.vzgallery.ui.components.SelectionBottomBar
import com.vztechcore.vzgallery.ui.screens.viewer.ViewerScreen
import com.vztechcore.vzgallery.ui.theme.VzBackground
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzOnSurfaceVariant
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import com.vztechcore.vzgallery.ui.theme.VzSurface
import kotlinx.coroutines.launch

@Composable
fun DuplicateDetailScreen(
    title: String,
    groups: List<StorageAnalyzer.DuplicateGroup>,
    onBack: () -> Unit,
    onChanged: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { Prefs(context) }

    val animationMode by prefs.animationMode.collectAsState(initial = AnimationMode.ANIM_1)
    val favoritos by prefs.favorites.collectAsState(initial = emptySet())
    val showName by prefs.showName.collectAsState(initial = false)

    data class Entry(val item: MediaItem, val isReal: Boolean)

    val entries: List<Entry> = remember(groups) {
        groups.flatMap { g ->
            val real = g.realItem
            val realUri = real?.uri?.toString()
            g.items.map { item ->
                Entry(item = item, isReal = item.uri.toString() == realUri)
            }
        }
    }

    val duplicateUris: Set<String> = remember(entries) {
        entries.filter { !it.isReal }.map { it.item.uri.toString() }.toSet()
    }

    val gridState = rememberLazyGridState()
    val boundsCache = remember { mutableStateMapOf<String, androidx.compose.ui.geometry.Rect>() }

    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var viewerOpen by remember { mutableStateOf(false) }
    var viewerIndex by remember { mutableStateOf(0) }

    var showDeleteDialog by remember { mutableStateOf(false) }

    val selectionMode = selected.isNotEmpty()
    val allDuplicatesSelected = duplicateUris.isNotEmpty() && selected.containsAll(duplicateUris)

    BackHandler {
        if (selectionMode) selected = emptySet()
        else onBack()
    }

    
        fun deleteSelection() {
        val toDelete = entries.map { it.item }.filter { it.uri.toString() in selected }
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
        val toDelete = entries.map { it.item }.filter { it.uri.toString() in selected }
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
        val hasVideo = entries.any { it.item.isVideo && it.item.uri.toString() in selected }
        val hasImage = entries.any { !it.item.isVideo && it.item.uri.toString() in selected }
        val type = when {
            hasVideo && hasImage -> "*/*"
            hasVideo -> "video/*"
            else -> "image/*"
        }
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND_MULTIPLE).apply {
            this.type = type
            putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Enviar via"))
    }

    val visibleItems = entries.map { it.item }

    Box(modifier = Modifier.fillMaxSize().background(VzBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
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
                                selected = if (allDuplicatesSelected) {
                                    selected - duplicateUris
                                } else {
                                    selected + duplicateUris
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Checklist,
                            contentDescription = "Selecionar todos os duplicados",
                            tint = if (allDuplicatesSelected) VzSecondary else VzOnBackground
                        )
                    }
                }
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = VzOnBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "★ = original · outros = duplicados",
                            color = VzOnSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = 8.dp,
                    bottom = if (selectionMode) 110.dp else 24.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item(
                    key = "hint",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(VzSurface)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Toque para abrir, segure para selecionar. O ícone ✓ no topo marca todos os duplicados de uma vez.",
                            color = VzOnSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                items(entries, key = { it.item.uri.toString() }) { entry ->
                    val uri = entry.item.uri.toString()
                    Box {
                        MediaThumb(
                            item = entry.item,
                            isSelected = uri in selected,
                            isFavorite = uri in favoritos,
                            showName = showName,
                            showFavoriteBadge = false,
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

                        if (entry.isReal) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.65f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "Original",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
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
            SelectionBottomBar(
                onDelete = { showDeleteDialog = true },
                onHide = { /* not used */ },
                onShare = { shareSelection() }
            )
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
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = if (item.isVideo) "video/*" else "image/*"
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
                    viewerOpen = false
                    onChanged()
                }
            )
        }
    }

    // ===== Diálogo: Excluir (Lixeira / Permanente) =====
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
                    TextButton(onClick = { deleteSelection() }) {
                        Text("Lixeira", color = VzSecondary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        )
    }
}