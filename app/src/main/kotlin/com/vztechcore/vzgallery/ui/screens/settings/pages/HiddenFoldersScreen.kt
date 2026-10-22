package com.vztechcore.vzgallery.ui.screens.settings.pages

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.SubdirectoryArrowRight
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vztechcore.vzgallery.data.MediaScanner
import com.vztechcore.vzgallery.data.Prefs
import com.vztechcore.vzgallery.model.FolderInfo
import com.vztechcore.vzgallery.ui.screens.settings.SettingsTopBar
import com.vztechcore.vzgallery.ui.theme.VzBackground
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzOnSurfaceVariant
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import com.vztechcore.vzgallery.ui.theme.VzSurface
import com.vztechcore.vzgallery.ui.theme.VzSurfaceVariant
import kotlinx.coroutines.launch

private enum class HiddenFoldersPage { List, Picker }

@Composable
fun HiddenFoldersScreen(onBack: () -> Unit) {
    var currentPage by remember { mutableStateOf(HiddenFoldersPage.List) }

    when (currentPage) {
        HiddenFoldersPage.List -> FolderListPage(
            onBack = onBack,
            onOpenPicker = { currentPage = HiddenFoldersPage.Picker }
        )
        HiddenFoldersPage.Picker -> FolderPickerScreen(
            onBack = { currentPage = HiddenFoldersPage.List }
        )
    }
}

/**
 * Returns the parent folder that is hiding this entry, if any.
 * Ex: if "DCIM/" is in hidden, and the entry is "DCIM/Camera/", returns "DCIM/".
 * If the entry itself is checked, returns null (it's not inheritance).
 */
private fun findInheritedFrom(
    relativePath: String,
    hiddenFolders: Set<String>
): String? {
    if (relativePath in hiddenFolders) return null
    return hiddenFolders.firstOrNull { parent ->
        relativePath.startsWith(parent) && relativePath != parent
    }
}

@Composable
private fun FolderListPage(
    onBack: () -> Unit,
    onOpenPicker: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { Prefs(context) }
    val scanner = remember { MediaScanner(context) }

    val hiddenFolders by prefs.hiddenFolders.collectAsState(initial = emptySet())

    var loading by remember { mutableStateOf(true) }
    var folders by remember { mutableStateOf<List<FolderInfo>>(emptyList()) }

    BackHandler { onBack() }

    LaunchedEffect(Unit) {
        folders = scanner.listFolders()
        loading = false
    }

    LaunchedEffect(hiddenFolders) {
        folders = scanner.listFolders()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VzBackground)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        SettingsTopBar(title = "Pastas ocultas", onBack = onBack)

        // ===== "PICK FROM ROOT" BUTTON =====
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(VzSurface)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onOpenPicker() }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(VzSecondary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = VzSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Escolher pela raiz do sistema",
                        style = MaterialTheme.typography.bodyLarge,
                        color = VzOnBackground,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Navegue por /storage/emulated/0/ e escolha a pasta",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VzOnSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = VzOnSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = VzSecondary, trackColor = VzSurfaceVariant)
            }
        } else if (folders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhuma pasta com mídia encontrada",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VzOnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Marque as pastas cujo conteúdo NÃO deve aparecer na galeria. " +
                            "Subpastas de uma pasta oculta ficam marcadas automaticamente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VzOnSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(folders, key = { it.relativePath }) { folder ->
                        val isDirectlyHidden = folder.relativePath in hiddenFolders
                        val inheritedFrom = findInheritedFrom(folder.relativePath, hiddenFolders)
                        val isInherited = inheritedFrom != null
                        val showAsChecked = isDirectlyHidden || isInherited

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(VzSurface)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    if (isInherited) return@clickable
                                    scope.launch {
                                        prefs.toggleHiddenFolder(folder.relativePath, !isDirectlyHidden)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isInherited) VzSurfaceVariant.copy(alpha = 0.5f)
                                        else VzSurfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isInherited)
                                        Icons.Outlined.SubdirectoryArrowRight
                                    else
                                        Icons.Outlined.FolderOff,
                                    contentDescription = null,
                                    tint = if (isInherited)
                                        VzOnSurfaceVariant
                                    else
                                        VzSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = folder.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isInherited)
                                        VzOnSurfaceVariant
                                    else
                                        VzOnBackground,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.height(2.dp))
                                if (isInherited) {
                                    Text(
                                        text = "Oculta por: ${inheritedFrom!!.trimEnd('/')}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = VzSecondary.copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                } else {
                                    Text(
                                        text = "${folder.fullPath} • ${folder.itemCount} ${if (folder.itemCount == 1) "item" else "itens"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = VzOnSurfaceVariant,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Checkbox(
                                checked = showAsChecked,
                                onCheckedChange = { checked ->
                                    if (isInherited) return@Checkbox
                                    scope.launch {
                                        prefs.toggleHiddenFolder(folder.relativePath, checked)
                                    }
                                },
                                enabled = !isInherited,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = if (isInherited)
                                        VzOnSurfaceVariant.copy(alpha = 0.5f)
                                    else
                                        VzSecondary,
                                    uncheckedColor = VzOnSurfaceVariant,
                                    checkmarkColor = VzOnBackground,
                                    disabledCheckedColor = VzOnSurfaceVariant.copy(alpha = 0.5f),
                                    disabledUncheckedColor = VzOnSurfaceVariant.copy(alpha = 0.4f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}