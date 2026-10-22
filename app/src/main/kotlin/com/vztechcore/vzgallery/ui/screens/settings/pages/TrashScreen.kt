package com.vztechcore.vzgallery.ui.screens.settings.pages

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
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
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vztechcore.vzgallery.data.AnimationMode
import com.vztechcore.vzgallery.data.ReloadBus
import com.vztechcore.vzgallery.data.SnackbarBus
import com.vztechcore.vzgallery.data.TrashItem
import com.vztechcore.vzgallery.data.TrashManager
import com.vztechcore.vzgallery.model.MediaItem
import com.vztechcore.vzgallery.ui.anim.bottomBarEnter
import com.vztechcore.vzgallery.ui.anim.bottomBarExit
import com.vztechcore.vzgallery.ui.screens.viewer.ViewerScreen
import com.vztechcore.vzgallery.ui.theme.VzBackground
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzOnSurfaceVariant
import com.vztechcore.vzgallery.ui.theme.VzPrimary
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import com.vztechcore.vzgallery.ui.theme.VzSurface
import com.vztechcore.vzgallery.ui.theme.VzSurfaceVariant
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun TrashScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()

    val items by TrashManager.items.collectAsState()

    LaunchedEffect(Unit) { TrashManager.reload() }

    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showEmptyDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var viewerOpen by remember { mutableStateOf(false) }
    var viewerIndex by remember { mutableStateOf(0) }
    val boundsCache = remember { mutableStateMapOf<String, Rect>() }

    val selectionMode = selectedIds.isNotEmpty()
    val allSelected = items.isNotEmpty() && selectedIds.size == items.size

    BackHandler {
        if (selectionMode) selectedIds = emptySet()
        else onBack()
    }

    fun restoreSelection() {
        val ids = selectedIds
        val count = ids.size
        scope.launch {
            val toRestore = TrashManager.items.value.filter { it.id in ids }
            toRestore.forEach { TrashManager.restore(it) }
            selectedIds = emptySet()
            ReloadBus.request()
            SnackbarBus.show(
                if (count == 1) "1 item restaurado"
                else "$count itens restaurados"
            )
        }
    }

    fun deleteSelectionPermanently() {
        val ids = selectedIds
        val count = ids.size
        scope.launch {
            val toDelete = TrashManager.items.value.filter { it.id in ids }
            toDelete.forEach { TrashManager.deletePermanently(it) }
            selectedIds = emptySet()
            SnackbarBus.show(
                if (count == 1) "1 item apagado"
                else "$count itens apagados"
            )
        }
        showDeleteDialog = false
    }

    fun emptyTrash() {
        scope.launch {
            val count = TrashManager.items.value.size
            TrashManager.emptyTrash()
            SnackbarBus.show(
                if (count == 1) "1 item apagado"
                else "$count itens apagados"
            )
        }
        showEmptyDialog = false
    }

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
                            ) { selectedIds = emptySet() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Fechar seleção",
                            tint = VzOnBackground
                        )
                    }

                    Text(
                        text = if (selectedIds.size == 1) "1 item selecionado"
                        else "${selectedIds.size} itens selecionados",
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
                                selectedIds = if (allSelected) {
                                    emptySet()
                                } else {
                                    items.map { it.id }.toSet()
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                            text = "Lixeira",
                            style = MaterialTheme.typography.displayLarge,
                            color = VzOnBackground,
                            fontSize = 28.sp
                        )
                    }

                    if (items.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { showEmptyDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = "Esvaziar lixeira",
                                tint = VzSecondary
                            )
                        }
                    }
                }
            }

            // ===== Aviso topo =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VzSurface)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Itens são apagados permanentemente após 24 horas.",
                    color = VzOnSurfaceVariant,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ===== Conteúdo =====
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
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = VzSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "Lixeira vazia",
                        style = MaterialTheme.typography.titleLarge,
                        color = VzOnBackground
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Itens excluídos aparecem aqui por 24h antes de serem apagados de vez.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VzOnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    state = rememberLazyGridState(),
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
                    items(items, key = { it.id }) { item ->
                        TrashThumb(
                            item = item,
                            isSelected = item.id in selectedIds,
                            onClick = {
                                if (selectionMode) {
                                    selectedIds = if (item.id in selectedIds) selectedIds - item.id
                                    else selectedIds + item.id
                                } else {
                                    val idx = items.indexOfFirst { it.id == item.id }
                                    if (idx >= 0) {
                                        viewerIndex = idx
                                        viewerOpen = true
                                    }
                                }
                            },
                            onLongClick = { selectedIds = selectedIds + item.id },
                            onPositioned = { rect -> boundsCache[item.id] = rect }
                        )
                    }
                }
            }
        }

        // ===== Bottom bar de seleção =====
        AnimatedVisibility(
            visible = selectionMode,
            enter = bottomBarEnter(AnimationMode.ANIM_1),
            exit = bottomBarExit(AnimationMode.ANIM_1),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            Icons.Filled.DeleteForever,
                            null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Excluir",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = { restoreSelection() },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VzSecondary,
                            contentColor = VzBackground
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            Icons.Filled.Restore,
                            null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Restaurar",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // ===== Viewer =====
        if (viewerOpen && items.isNotEmpty()) {
            val mediaItems: List<MediaItem> = items.map { trash ->
                MediaItem(
                    id = trash.id.hashCode().toLong(),
                    uri = Uri.fromFile(File(trash.trashFile)),
                    nome = trash.originalName,
                    dataAdicionada = trash.dateAdded,
                    isVideo = trash.isVideo,
                    tamanhoBytes = trash.sizeBytes
                )
            }

            ViewerScreen(
                items = mediaItems,
                startIndex = viewerIndex,
                getCurrentBounds = { media ->
                    val trash = items.firstOrNull { it.originalName == media.nome }
                    trash?.let { boundsCache[it.id] }
                },
                onClose = { idx ->
                    viewerOpen = false
                    viewerIndex = idx
                },
                onDelete = { media ->
                    val trash = items.firstOrNull { it.originalName == media.nome }
                    if (trash != null) {
                        scope.launch {
                            TrashManager.deletePermanently(trash)
                            SnackbarBus.show("Item apagado")
                        }
                    }
                    viewerOpen = false
                },
                onHide = { /* not used in trash mode */ },
                onShare = { /* not used in trash mode */ },
                trashMode = true,
                onRestore = { media ->
                    val trash = items.firstOrNull { it.originalName == media.nome }
                    if (trash != null) {
                        scope.launch {
                            TrashManager.restore(trash)
                            ReloadBus.request()
                            SnackbarBus.show("Item restaurado")
                        }
                    }
                    viewerOpen = false
                }
            )
        }
    }

    // ===== Diálogo: Excluir permanentemente (seleção) =====
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = VzSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = if (selectedIds.size == 1) "Excluir permanentemente?"
                    else "Excluir ${selectedIds.size} itens permanentemente?",
                    style = MaterialTheme.typography.titleLarge,
                    color = VzOnBackground
                )
            },
            text = {
                Text(
                    text = "Os itens selecionados serão apagados de vez. Não dá pra recuperar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VzOnSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = { deleteSelectionPermanently() }) {
                    Text("Excluir", color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar", color = VzOnSurfaceVariant)
                }
            }
        )
    }

    // ===== Diálogo: Esvaziar lixeira =====
    if (showEmptyDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyDialog = false },
            containerColor = VzSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Esvaziar lixeira?",
                    style = MaterialTheme.typography.titleLarge,
                    color = VzOnBackground
                )
            },
            text = {
                Text(
                    text = "Todos os ${items.size} itens serão apagados permanentemente. Essa ação não pode ser desfeita.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VzOnSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = { emptyTrash() }) {
                    Text("Esvaziar lixeira", color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyDialog = false }) {
                    Text("Cancelar", color = VzOnSurfaceVariant)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrashThumb(
    item: TrashItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPositioned: (Rect) -> Unit
) {
    // 🔥 Escala + borda igual ao MediaThumb do Home
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.90f else 1f,
        animationSpec = tween(140),
        label = "scale"
    )

    val radius = RoundedCornerShape(10.dp)
    val borderWidth = if (isSelected) 3.dp else 0.dp

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .onGloballyPositioned { coords -> onPositioned(coords.boundsInWindow()) }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .border(
                width = borderWidth,
                color = if (isSelected) VzPrimary else Color.Transparent,
                shape = radius
            )
            .clip(radius)
            .combinedClickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(radius)
                .background(VzSurfaceVariant)
        ) {
            AsyncImage(
                model = File(item.trashFile),
                contentDescription = item.originalName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Badge do tempo restante (canto inferior esquerdo)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = formatTimeLeft(item),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatTimeLeft(item: TrashItem): String {
    val ms = item.remainingMs()
    if (ms <= 0) return "0min"
    val hours = ms / (60 * 60 * 1000L)
    if (hours >= 1) return "${hours}h"
    val minutes = ms / (60 * 1000L)
    return "${minutes}min"
}