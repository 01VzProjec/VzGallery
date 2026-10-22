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
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOff
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vztechcore.vzgallery.data.Prefs
import com.vztechcore.vzgallery.ui.screens.settings.SectionCard
import com.vztechcore.vzgallery.ui.screens.settings.SettingsTopBar
import com.vztechcore.vzgallery.ui.theme.VzBackground
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzOnSurfaceVariant
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import com.vztechcore.vzgallery.ui.theme.VzSurface
import com.vztechcore.vzgallery.ui.theme.VzSurfaceVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Real folder browser, starting at /storage/emulated/0/.
 * Check a parent folder → hides it AND all subfolders.
 */
@Composable
fun FolderPickerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { Prefs(context) }

    val hiddenFolders by prefs.hiddenFolders.collectAsState(initial = emptySet())

    val rootPath = remember { "/storage/emulated/0" }
    var currentPath by remember { mutableStateOf(rootPath) }
    var folders by remember { mutableStateOf<List<FolderEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    // Load subfolders from current directory
    LaunchedEffect(currentPath) {
        loading = true
        folders = withContext(Dispatchers.IO) { listSubfolders(currentPath) }
        loading = false
    }

    // Back button: go up one level, or exit if at root
    fun goUp() {
        if (currentPath == rootPath) {
            onBack()
        } else {
            val parent = File(currentPath).parent ?: rootPath
            currentPath = if (parent.length < rootPath.length) rootPath else parent
        }
    }

    BackHandler { goUp() }

    // Path relative to root (/storage/emulated/0)
    // Ex: "DCIM/Camera/" — this is what Prefs stores
    fun pathToRelative(absolute: String): String {
        val rel = absolute.removePrefix(rootPath).trimStart('/')
        return if (rel.isEmpty()) "" else "$rel/"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VzBackground)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        SettingsTopBar(title = "Escolher pasta", onBack = { goUp() })

        // Breadcrumb
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Caminho atual",
                style = MaterialTheme.typography.bodyMedium,
                color = VzOnSurfaceVariant,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = currentPath.removePrefix("/storage/emulated/0").let {
                    if (it.isEmpty()) "storage/emulated/0/" else "storage/emulated/0$it"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = VzSecondary,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Marque uma pasta pra ocultar ela e TODAS as subpastas.",
                style = MaterialTheme.typography.bodyMedium,
                color = VzOnSurfaceVariant,
                fontSize = 11.sp
            )
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(VzSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = null,
                            tint = VzSecondary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Nenhuma subpasta aqui",
                        style = MaterialTheme.typography.titleLarge,
                        color = VzOnBackground
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                SectionCard {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(folders, key = { it.absolutePath }) { entry ->
                            val relativePath = pathToRelative(entry.absolutePath)
                            val isHidden = relativePath.isNotEmpty() &&
                                    relativePath in hiddenFolders

                            FolderRow(
                                entry = entry,
                                isHidden = isHidden,
                                onToggle = { checked ->
                                    if (relativePath.isEmpty()) return@FolderRow
                                    scope.launch {
                                        prefs.toggleHiddenFolder(relativePath, checked)
                                    }
                                },
                                onEnter = {
                                    currentPath = entry.absolutePath
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun FolderRow(
    entry: FolderEntry,
    isHidden: Boolean,
    onToggle: (Boolean) -> Unit,
    onEnter: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onEnter() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isHidden) VzSecondary.copy(alpha = 0.15f) else VzSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isHidden) Icons.Outlined.FolderOff else Icons.Outlined.Folder,
                contentDescription = null,
                tint = if (isHidden) VzSecondary else VzSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyLarge,
                color = VzOnBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isHidden) "Oculta" else "Toque pra abrir",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isHidden) VzSecondary else VzOnSurfaceVariant,
                fontSize = 11.sp
            )
        }
        Spacer(Modifier.width(6.dp))

        Checkbox(
            checked = isHidden,
            onCheckedChange = { onToggle(it) },
            colors = CheckboxDefaults.colors(
                checkedColor = VzSecondary,
                uncheckedColor = VzOnSurfaceVariant,
                checkmarkColor = VzOnBackground
            )
        )

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Entrar",
            tint = VzOnSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ============================================================
// FILE UTILS
// ============================================================
data class FolderEntry(
    val name: String,
    val absolutePath: String
)

private fun listSubfolders(path: String): List<FolderEntry> {
    return try {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        dir.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith(".") }
            ?.sortedBy { it.name.lowercase() }
            ?.map { FolderEntry(name = it.name, absolutePath = it.absolutePath) }
            ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }
}