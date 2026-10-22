package com.vztechcore.vzgallery.ui.screens.settings

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vztechcore.vzgallery.data.*
import com.vztechcore.vzgallery.data.LockRepository
import com.vztechcore.vzgallery.data.LockType
import com.vztechcore.vzgallery.model.MediaItem
import com.vztechcore.vzgallery.ui.screens.settings.pages.lock.UnlockScreen
import com.vztechcore.vzgallery.ui.screens.settings.pages.lock.LockedVaultHomeScreen
import com.vztechcore.vzgallery.ui.screens.cleanup.CleanupScreen
import com.vztechcore.vzgallery.ui.screens.settings.pages.lock.BiometricSetupScreen
import com.vztechcore.vzgallery.ui.screens.settings.pages.lock.LockedVaultSettingsScreen
import com.vztechcore.vzgallery.ui.screens.settings.pages.lock.PinSetupScreen
import com.vztechcore.vzgallery.ui.screens.settings.pages.lock.PasswordSetupScreen
import com.vztechcore.vzgallery.ui.screens.settings.pages.*
import com.vztechcore.vzgallery.ui.screens.stats.StatsScreen
import com.vztechcore.vzgallery.ui.screens.wrapped.WrappedScreen
import com.vztechcore.vzgallery.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SettingsPage {
    Main,
    HiddenItems,
    HiddenFolders,
    About,
    Stats,
    Wrapped,
    Trash,
    Cleanup,
    LockedVault,
    PinSetup,
    PasswordSetup,
    BiometricSetup,
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {

    var currentPage by rememberSaveable { mutableStateOf(SettingsPage.Main) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { Prefs(context) }

    when (currentPage) {
        SettingsPage.Main -> MainSettingsPage(
            onBack = onBack,
            onOpenHiddenItems = { currentPage = SettingsPage.HiddenItems },
            onOpenHiddenFolders = { currentPage = SettingsPage.HiddenFolders },
            onOpenAbout = { currentPage = SettingsPage.About },
            onOpenStats = { currentPage = SettingsPage.Stats },
            onOpenWrapped = { currentPage = SettingsPage.Wrapped },
            onOpenTrash = { currentPage = SettingsPage.Trash },
            onOpenCleanup = { currentPage = SettingsPage.Cleanup },
            onOpenLockedVault = { currentPage = SettingsPage.LockedVault }
        )
        SettingsPage.HiddenItems -> AllHiddenScreen(
            initialMode = HiddenMode.ALL,
            onBack = { currentPage = SettingsPage.Main }
        )
        SettingsPage.HiddenFolders -> HiddenFoldersScreen(
            onBack = { currentPage = SettingsPage.Main }
        )
        SettingsPage.About -> AboutScreen(onBack = { currentPage = SettingsPage.Main })
        SettingsPage.Stats -> StatsScreen(onBack = { currentPage = SettingsPage.Main })
        SettingsPage.Wrapped -> WrappedScreen(onBack = { currentPage = SettingsPage.Main })
        SettingsPage.Trash -> TrashScreen(onBack = { currentPage = SettingsPage.Main })
        SettingsPage.Cleanup -> CleanupScreen(onBack = { currentPage = SettingsPage.Main })

        SettingsPage.LockedVault -> {
            val lockConfigured by prefs.lockConfigured.collectAsState(initial = false)
            var unlocked by remember { mutableStateOf(false) }

            if (!lockConfigured) {
                LockedVaultSettingsScreen(
                    onBack = { currentPage = SettingsPage.Main },
                    onChoosePin = { currentPage = SettingsPage.PinSetup },
                    onChoosePassword = { currentPage = SettingsPage.PasswordSetup }
                )
            } else if (!unlocked) {
                UnlockScreen(
                    onUnlocked = { unlocked = true },
                    onBack = { currentPage = SettingsPage.Main }
                )
            } else {
                LockedVaultHomeScreen(
                    onBack = {
                        unlocked = false
                        currentPage = SettingsPage.Main
                    },
                    onResetLock = {
                        scope.launch {
                            // Preserva arquivos — só desliga a proteção
                            prefs.clearLock()
                            unlocked = false
                            currentPage = SettingsPage.Main
                        }
                    }
                )
            }
        }

        SettingsPage.PinSetup -> PinSetupScreen(
            onBack = { currentPage = SettingsPage.LockedVault },
            onPinSet = { pin ->
                scope.launch {
                    val salt = LockRepository.generateSalt()
                    val hash = LockRepository.hashPassword(pin, salt)
                    prefs.saveLock(LockType.PIN, hash, salt)
                    currentPage = SettingsPage.BiometricSetup
                }
            }
        )

        SettingsPage.PasswordSetup -> PasswordSetupScreen(
            onBack = { currentPage = SettingsPage.LockedVault },
            onPasswordSet = { password ->
                scope.launch {
                    val salt = LockRepository.generateSalt()
                    val hash = LockRepository.hashPassword(password, salt)
                    prefs.saveLock(LockType.PASSWORD, hash, salt)
                    currentPage = SettingsPage.BiometricSetup
                }
            }
        )

        SettingsPage.BiometricSetup -> BiometricSetupScreen(
            onBack = { currentPage = SettingsPage.LockedVault },
            onDone = { biometricEnabled ->
                scope.launch {
                    prefs.setLockBiometricEnabled(biometricEnabled)
                    currentPage = SettingsPage.Main
                }
            }
        )
    }
}

@Composable
private fun MainSettingsPage(
    onBack: () -> Unit,
    onOpenHiddenItems: () -> Unit,
    onOpenHiddenFolders: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenWrapped: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenCleanup: () -> Unit,
    onOpenLockedVault: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { Prefs(context) }

    val showSvg by prefs.showSvg.collectAsState(initial = false)
    val hiddenItems by prefs.hiddenItems.collectAsState(initial = emptySet())
    val hiddenFolders by prefs.hiddenFolders.collectAsState(initial = emptySet())
    val themeMode by prefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val gridSize by prefs.gridSize.collectAsState(initial = GridSize.NORMAL)
    val autoReload by prefs.autoReload.collectAsState(initial = true)
    val animationMode by prefs.animationMode.collectAsState(initial = AnimationMode.ANIM_1)
    val showName by prefs.showName.collectAsState(initial = false)
    val showFavoriteBadge by prefs.showFavoriteBadge.collectAsState(initial = true)

    val trashItems by TrashManager.items.collectAsState()

    var allMedia by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var cacheSize by remember { mutableStateOf(0L) }
    var showResetDialog by remember { mutableStateOf(false) }

    var themeMenuOpen by remember { mutableStateOf(false) }
    var gridMenuOpen by remember { mutableStateOf(false) }
    var animationMenuOpen by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    LaunchedEffect(Unit) {
        cacheSize = withContext(Dispatchers.IO) { computeCacheSize(context) }
        TrashManager.reload()
        allMedia = withContext(Dispatchers.IO) { MediaScanner(context).loadMedia() }
    }

    val totalHiddenCount = remember(hiddenItems, hiddenFolders, allMedia, showSvg) {
        fun isSvgItem(item: MediaItem): Boolean {
            val ext = item.nome.substringAfterLast('.', "").lowercase()
            return ext == "svg" || ext == "svgz"
        }
        val allByUri = allMedia.associateBy { it.uri.toString() }
        val manualFiltered = hiddenItems.filter { uri ->
            if (showSvg) return@filter true
            val item = allByUri[uri]
            if (item != null) {
                !isSvgItem(item)
            } else {
                val ext = uri.substringAfterLast('.', "").lowercase()
                ext != "svg" && ext != "svgz"
            }
        }
        val folderFiltered = if (hiddenFolders.isEmpty()) {
            emptyList<String>()
        } else {
            allMedia
                .filter { item ->
                    val path = item.relativePath
                    hiddenFolders.any { folder -> path.startsWith(folder) }
                }
                .filter { item -> showSvg || !isSvgItem(item) }
                .map { it.uri.toString() }
        }
        (manualFiltered + folderFiltered).toSet().size
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VzBackground)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        SettingsTopBar(title = "Configurações", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {

            // ============================================================
            // 1. APARÊNCIA
            // ============================================================
            SectionTitle("Aparência")
            SectionCard {
                Box {
                    NavigationRow(
                        icon = Icons.Outlined.Palette,
                        title = "Tema",
                        subtitle = "${themeMode.label} • em breve aplicado",
                        onClick = { themeMenuOpen = true }
                    )
                    DropdownMenu(
                        expanded = themeMenuOpen,
                        onDismissRequest = { themeMenuOpen = false },
                        modifier = Modifier.background(VzSurface)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = mode.label,
                                        color = if (mode == themeMode) VzSecondary else VzOnBackground,
                                        fontWeight = if (mode == themeMode) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    scope.launch { prefs.setThemeMode(mode) }
                                    themeMenuOpen = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Box {
                    NavigationRow(
                        icon = Icons.Outlined.Animation,
                        title = "Estilo de animação",
                        subtitle = animationMode.label,
                        onClick = { animationMenuOpen = true }
                    )
                    DropdownMenu(
                        expanded = animationMenuOpen,
                        onDismissRequest = { animationMenuOpen = false },
                        modifier = Modifier.background(VzSurface)
                    ) {
                        AnimationMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Column(modifier = Modifier.width(240.dp)) {
                                        Text(
                                            text = mode.label,
                                            color = if (mode == animationMode) VzSecondary else VzOnBackground,
                                            fontWeight = if (mode == animationMode) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = mode.description,
                                            color = VzOnSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                },
                                onClick = {
                                    scope.launch { prefs.setAnimationMode(mode) }
                                    animationMenuOpen = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ============================================================
            // 2. GALERIA
            // ============================================================
            SectionTitle("Galeria")
            SectionCard {
                Box {
                    NavigationRow(
                        icon = Icons.Outlined.GridView,
                        title = "Tamanho da grade",
                        subtitle = "${gridSize.columns} colunas • ${gridSize.label}",
                        onClick = { gridMenuOpen = true }
                    )
                    DropdownMenu(
                        expanded = gridMenuOpen,
                        onDismissRequest = { gridMenuOpen = false },
                        modifier = Modifier.background(VzSurface)
                    ) {
                        GridSize.entries.forEach { size ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${size.columns} colunas • ${size.label}",
                                        color = if (size == gridSize) VzSecondary else VzOnBackground,
                                        fontWeight = if (size == gridSize) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    scope.launch { prefs.setGridSize(size) }
                                    gridMenuOpen = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                SwitchRow(
                    title = "Mostrar nome do arquivo",
                    subtitle = "Exibe o nome sobreposto na miniatura",
                    checked = showName,
                    onCheckedChange = { value ->
                        scope.launch { prefs.setShowName(value) }
                    }
                )
                Spacer(Modifier.height(4.dp))
                SwitchRow(
                    title = "Indicador de favorito",
                    subtitle = "Mostra um coração vermelho nas fotos favoritadas",
                    checked = showFavoriteBadge,
                    onCheckedChange = { value ->
                        scope.launch { prefs.setShowFavoriteBadge(value) }
                    }
                )
            }

            Spacer(Modifier.height(20.dp))

            // ============================================================
            // 3. CONTEÚDO OCULTO
            // ============================================================
            SectionTitle("Conteúdo oculto")
            SectionCard {
                SwitchRow(
                    title = "Exibir arquivos SVG",
                    subtitle = "Exibe arquivos .svg na galeria principal",
                    checked = showSvg,
                    onCheckedChange = { value ->
                        scope.launch { prefs.setShowSvg(value) }
                    }
                )
                Spacer(Modifier.height(4.dp))
                NavigationRow(
                    icon = Icons.Outlined.VisibilityOff,
                    title = "Gerenciar itens ocultos",
                    subtitle = if (totalHiddenCount == 0) "Nenhum item oculto"
                    else "$totalHiddenCount ${if (totalHiddenCount == 1) "item" else "itens"} oculto${if (totalHiddenCount == 1) "" else "s"}",
                    onClick = onOpenHiddenItems
                )
                Spacer(Modifier.height(4.dp))
                NavigationRow(
                    icon = Icons.Outlined.FolderOff,
                    title = "Ocultar pastas",
                    subtitle = if (hiddenFolders.isEmpty()) "Nenhuma pasta oculta"
                    else "${hiddenFolders.size} ${if (hiddenFolders.size == 1) "pasta" else "pastas"} oculta${if (hiddenFolders.size == 1) "" else "s"}",
                    onClick = onOpenHiddenFolders
                )
            }

            Spacer(Modifier.height(20.dp))

            // ============================================================
            // 4. PRIVACIDADE
            // ============================================================
            SectionTitle("Privacidade")
            SectionCard {
                NavigationRow(
                    icon = Icons.Outlined.Lock,
                    title = "Área Trancada",
                    subtitle = "Fotos protegidas por senha",
                    onClick = onOpenLockedVault
                )
            }

            Spacer(Modifier.height(20.dp))

            // ============================================================
            // 5. ARMAZENAMENTO
            // ============================================================
            SectionTitle("Armazenamento")
            SectionCard {
                NavigationRow(
                    icon = Icons.Outlined.DeleteSweep,
                    title = "Liberar espaço",
                    subtitle = "Fotos duplicadas, vídeos grandes e screenshots",
                    onClick = onOpenCleanup
                )
                Spacer(Modifier.height(4.dp))
                NavigationRow(
                    icon = Icons.Outlined.Delete,
                    title = "Lixeira",
                    subtitle = if (trashItems.isEmpty()) "Vazia"
                    else "${trashItems.size} ${if (trashItems.size == 1) "item" else "itens"} • ${formatBytes(TrashManager.totalSize())}",
                    onClick = onOpenTrash
                )
                Spacer(Modifier.height(4.dp))
                NavigationRow(
                    icon = Icons.Outlined.CleaningServices,
                    title = "Limpar cache",
                    subtitle = if (cacheSize > 0) "Liberar ${formatBytes(cacheSize)}" else "Nada para limpar",
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) { clearCache(context) }
                            cacheSize = 0L
                        }
                    }
                )
            }

            Spacer(Modifier.height(20.dp))

            // ============================================================
            // 6. GERAL
            // ============================================================
            SectionTitle("Geral")
            SectionCard {
                SwitchRow(
                    title = "Reler arquivos automaticamente",
                    subtitle = "Atualiza a galeria ao voltar pro app",
                    checked = autoReload,
                    onCheckedChange = { value ->
                        scope.launch { prefs.setAutoReload(value) }
                    }
                )
                Spacer(Modifier.height(4.dp))
                NavigationRow(
                    icon = Icons.Outlined.Refresh,
                    title = "Reler todos os arquivos",
                    subtitle = "Força o app a re-escanear a galeria",
                    onClick = { ReloadBus.request() }
                )
            }

            Spacer(Modifier.height(20.dp))

            // ============================================================
            // 7. DESCOBERTAS
            // ============================================================
            SectionTitle("Descobertas")
            SectionCard {
                NavigationRow(
                    icon = Icons.Outlined.BarChart,
                    title = "Estatísticas",
                    subtitle = "Formatos, meses, maiores arquivos e mais",
                    onClick = onOpenStats
                )
                Spacer(Modifier.height(4.dp))
                NavigationRow(
                    icon = Icons.Outlined.AutoAwesome,
                    title = "VzWrapped",
                    subtitle = "Sua retrospectiva da galeria",
                    onClick = onOpenWrapped
                )
            }

            Spacer(Modifier.height(20.dp))

            // ============================================================
            // 8. AVANÇADO
            // ============================================================
            SectionTitle("Avançado")
            SectionCard {
                NavigationRow(
                    icon = Icons.Outlined.RestartAlt,
                    title = "Resetar preferências",
                    subtitle = "Apaga todas as configurações do app",
                    onClick = { showResetDialog = true }
                )
            }

            Spacer(Modifier.height(20.dp))

            // ============================================================
            // 9. SOBRE
            // ============================================================
            SectionTitle("Sobre")
            SectionCard {
                NavigationRow(
                    icon = Icons.Outlined.Info,
                    title = "Sobre o VzGallery",
                    subtitle = "Versão, autor e informações do projeto",
                    onClick = onOpenAbout
                )
            }

            Spacer(Modifier.height(32.dp))
            Text(
                text = "VzGallery • VzTechCore",
                style = MaterialTheme.typography.bodyMedium,
                color = VzOnSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                textAlign = TextAlign.Center
            )
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = VzSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Resetar preferências?",
                    style = MaterialTheme.typography.titleLarge,
                    color = VzOnBackground
                )
            },
            text = {
                Text(
                    "Isso apaga: filtros, ocultos, favoritos, capas customizadas, tema, grade, animação, etc. " +
                            "Os arquivos em si NÃO são afetados.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VzOnSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        prefs.resetAll()
                        showResetDialog = false
                        onBack()
                    }
                }) {
                    Text("Resetar", color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancelar", color = VzOnSurfaceVariant)
                }
            }
        )
    }
}

// ============================================================
// COMPONENTES REUTILIZÁVEIS
// ============================================================

@Composable
fun SettingsTopBar(title: String, onBack: () -> Unit) {
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
            text = title,
            style = MaterialTheme.typography.displayLarge,
            color = VzOnBackground,
            fontSize = 28.sp
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = VzSecondary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        fontSize = 13.sp
    )
}

@Composable
fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(VzSurface)
            .padding(vertical = 4.dp),
        content = content
    )
}

@Composable
fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = VzOnBackground)
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = VzOnSurfaceVariant,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = VzOnBackground,
                checkedTrackColor = VzSecondary,
                uncheckedThumbColor = VzOnSurfaceVariant,
                uncheckedTrackColor = VzSurfaceVariant
            )
        )
    }
}

@Composable
fun NavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(VzSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = VzSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = VzOnBackground
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = VzOnSurfaceVariant,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = VzOnSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ============================================================
// UTILS
// ============================================================

private fun computeCacheSize(context: android.content.Context): Long {
    return try {
        val dir = context.cacheDir ?: return 0L
        dir.walkBottomUp().filter { it.isFile }.map { it.length() }.sum()
    } catch (_: Exception) { 0L }
}

private fun clearCache(context: android.content.Context) {
    try {
        context.cacheDir?.listFiles()?.forEach { file ->
            try { file.deleteRecursively() } catch (_: Exception) {}
        }
    } catch (_: Exception) {}
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.2f GB".format(gb)
}
