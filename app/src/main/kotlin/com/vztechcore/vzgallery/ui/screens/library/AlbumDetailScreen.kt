package com.vztechcore.vzgallery.ui.screens.library

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CalendarViewDay
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.vztechcore.vzgallery.data.AlbumType
import com.vztechcore.vzgallery.data.AnimationMode
import com.vztechcore.vzgallery.data.DataGrouper
import com.vztechcore.vzgallery.data.GridSize
import com.vztechcore.vzgallery.data.MediaActions
import com.vztechcore.vzgallery.data.MediaScanner
import com.vztechcore.vzgallery.data.Prefs
import com.vztechcore.vzgallery.data.ReloadBus
import com.vztechcore.vzgallery.data.SnackbarBus
import com.vztechcore.vzgallery.data.SortMode
import com.vztechcore.vzgallery.data.TrashManager
import com.vztechcore.vzgallery.model.GroupData
import com.vztechcore.vzgallery.model.MediaItem
import com.vztechcore.vzgallery.ui.anim.bottomBarEnter
import com.vztechcore.vzgallery.ui.anim.bottomBarExit
import com.vztechcore.vzgallery.ui.anim.menuEnter
import com.vztechcore.vzgallery.ui.anim.menuExit
import com.vztechcore.vzgallery.ui.components.GroupSelectAllButton
import com.vztechcore.vzgallery.ui.components.MediaGridView
import com.vztechcore.vzgallery.ui.components.MoveToVaultDialogs
import com.vztechcore.vzgallery.ui.components.SelectionBottomBar
import com.vztechcore.vzgallery.ui.components.SelectionTopBar
import com.vztechcore.vzgallery.ui.components.VaultSetupFlow
import com.vztechcore.vzgallery.ui.screens.viewer.ViewerScreen
import com.vztechcore.vzgallery.ui.theme.VzBackground
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzOnSurfaceVariant
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import com.vztechcore.vzgallery.ui.theme.VzSurface
import com.vztechcore.vzgallery.ui.theme.VzSurfaceVariant
import kotlinx.coroutines.launch

private val TOP_BAR_HEIGHT = 64.dp

@Composable
fun AlbumDetailScreen(
    album: AlbumType,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scanner = remember { MediaScanner(context) }
    val prefs = remember { Prefs(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    val showSvg by prefs.showSvg.collectAsState(initial = false)
    val hiddenItems by prefs.hiddenItems.collectAsState(initial = emptySet())
    val hiddenFolders by prefs.hiddenFolders.collectAsState(initial = emptySet())
    val favorites by prefs.favorites.collectAsState(initial = emptySet())
    val orderMode by prefs.orderMode(album).collectAsState(initial = SortMode.RECENT_FIRST)
    val hideDate by prefs.hideDate(album).collectAsState(initial = false)
    val gridSize by prefs.gridSize.collectAsState(initial = GridSize.NORMAL)
    val autoReload by prefs.autoReload.collectAsState(initial = true)
    val animationMode by prefs.animationMode.collectAsState(initial = AnimationMode.ANIM_1)
    val showName by prefs.showName.collectAsState(initial = false)
    val showFavoriteBadge by prefs.showFavoriteBadge.collectAsState(initial = true)
    val lockConfigured by prefs.lockConfigured.collectAsState(initial = false)

    val autoReloadState = rememberUpdatedState(autoReload)

    var loading by remember { mutableStateOf(true) }
    var allItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    val gridState = rememberLazyGridState()

    var menuOpen by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var coverPickMode by remember { mutableStateOf(false) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showHideDialog by remember { mutableStateOf(false) }
    var showVaultDialog by remember { mutableStateOf(false) }
    var showVaultSetup by remember { mutableStateOf(false) }

    val boundsCache = remember { mutableStateMapOf<String, Rect>() }
    var viewerOpen by remember { mutableStateOf(false) }
    var viewerIndex by remember { mutableStateOf(0) }

    val selectionMode = selected.isNotEmpty()

    suspend fun reload() {
        loading = true
        allItems = scanner.loadMedia()
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && autoReloadState.value) {
                scope.launch { reload() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val reloadTick by ReloadBus.trigger.collectAsState()
    LaunchedEffect(reloadTick) { if (reloadTick > 0) reload() }

    val albumItems = remember(allItems, album, showSvg, hiddenItems, hiddenFolders, favorites) {
        var filtered = if (showSvg) allItems
        else allItems.filterNot { MediaActions.isSvgFile(it) }
        filtered = filtered.filterNot { it.uri.toString() in hiddenItems }
        if (hiddenFolders.isNotEmpty()) {
            filtered = filtered.filterNot { item ->
                hiddenFolders.any { folder -> item.relativePath.startsWith(folder) }
            }
        }
        when (album) {
            AlbumType.ALL_PHOTOS -> filtered.filter { !it.isVideo }
            AlbumType.FAVORITES -> filtered.filter { it.uri.toString() in favorites }
            AlbumType.CAMERA -> filtered.filter {
                val path = it.relativePath.lowercase()
                path.contains("dcim") || path.contains("camera")
            }
            AlbumType.ALL_VIDEOS -> filtered.filter { it.isVideo }
        }
    }

    val sortedItems = remember(albumItems, orderMode) {
        when (orderMode) {
            SortMode.RECENT_FIRST -> albumItems.sortedByDescending { it.dataAdicionada }
            SortMode.OLDEST_FIRST -> albumItems.sortedBy { it.dataAdicionada }
            SortMode.SIZE_ASC -> albumItems.sortedBy { it.tamanhoBytes }
            SortMode.SIZE_DESC -> albumItems.sortedByDescending { it.tamanhoBytes }
            SortMode.NAME_ASC -> albumItems.sortedBy { it.nome.lowercase() }
        }
    }

    val groups: List<GroupData> = remember(sortedItems, hideDate) {
        if (hideDate) listOf(GroupData("Todos os itens", sortedItems))
        else DataGrouper.group(sortedItems)
    }

    val visibleItems = remember(groups) { groups.flatMap { it.itens } }
    val allSelected = visibleItems.isNotEmpty() && selected.size == visibleItems.size

    BackHandler {
        when {
            menuOpen -> menuOpen = false
            coverPickMode -> coverPickMode = false
            selectionMode -> selected = emptySet()
            else -> onBack()
        }
    }

    fun hasFullAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager()
        else true

    fun openFullAccessSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))
                )
            } catch (_: Exception) {
                try {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                } catch (_: Exception) {}
            }
        }
    }

    fun deleteToTrash() {
        if (!hasFullAccess()) { openFullAccessSettings(); return }
        val toDelete = visibleItems.filter { it.uri.toString() in selected }
        scope.launch {
            val moved = MediaActions.deleteToTrash(toDelete)
            selected = emptySet()
            if (moved.isNotEmpty()) {
                SnackbarBus.showWithAction(
                    text = MediaActions.formatResult(moved.size, toDelete.size, "excluído"),
                    actionLabel = "Desfazer",
                    onAction = {
                        scope.launch {
                            moved.values.forEach { TrashManager.restore(it) }
                            reload()
                        }
                    }
                )
            }
        }
        showDeleteDialog = false
    }

    fun deletePermanent() {
        if (!hasFullAccess()) { openFullAccessSettings(); return }
        val toDelete = visibleItems.filter { it.uri.toString() in selected }
        scope.launch {
            val deleted = MediaActions.deletePermanent(context, toDelete)
            selected = emptySet()
            if (deleted.isNotEmpty()) reload()
            SnackbarBus.show(MediaActions.formatResult(deleted.size, toDelete.size, "apagado"))
        }
        showDeleteDialog = false
    }

    fun moveToVault() {
        val toMove = visibleItems.filter { it.uri.toString() in selected }
        val total = toMove.size
        if (total == 0) return
        scope.launch {
            val moved = MediaActions.moveToVault(context, toMove)
            selected = emptySet()
            if (moved.isNotEmpty()) reload()
            SnackbarBus.show(MediaActions.formatResult(moved.size, total, "movido pro cofre"))
        }
    }

    fun deleteSingle(item: MediaItem) {
        if (!hasFullAccess()) { openFullAccessSettings(); return }
        scope.launch {
            val moved = MediaActions.deleteToTrash(listOf(item))
            if (moved.isNotEmpty()) {
                SnackbarBus.showWithAction(
                    text = "Item excluído",
                    actionLabel = "Desfazer",
                    onAction = {
                        scope.launch {
                            moved.values.forEach { TrashManager.restore(it) }
                            reload()
                        }
                    }
                )
            }
        }
    }

    fun deleteSinglePermanent(item: MediaItem) {
        if (!hasFullAccess()) { openFullAccessSettings(); return }
        scope.launch {
            val deleted = MediaActions.deletePermanent(context, listOf(item))
            if (deleted.isNotEmpty()) {
                SnackbarBus.show("Item apagado")
                reload()
            }
        }
    }

    fun hideSelection() {
        val count = selected.size
        scope.launch {
            MediaActions.hide(prefs, selected)
            selected = emptySet()
            SnackbarBus.show(if (count == 1) "1 item ocultado" else "$count itens ocultados")
        }
        showHideDialog = false
    }

    Box(modifier = Modifier.fillMaxSize().background(VzBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .then(
                    if (menuOpen && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(10.dp) else Modifier
                )
        ) {
            if (selectionMode) {
                SelectionTopBar(
                    selectedCount = selected.size,
                    allSelected = allSelected,
                    onClose = { selected = emptySet() },
                    onSelectAll = {
                        selected = if (allSelected) emptySet()
                        else visibleItems.map { it.uri.toString() }.toSet()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                AlbumTopBar(album = album, onBack = onBack, onMenuClick = { menuOpen = !menuOpen })
            }

            MediaGridView(
                modifier = Modifier.weight(1f),
                groups = groups,
                visibleItems = visibleItems,
                loading = loading,
                selected = selected,
                onSelectedChange = { selected = it },
                selectionMode = selectionMode,
                allSelected = allSelected,
                gridSize = gridSize,
                onGridSizeChange = { newGrid -> scope.launch { prefs.setGridSize(newGrid) } },
                gridState = gridState,
                showName = showName,
                showFavoriteBadge = showFavoriteBadge,
                favorites = favorites,
                hiddenItems = hiddenItems,
                onItemClick = { item, idx ->
                    if (coverPickMode) {
                        scope.launch {
                            prefs.setCustomCover(album, item.uri.toString())
                            coverPickMode = false
                        }
                    } else {
                        viewerIndex = idx
                        viewerOpen = true
                    }
                },
                onItemLongClick = { item ->
                    if (!coverPickMode) selected = selected + item.uri.toString()
                },
                onPositioned = { uri, rect -> boundsCache[uri] = rect },
                contentEndPadding = 20.dp,
                sectionHeaderTrailing = { _, groupUris ->
                    GroupSelectAllButton(
                        groupUris = groupUris,
                        selected = selected,
                        onSelectedChange = { selected = it }
                    )
                },
                emptyContent = { EmptyAlbumContent(album) }
            )
        }

        if (coverPickMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(top = 72.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(VzSecondary)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    "Toque numa foto/vídeo pra ser a capa do álbum",
                    color = VzBackground,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (menuOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { menuOpen = false }
            )
        }

        AnimatedVisibility(
            visible = menuOpen,
            enter = menuEnter(animationMode),
            exit = menuExit(animationMode),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(top = 52.dp, end = 12.dp)
        ) {
            AlbumOptionsMenu(
                onSetCover = { menuOpen = false; coverPickMode = true },
                currentSort = orderMode,
                onSortChange = { mode -> scope.launch { prefs.setOrderMode(album, mode) } },
                hideDate = hideDate,
                onHideDateChange = { value -> scope.launch { prefs.setHideDate(album, value) } }
            )
        }

        AnimatedVisibility(
            visible = selectionMode,
            enter = bottomBarEnter(animationMode),
            exit = bottomBarExit(animationMode),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            SelectionBottomBar(
                onDelete = { showDeleteDialog = true },
                onHide = { showHideDialog = true },
                onShare = {
                    val items = visibleItems.filter { it.uri.toString() in selected }
                    MediaActions.share(context, items)
                },
                onProtect = { showVaultDialog = true }
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
                    scope.launch {
                        val target = visibleItems.getOrNull(idx)?.uri?.toString() ?: return@launch
                        var gridIdx = 0; var found = false
                        for (group in groups) {
                            gridIdx++
                            for (item in group.itens) {
                                if (item.uri.toString() == target) { found = true; break }
                                gridIdx++
                            }
                            if (found) break
                        }
                        if (!found) return@launch
                        val info = gridState.layoutInfo
                        if (info.visibleItemsInfo.any { it.index == gridIdx }) return@launch
                        val viewportH = info.viewportSize.height
                        val sampleH = info.visibleItemsInfo.firstOrNull { !it.key.toString().startsWith("header_") }?.size?.height ?: (viewportH / 4)
                        gridState.scrollToItem(gridIdx, ((viewportH - sampleH) / 2).coerceAtLeast(0))
                    }
                },
                onDelete = { item -> deleteSingle(item); viewerOpen = false },
                onHide = { item ->
                    scope.launch {
                        MediaActions.hide(prefs, setOf(item.uri.toString()))
                        SnackbarBus.show("Item ocultado")
                    }
                    viewerOpen = false
                },
                onShare = { item -> MediaActions.share(context, listOf(item)) },
                onDeletePermanently = { item -> deleteSinglePermanent(item); viewerOpen = false }
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
                    if (selected.size == 1) "Excluir 1 item?" else "Excluir ${selected.size} itens?",
                    style = MaterialTheme.typography.titleLarge,
                    color = VzOnBackground
                )
            },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.RestoreFromTrash, null, tint = VzSecondary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Lixeira", color = VzSecondary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("O item sai do celular agora, mas fica guardado dentro do VzGallery pelas próximas 24 horas.", style = MaterialTheme.typography.bodyMedium, color = VzOnSurfaceVariant, fontSize = 12.sp)
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.DeleteForever, null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Excluir permanentemente", color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("O item é apagado de vez. Não dá pra recuperar.", style = MaterialTheme.typography.bodyMedium, color = VzOnSurfaceVariant, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { deletePermanent() }) {
                    Text("Excluir", color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar", color = VzOnSurfaceVariant) }
                    TextButton(onClick = { deleteToTrash() }) { Text("Lixeira", color = VzSecondary, fontWeight = FontWeight.SemiBold) }
                }
            }
        )
    }

    if (showHideDialog) {
        AlertDialog(
            onDismissRequest = { showHideDialog = false },
            containerColor = VzSurface,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Ocultar ${if (selected.size == 1) "1 item" else "${selected.size} itens"}?", color = VzOnBackground) },
            text = { Text("Os itens selecionados NÃO serão excluídos. Eles ficarão ocultos da lista principal.", color = VzOnSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = { hideSelection() }) {
                    Text("Ocultar", color = VzSecondary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showHideDialog = false }) { Text("Cancelar", color = VzOnSurfaceVariant) }
            }
        )
    }

    if (showVaultDialog) {
        MoveToVaultDialogs(
            lockConfigured = lockConfigured,
            selectedCount = selected.size,
            onDismiss = { showVaultDialog = false },
            onConfirm = { showVaultDialog = false; moveToVault() },
            onGoToSetup = { showVaultDialog = false; showVaultSetup = true }
        )
    }

    if (showVaultSetup) {
        Box(modifier = Modifier.fillMaxSize().background(VzBackground)) {
            VaultSetupFlow(
                onComplete = { showVaultSetup = false; moveToVault() },
                onCancel = { showVaultSetup = false }
            )
        }
    }
}

@Composable
private fun AlbumTopBar(album: AlbumType, onBack: () -> Unit, onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(TOP_BAR_HEIGHT).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = VzOnBackground)
            }
            Spacer(Modifier.width(8.dp))
            Text(album.title, style = MaterialTheme.typography.titleLarge, color = VzOnBackground, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(
            modifier = Modifier.size(42.dp).clip(CircleShape)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onMenuClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.MoreVert, "Mais opções", tint = VzOnBackground)
        }
    }
}

@Composable
private fun AlbumOptionsMenu(
    onSetCover: () -> Unit,
    currentSort: SortMode,
    onSortChange: (SortMode) -> Unit,
    hideDate: Boolean,
    onHideDateChange: (Boolean) -> Unit
) {
    var sortMenuOpen by remember { mutableStateOf(false) }
    Column(modifier = Modifier.width(240.dp).clip(RoundedCornerShape(16.dp)).background(VzSurface).padding(vertical = 6.dp)) {
        AlbumMenuRow(Icons.Outlined.Image, "Alterar capa do álbum", onClick = onSetCover)
        Box {
            AlbumMenuRow(Icons.Outlined.Sort, "Ordenar por", subtitle = currentSort.label, onClick = { sortMenuOpen = true })
            DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }, modifier = Modifier.background(VzSurface)) {
                SortMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.label, color = if (mode == currentSort) VzSecondary else VzOnBackground, fontWeight = if (mode == currentSort) FontWeight.Bold else FontWeight.Normal) },
                        onClick = { onSortChange(mode); sortMenuOpen = false }
                    )
                }
            }
        }
        AlbumMenuRow(
            Icons.Outlined.CalendarViewDay,
            if (hideDate) "Mostrar data" else "Ocultar data",
            subtitle = if (hideDate) "Itens sem agrupamento" else "Agrupa por data",
            onClick = { onHideDateChange(!hideDate) }
        )
    }
}

@Composable
private fun AlbumMenuRow(icon: ImageVector, label: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = VzSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = VzOnBackground, fontSize = 14.sp)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = VzOnSurfaceVariant, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun EmptyAlbumContent(album: AlbumType) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.size(96.dp).clip(CircleShape).background(VzSurfaceVariant), contentAlignment = Alignment.Center) {
            Icon(album.icon, null, tint = VzSecondary, modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Nada por aqui", style = MaterialTheme.typography.titleLarge, color = VzOnBackground)
        Spacer(Modifier.height(6.dp))
        Text("Nenhum item encontrado neste álbum.", style = MaterialTheme.typography.bodyMedium, color = VzOnSurfaceVariant)
    }
}