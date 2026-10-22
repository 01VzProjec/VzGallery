package com.vztechcore.vzgallery.ui.screens.home

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.vztechcore.vzgallery.data.AnimationMode
import com.vztechcore.vzgallery.data.DataGrouper
import com.vztechcore.vzgallery.data.GridSize
import com.vztechcore.vzgallery.data.MediaActions
import com.vztechcore.vzgallery.data.MediaScanner
import com.vztechcore.vzgallery.data.Prefs
import com.vztechcore.vzgallery.data.ReloadBus
import com.vztechcore.vzgallery.data.SnackbarBus
import com.vztechcore.vzgallery.data.TrashManager
import com.vztechcore.vzgallery.model.MediaItem
import com.vztechcore.vzgallery.ui.anim.ScreenSlideDirection
import com.vztechcore.vzgallery.ui.anim.bottomBarEnter
import com.vztechcore.vzgallery.ui.anim.bottomBarExit
import com.vztechcore.vzgallery.ui.anim.menuEnter
import com.vztechcore.vzgallery.ui.anim.menuExit
import com.vztechcore.vzgallery.ui.anim.screenEnter
import com.vztechcore.vzgallery.ui.anim.screenExit
import com.vztechcore.vzgallery.ui.components.GroupSelectAllButton
import com.vztechcore.vzgallery.ui.components.MediaGridView
import com.vztechcore.vzgallery.ui.components.MoveToVaultDialogs
import com.vztechcore.vzgallery.ui.components.OptionsMenu
import com.vztechcore.vzgallery.ui.components.SelectionBottomBar
import com.vztechcore.vzgallery.ui.components.SelectionTopBar
import com.vztechcore.vzgallery.ui.components.VaultSetupFlow
import com.vztechcore.vzgallery.ui.screens.cleanup.CleanupScreen
import com.vztechcore.vzgallery.ui.screens.home.components.StatsBar
import com.vztechcore.vzgallery.ui.screens.library.LibraryScreen
import com.vztechcore.vzgallery.ui.screens.search.SearchScreen
import com.vztechcore.vzgallery.ui.screens.settings.SettingsScreen
import com.vztechcore.vzgallery.ui.screens.viewer.ViewerScreen
import com.vztechcore.vzgallery.ui.theme.VzBackground
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzOnSurfaceVariant
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import com.vztechcore.vzgallery.ui.theme.VzSurface
import kotlinx.coroutines.launch

private val TOP_BAR_HEIGHT = 72.dp

private enum class AppScreen { Gallery, Library, Settings, Search, Cleanup }

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scanner = remember { MediaScanner(context) }
    val prefs = remember { Prefs(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    val showSvg by prefs.showSvg.collectAsState(initial = false)
    val hiddenItems by prefs.hiddenItems.collectAsState(initial = emptySet())
    val hiddenFolders by prefs.hiddenFolders.collectAsState(initial = emptySet())
    val gridSize by prefs.gridSize.collectAsState(initial = GridSize.NORMAL)
    val autoReload by prefs.autoReload.collectAsState(initial = true)
    val animationMode by prefs.animationMode.collectAsState(initial = AnimationMode.ANIM_1)
    val showName by prefs.showName.collectAsState(initial = false)
    val showFavoriteBadge by prefs.showFavoriteBadge.collectAsState(initial = true)
    val favorites by prefs.favorites.collectAsState(initial = emptySet())
    val lockConfigured by prefs.lockConfigured.collectAsState(initial = false)

    val autoReloadState = rememberUpdatedState(autoReload)

    var loading by remember { mutableStateOf(true) }
    var allItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    val gridState = rememberLazyGridState()
    var menuOpen by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showHideDialog by remember { mutableStateOf(false) }
    var showVaultDialog by remember { mutableStateOf(false) }
    var showVaultSetup by remember { mutableStateOf(false) }
    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.Gallery) }

    val boundsCache = remember { mutableStateMapOf<String, Rect>() }
    var viewerOpen by remember { mutableStateOf(false) }
    var viewerIndex by remember { mutableStateOf(0) }

    val selectionMode = selected.isNotEmpty()
    var pendingScrollToTop by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        SnackbarBus.messages.collect { msg ->
            val result = snackbarHostState.showSnackbar(
                message = msg.text,
                actionLabel = msg.actionLabel,
                duration = if (msg.actionLabel != null) SnackbarDuration.Long
                else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) msg.onAction?.invoke()
        }
    }

    suspend fun reload() {
        boundsCache.clear()
        loading = true
        allItems = scanner.loadMedia()
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && autoReloadState.value) {
                pendingScrollToTop = true
                scope.launch { reload() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(pendingScrollToTop, viewerOpen, selectionMode) {
        if (pendingScrollToTop) {
            if (!viewerOpen && !selectionMode) gridState.scrollToItem(0)
            pendingScrollToTop = false
        }
    }

    val reloadTick by ReloadBus.trigger.collectAsState()
    LaunchedEffect(reloadTick) { if (reloadTick > 0) reload() }

    val groups = remember(allItems, showSvg, hiddenItems, hiddenFolders) {
        var filtered = if (showSvg) allItems
        else allItems.filterNot { MediaActions.isSvgFile(it) }
        filtered = filtered.filterNot { it.uri.toString() in hiddenItems }
        if (hiddenFolders.isNotEmpty()) {
            filtered = filtered.filterNot { item ->
                hiddenFolders.any { folder -> item.relativePath.startsWith(folder) }
            }
        }
        DataGrouper.group(filtered)
    }

    val visibleItems = remember(groups) { groups.flatMap { it.itens } }
    val allSelected = visibleItems.isNotEmpty() && selected.size == visibleItems.size

    BackHandler(enabled = selectionMode) { selected = emptySet() }

    fun deleteToTrash() {
        val toDelete = visibleItems.filter { it.uri.toString() in selected }
        val total = toDelete.size
        scope.launch {
            val moved = MediaActions.deleteToTrash(toDelete)
            if (moved.isNotEmpty()) {
                allItems = allItems.filterNot { it.uri.toString() in moved.keys }
            }
            selected = emptySet()
            val text = MediaActions.formatResult(moved.size, total, "excluído")
            if (moved.isNotEmpty()) {
                SnackbarBus.showWithAction(
                    text = text,
                    actionLabel = "Desfazer",
                    onAction = {
                        scope.launch {
                            moved.values.forEach { TrashManager.restore(it) }
                            reload()
                        }
                    }
                )
            } else {
                SnackbarBus.show(text)
            }
        }
        showDeleteDialog = false
    }

    fun deletePermanent() {
        val toDelete = visibleItems.filter { it.uri.toString() in selected }
        val total = toDelete.size
        scope.launch {
            val deleted = MediaActions.deletePermanent(context, toDelete)
            if (deleted.isNotEmpty()) {
                allItems = allItems.filterNot { it.uri.toString() in deleted }
            }
            selected = emptySet()
            SnackbarBus.show(MediaActions.formatResult(deleted.size, total, "apagado"))
        }
        showDeleteDialog = false
    }

    fun moveToVault() {
        val toMove = visibleItems.filter { it.uri.toString() in selected }
        val total = toMove.size
        if (total == 0) return
        scope.launch {
            val moved = MediaActions.moveToVault(context, toMove)
            if (moved.isNotEmpty()) {
                allItems = allItems.filterNot { it.uri.toString() in moved }
            }
            selected = emptySet()
            SnackbarBus.show(MediaActions.formatResult(moved.size, total, "movido pro cofre"))
        }
    }

    fun deleteSingle(item: MediaItem) {
        scope.launch {
            val moved = MediaActions.deleteToTrash(listOf(item))
            if (moved.isNotEmpty()) {
                allItems = allItems.filterNot { it.uri.toString() == item.uri.toString() }
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
        scope.launch {
            val deleted = MediaActions.deletePermanent(context, listOf(item))
            if (deleted.isNotEmpty()) {
                allItems = allItems.filterNot { it.uri.toString() == item.uri.toString() }
                SnackbarBus.show("Item apagado")
            }
        }
    }

    fun hideSelection() {
        val count = selected.size
        scope.launch {
            MediaActions.hide(prefs, selected)
            selected = emptySet()
            SnackbarBus.show(
                if (count == 1) "1 item ocultado" else "$count itens ocultados"
            )
        }
        showHideDialog = false
    }

    fun shareSelection() {
        val items = visibleItems.filter { it.uri.toString() in selected }
        MediaActions.share(context, items)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val dir = if (forward) ScreenSlideDirection.RIGHT else ScreenSlideDirection.LEFT
                screenEnter(animationMode, dir) togetherWith screenExit(animationMode, dir)
            },
            label = "screen_transition"
        ) { screen ->
            when (screen) {
                AppScreen.Library -> LibraryScreen(onBack = { currentScreen = AppScreen.Gallery })
                AppScreen.Settings -> SettingsScreen(onBack = { currentScreen = AppScreen.Gallery })
                AppScreen.Search -> SearchScreen(onBack = { currentScreen = AppScreen.Gallery })
                AppScreen.Cleanup -> CleanupScreen(onBack = { currentScreen = AppScreen.Gallery })
                AppScreen.Gallery -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(VzBackground)
                            .windowInsetsPadding(WindowInsets.systemBars)
                            .then(
                                if (menuOpen && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                    Modifier.blur(10.dp) else Modifier
                            )
                    ) {
                        MediaGridView(
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
                            onItemClick = { _, idx -> viewerIndex = idx; viewerOpen = true },
                            onItemLongClick = { item -> selected = selected + item.uri.toString() },
                            onPositioned = { uri, rect -> boundsCache[uri] = rect },
                            contentTopPadding = TOP_BAR_HEIGHT + 8.dp,
                            headerContent = {
                                StatsBar(
                                    allItems = allItems,
                                    hidden = hiddenItems,
                                    hiddenFolders = hiddenFolders,
                                    showSvg = showSvg
                                )
                            },
                            sectionHeaderTrailing = { _, groupUris ->
                                GroupSelectAllButton(
                                    groupUris = groupUris,
                                    selected = selected,
                                    onSelectedChange = { selected = it }
                                )
                            }
                        )

                        if (selectionMode) {
                            SelectionTopBar(
                                selectedCount = selected.size,
                                allSelected = allSelected,
                                onClose = { selected = emptySet() },
                                onSelectAll = {
                                    selected = if (allSelected) emptySet()
                                    else visibleItems.map { it.uri.toString() }.toSet()
                                },
                                modifier = Modifier.align(Alignment.TopCenter)
                            )
                        } else {
                            HomeTopBar(
                                onSearchClick = { currentScreen = AppScreen.Search },
                                onMenuClick = { menuOpen = !menuOpen },
                                modifier = Modifier.align(Alignment.TopCenter)
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = if (selectionMode) 96.dp else 16.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = VzSurface,
                contentColor = VzOnBackground,
                actionColor = VzSecondary,
                shape = RoundedCornerShape(12.dp)
            )
        }

        if (menuOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { menuOpen = false }
            )
        }

        AnimatedVisibility(
            visible = menuOpen,
            enter = menuEnter(animationMode),
            exit = menuExit(animationMode),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 60.dp, end = 12.dp)
        ) {
            OptionsMenu(
                onFreeUpSpace = { menuOpen = false; currentScreen = AppScreen.Cleanup },
                onLibrary = { menuOpen = false; currentScreen = AppScreen.Library },
                onSettings = { menuOpen = false; currentScreen = AppScreen.Settings }
            )
        }

        AnimatedVisibility(
            visible = selectionMode && currentScreen == AppScreen.Gallery,
            enter = bottomBarEnter(animationMode),
            exit = bottomBarExit(animationMode),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            SelectionBottomBar(
                onDelete = { showDeleteDialog = true },
                onHide = { showHideDialog = true },
                onShare = { shareSelection() },
                onProtect = { showVaultDialog = true }
            )
        }

        if (viewerOpen && visibleItems.isNotEmpty()) {
            ViewerScreen(
                items = visibleItems,
                startIndex = viewerIndex,
                getCurrentBounds = { item -> boundsCache[item.uri.toString()] },
                onClose = { finalIndex ->
                    viewerOpen = false
                    viewerIndex = finalIndex
                    scope.launch {
                        val target = visibleItems.getOrNull(finalIndex)?.uri?.toString() ?: return@launch
                        var gridIdx = 0
                        var found = false
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
                        val sampleH = info.visibleItemsInfo
                            .firstOrNull { !it.key.toString().startsWith("header_") }
                            ?.size?.height ?: (viewportH / 4)
                        val offset = ((viewportH - sampleH) / 2).coerceAtLeast(0)
                        gridState.scrollToItem(gridIdx, offset)
                    }
                },
                onDelete = { item -> deleteSingle(item) },
                onHide = { item ->
                    scope.launch {
                        MediaActions.hide(prefs, setOf(item.uri.toString()))
                        SnackbarBus.show("Item ocultado")
                    }
                },
                onShare = { item -> MediaActions.share(context, listOf(item)) },
                onDeletePermanently = { item -> deleteSinglePermanent(item) }
            )
        }
    }

    if (showDeleteDialog) {
        DeleteOptionsDialog(
            count = selected.size,
            onCancel = { showDeleteDialog = false },
            onTrash = { deleteToTrash() },
            onPermanent = { deletePermanent() }
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
                TextButton(onClick = { showHideDialog = false }) {
                    Text("Cancelar", color = VzOnSurfaceVariant)
                }
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
private fun HomeTopBar(
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(VzBackground, VzBackground.copy(alpha = 0.85f), VzBackground.copy(alpha = 0f))
                )
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Fotos", style = MaterialTheme.typography.displayLarge, color = VzOnBackground)
            Row(verticalAlignment = Alignment.CenterVertically) {
                HomeIconButton(icon = Icons.Filled.Search, description = "Pesquisar", onClick = onSearchClick)
                HomeIconButton(icon = Icons.Filled.MoreVert, description = "Mais opções", onClick = onMenuClick)
            }
        }
    }
}

@Composable
private fun HomeIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = VzOnBackground, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun DeleteOptionsDialog(
    count: Int,
    onCancel: () -> Unit,
    onTrash: () -> Unit,
    onPermanent: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = VzSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                if (count == 1) "Excluir 1 item?" else "Excluir $count itens?",
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
                Text(
                    "O item sai do celular agora, mas fica guardado dentro do VzGallery pelas próximas 24 horas. Você pode restaurar a qualquer momento.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VzOnSurfaceVariant,
                    fontSize = 12.sp
                )
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
            TextButton(onClick = onPermanent) {
                Text("Excluir", color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onCancel) { Text("Cancelar", color = VzOnSurfaceVariant) }
                TextButton(onClick = onTrash) { Text("Lixeira", color = VzSecondary, fontWeight = FontWeight.SemiBold) }
            }
        }
    )
}