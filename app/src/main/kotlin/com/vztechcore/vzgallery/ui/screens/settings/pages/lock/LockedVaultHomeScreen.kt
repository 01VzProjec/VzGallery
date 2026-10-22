package com.vztechcore.vzgallery.ui.screens.settings.pages.lock

import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vztechcore.vzgallery.R
import com.vztechcore.vzgallery.data.AnimationMode
import com.vztechcore.vzgallery.data.GridSize
import com.vztechcore.vzgallery.data.LockRepository
import com.vztechcore.vzgallery.data.LockType
import com.vztechcore.vzgallery.data.Prefs
import com.vztechcore.vzgallery.data.SnackbarBus
import com.vztechcore.vzgallery.model.MediaItem
import com.vztechcore.vzgallery.ui.anim.bottomBarEnter
import com.vztechcore.vzgallery.ui.anim.bottomBarExit
import com.vztechcore.vzgallery.ui.anim.menuEnter
import com.vztechcore.vzgallery.ui.anim.menuExit
import com.vztechcore.vzgallery.ui.components.MediaThumb
import com.vztechcore.vzgallery.ui.components.SelectionTopBar
import com.vztechcore.vzgallery.ui.theme.VzBackground
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzOnSurfaceVariant
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import com.vztechcore.vzgallery.ui.theme.VzSurface
import com.vztechcore.vzgallery.ui.theme.VzSurfaceVariant
import kotlinx.coroutines.launch

private val TOP_BAR_HEIGHT = 72.dp

private fun nextGridSize(current: GridSize): GridSize? = when (current) {
    GridSize.LARGE -> GridSize.NORMAL
    GridSize.NORMAL -> GridSize.SMALL
    GridSize.SMALL -> GridSize.COMPACT
    GridSize.COMPACT -> null
}

private fun previousGridSize(current: GridSize): GridSize? = when (current) {
    GridSize.COMPACT -> GridSize.SMALL
    GridSize.SMALL -> GridSize.NORMAL
    GridSize.NORMAL -> GridSize.LARGE
    GridSize.LARGE -> null
}

@Composable
fun LockedVaultHomeScreen(
    onBack: () -> Unit,
    onResetLock: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { Prefs(context) }

    val gridSize by prefs.gridSize.collectAsState(initial = GridSize.NORMAL)
    val showName by prefs.showName.collectAsState(initial = false)
    val animationMode by prefs.animationMode.collectAsState(initial = AnimationMode.ANIM_1)
    val lockType by prefs.lockType.collectAsState(initial = LockType.PIN)
    val biometricEnabled by prefs.lockBiometricEnabled.collectAsState(initial = false)

    var loading by remember { mutableStateOf(true) }
    var lockedFiles by remember { mutableStateOf<List<LockRepository.LockedFile>>(emptyList()) }
    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var uriMap by remember { mutableStateOf<Map<String, Uri>>(emptyMap()) }

    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    val selectionMode = selected.isNotEmpty()
    val allSelected = lockedFiles.isNotEmpty() && selected.size == lockedFiles.size

    var menuOpen by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var showChangeLock by remember { mutableStateOf(false) }
    var showAddBiometric by remember { mutableStateOf(false) }

    val gridState = rememberLazyGridState()
    var viewerOpen by remember { mutableStateOf(false) }
    var viewerIndex by remember { mutableStateOf(0) }
    val boundsCache = remember { mutableStateMapOf<String, Rect>() }

    suspend fun loadVault() {
        loading = true
        boundsCache.clear()
        val files = LockRepository.listLocked(context)
        lockedFiles = files

        val uriTmp = mutableMapOf<String, Uri>()
        val itemTmp = mutableListOf<MediaItem>()
        for (file in files) {
            val uri = LockRepository.getDecryptedUri(context, file)
            if (uri != null) {
                uriTmp[file.id] = uri
                itemTmp.add(
                    MediaItem(
                        id = file.id.hashCode().toLong(),
                        uri = uri,
                        nome = file.originalName,
                        dataAdicionada = (file.dateTakenMs ?: file.lockedAt) / 1000L,
                        isVideo = file.isVideo,
                        duracaoMs = 0L,
                        relativePath = file.originalPath,
                        tamanhoBytes = file.sizeBytes
                    )
                )
            }
        }
        uriMap = uriTmp
        mediaItems = itemTmp
        loading = false
    }

    LaunchedEffect(Unit) { loadVault() }

    BackHandler(enabled = selectionMode) { selected = emptySet() }
    BackHandler(enabled = viewerOpen) { viewerOpen = false }
    BackHandler(enabled = menuOpen) { menuOpen = false }
    BackHandler(enabled = !selectionMode && !viewerOpen && !menuOpen) { onBack() }

    suspend fun restoreItem(file: LockRepository.LockedFile): Boolean {
        return LockRepository.unlockFile(context, file).isSuccess
    }

    fun restoreSelection() {
        val toRestore = lockedFiles.filter { it.id in selected }
        val total = toRestore.size
        scope.launch {
            var ok = 0
            for (file in toRestore) if (restoreItem(file)) ok++
            selected = emptySet()
            loadVault()
            val text = when {
                ok == 0 -> "Não foi possível restaurar"
                ok == total -> if (ok == 1) "1 arquivo restaurado" else "$ok arquivos restaurados"
                else -> "$ok de $total restaurados"
            }
            SnackbarBus.show(text)
        }
    }

    fun deleteSelection() {
        val toDelete = lockedFiles.filter { it.id in selected }
        val total = toDelete.size
        scope.launch {
            var ok = 0
            for (file in toDelete) if (LockRepository.deleteLocked(context, file)) ok++
            selected = emptySet()
            loadVault()
            val text = when {
                ok == 0 -> "Nenhum arquivo apagado"
                ok == total -> if (ok == 1) "1 arquivo apagado" else "$ok arquivos apagados"
                else -> "$ok de $total apagados"
            }
            SnackbarBus.show(text)
        }
        showDeleteDialog = false
    }

    fun shareSelection() {
        val uris = selected.mapNotNull { uriMap[it] }
        if (uris.isEmpty()) return
        val hasVideo = lockedFiles.any { it.id in selected && it.isVideo }
        val hasImage = lockedFiles.any { it.id in selected && !it.isVideo }
        val type = when {
            hasVideo && hasImage -> "*/*"
            hasVideo -> "video/*"
            else -> "image/*"
        }
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND_MULTIPLE).apply {
            this.type = type
            putParcelableArrayListExtra(
                android.content.Intent.EXTRA_STREAM,
                ArrayList<android.os.Parcelable>(uris)
            )
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(android.content.Intent.createChooser(intent, "Enviar via"))
        } catch (_: Exception) {
            SnackbarBus.show("Erro ao compartilhar")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ========== CONTEÚDO PRINCIPAL ==========
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (menuOpen && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        Modifier.blur(10.dp)
                    else Modifier
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(VzBackground)
                    .windowInsetsPadding(WindowInsets.systemBars)
            ) {
                when {
                    loading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = VzSecondary)
                    }

                    lockedFiles.isEmpty() -> EmptyVaultState()

                    else -> key(gridSize) {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(gridSize.columns),
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    var accumulated = 1f
                                    awaitEachGesture {
                                        awaitFirstDown(
                                            requireUnconsumed = false,
                                            pass = PointerEventPass.Initial
                                        )
                                        do {
                                            val event = awaitPointerEvent(PointerEventPass.Initial)
                                            val pressedCount = event.changes.count { it.pressed }
                                            if (pressedCount >= 2) {
                                                val zoom = event.calculateZoom()
                                                if (zoom != 1f && zoom.isFinite() && zoom > 0f) {
                                                    accumulated *= zoom
                                                    if (accumulated >= 1.25f) {
                                                        accumulated = 1f
                                                        val prev = previousGridSize(gridSize)
                                                        if (prev != null) {
                                                            scope.launch { prefs.setGridSize(prev) }
                                                        }
                                                    } else if (accumulated <= 0.8f) {
                                                        accumulated = 1f
                                                        val next = nextGridSize(gridSize)
                                                        if (next != null) {
                                                            scope.launch { prefs.setGridSize(next) }
                                                        }
                                                    }
                                                    event.changes.forEach {
                                                        if (it.pressed) it.consume()
                                                    }
                                                }
                                            }
                                        } while (event.changes.any { it.pressed })
                                        accumulated = 1f
                                    }
                                },
                            contentPadding = PaddingValues(
                                start = 8.dp,
                                end = 8.dp,
                                top = TOP_BAR_HEIGHT + 8.dp,
                                bottom = if (selectionMode) 110.dp else 8.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item(
                                key = "vault_stats_bar",
                                span = { GridItemSpan(maxLineSpan) }
                            ) {
                                Box(modifier = Modifier.padding(start = 4.dp, end = 4.dp)) {
                                    LockedVaultStatsBar(items = lockedFiles)
                                }
                            }

                            items(mediaItems, key = { it.uri.toString() }) { item ->
                                val uriKey = item.uri.toString()
                                val file = lockedFiles.firstOrNull {
                                    uriMap[it.id] == item.uri
                                }
                                val isSelected = file?.id in selected

                                MediaThumb(
                                    item = item,
                                    isSelected = isSelected,
                                    isFavorite = false,
                                    showName = showName,
                                    showFavoriteBadge = false,
                                    showCloudOffBadge = false,
                                    onClick = {
                                        if (selectionMode) {
                                            if (file != null) {
                                                selected = if (file.id in selected)
                                                    selected - file.id
                                                else selected + file.id
                                            }
                                        } else {
                                            val idx = mediaItems.indexOf(item)
                                            if (idx >= 0) {
                                                viewerIndex = idx
                                                viewerOpen = true
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        if (file != null) selected = selected + file.id
                                    },
                                    onPositioned = { rect -> boundsCache[uriKey] = rect }
                                )
                            }
                        }
                    }
                }

                // ========== TOP BAR (seleção ou normal) ==========
                if (selectionMode) {
                    SelectionTopBar(
                        selectedCount = selected.size,
                        allSelected = allSelected,
                        onClose = { selected = emptySet() },
                        onSelectAll = {
                            val all = lockedFiles.map { it.id }.toSet()
                            selected = if (selected.size == all.size) emptySet() else all
                        },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                } else {
                    VaultTopBar(
                        onBack = onBack,
                        onHelp = { showHelpDialog = true },
                        onMenuClick = { menuOpen = true },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }

        // ========== BOTTOM BAR DE SELEÇÃO (animada, fora do blur) ==========
        AnimatedVisibility(
            visible = selectionMode,
            enter = bottomBarEnter(animationMode),
            exit = bottomBarExit(animationMode),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            VaultSelectionBottomBar(
                onShare = { shareSelection() },
                onRestore = { restoreSelection() },
                onDelete = { showDeleteDialog = true }
            )
        }

        // ========== VIEWER ==========
        if (viewerOpen && mediaItems.isNotEmpty()) {
            LockedVaultViewerScreen(
                items = lockedFiles,
                decryptedUris = uriMap,
                startIndex = viewerIndex,
                onClose = { finalIdx ->
                    viewerOpen = false
                    viewerIndex = finalIdx
                },
                onRestore = { file ->
                    scope.launch {
                        val ok = restoreItem(file)
                        if (ok) {
                            SnackbarBus.show("Arquivo restaurado")
                            viewerOpen = false
                            loadVault()
                        } else {
                            SnackbarBus.show("Erro ao restaurar")
                        }
                    }
                },
                onDelete = { file ->
                    scope.launch {
                        LockRepository.deleteLocked(context, file)
                        SnackbarBus.show("Arquivo apagado")
                        viewerOpen = false
                        loadVault()
                    }
                },
                onShare = { uri, isVideo ->
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = if (isVideo) "video/*" else "image/*"
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        context.startActivity(android.content.Intent.createChooser(intent, "Enviar via"))
                    } catch (_: Exception) {
                        SnackbarBus.show("Erro ao compartilhar")
                    }
                }
            )
        }

        // ========== OVERLAY PRETO (menu aberto) ==========
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

        // ========== SUBMENU ANIMADO ==========
        AnimatedVisibility(
            visible = menuOpen,
            enter = menuEnter(animationMode),
            exit = menuExit(animationMode),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 60.dp, end = 12.dp)
        ) {
            VaultOptionsMenu(
                lockType = lockType,
                biometricEnabled = biometricEnabled,
                onChangePassword = {
                    menuOpen = false
                    showChangeLock = true
                },
                onAddBiometric = {
                    menuOpen = false
                    showAddBiometric = true
                },
                onBackup = {
                    menuOpen = false
                    SnackbarBus.show("Em breve: backup do cofre")
                },
                onDisable = {
                    menuOpen = false
                    showResetDialog = true
                }
            )
        }

        // ========== SUB-TELAS DO COFRE ==========
        if (showChangeLock) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(VzBackground)
            ) {
                ChangeLockScreen(
                    onBack = { showChangeLock = false },
                    onDone = {
                        showChangeLock = false
                        SnackbarBus.show("Proteção atualizada")
                    }
                )
            }
        }

        if (showAddBiometric) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(VzBackground)
            ) {
                AddBiometricScreen(
                    onBack = { showAddBiometric = false },
                    onDone = {
                        showAddBiometric = false
                        scope.launch {
                            prefs.setLockBiometricEnabled(true)
                            SnackbarBus.show("Digital ativada")
                        }
                    }
                )
            }
        }
    }

    // ========== DIALOGS ==========
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = VzSurface,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Desativar a proteção?", color = VzOnBackground) },
            text = {
                Text(
                    "Os arquivos trancados continuam intactos, mas a senha será removida. Você poderá criar uma nova depois.",
                    color = VzOnSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    onResetLock()
                }) {
                    Text("Desativar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancelar", color = VzSecondary)
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = VzSurface,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Excluir permanentemente?", color = VzOnBackground) },
            text = {
                Text(
                    "Os ${selected.size} arquivo(s) selecionado(s) serão apagados de vez. Não dá pra recuperar.",
                    color = VzOnSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = ::deleteSelection) {
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

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            containerColor = VzSurface,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Sobre a Área Trancada", color = VzOnBackground) },
            text = {
                Text(
                    "Os arquivos daqui estão protegidos e criptografados. Nenhum outro app consegue vê-los. " +
                            "Ao restaurar, eles voltam pra galeria do celular na data original. " +
                            "Ao excluir, são apagados permanentemente do cofre.",
                    color = VzOnSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Entendi", color = VzSecondary)
                }
            }
        )
    }
}

@Composable
private fun VaultTopBar(
    onBack: () -> Unit,
    onHelp: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        VzBackground,
                        VzBackground.copy(alpha = 0.85f),
                        VzBackground.copy(alpha = 0f)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                    tint = VzOnBackground,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Área Trancada",
                style = MaterialTheme.typography.displayLarge,
                color = VzOnBackground,
                fontSize = 24.sp
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onHelp() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_help_outline),
                    contentDescription = "Ajuda",
                    tint = VzOnBackground,
                    modifier = Modifier.size(24.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onMenuClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Mais opções",
                    tint = VzOnBackground,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyVaultState() {
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
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = VzSecondary,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Nenhum arquivo trancado",
            style = MaterialTheme.typography.titleLarge,
            color = VzOnBackground,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Mova fotos e vídeos da galeria principal pra cá. Eles ficarão protegidos e invisíveis pra outros apps.",
            color = VzOnSurfaceVariant,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}