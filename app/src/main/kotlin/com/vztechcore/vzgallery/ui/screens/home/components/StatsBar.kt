package com.vztechcore.vzgallery.ui.screens.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vztechcore.vzgallery.model.MediaItem
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzOnSurfaceVariant
import com.vztechcore.vzgallery.ui.theme.VzSecondary

private fun isSvg(item: MediaItem): Boolean {
    val ext = item.nome.substringAfterLast('.', "").lowercase()
    return ext == "svg" || ext == "svgz"
}

@Composable
fun StatsBar(
    allItems: List<MediaItem>,
    hidden: Set<String>,
    hiddenFolders: Set<String>,
    showSvg: Boolean = false
) {
    // SVG não conta como foto (a menos que showSvg esteja ativo)
    val countableItems = remember(allItems, showSvg) {
        if (showSvg) allItems else allItems.filterNot { isSvg(it) }
    }

    // Um item é oculto se:
    // 1. Tá no set de ocultos manuais OU
    // 2. Tá dentro de uma pasta oculta
    fun isHiddenItem(item: MediaItem): Boolean {
        if (item.uri.toString() in hidden) return true
        if (hiddenFolders.isEmpty()) return false
        val path = item.relativePath
        return hiddenFolders.any { folder -> path.startsWith(folder) }
    }

    val totalPhotos = countableItems.count { !it.isVideo }
    val totalVideos = countableItems.count { it.isVideo }
    val totalBytes = countableItems.sumOf { it.tamanhoBytes }

    val visibleList = countableItems.filterNot { isHiddenItem(it) }
    val hiddenList = countableItems.filter { isHiddenItem(it) }

    val visiblePhotos = visibleList.count { !it.isVideo }
    val visibleVideos = visibleList.count { it.isVideo }

    val hiddenPhotos = hiddenList.count { !it.isVideo }
    val hiddenVideos = hiddenList.count { it.isVideo }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 28.dp)
    ) {
        Text(
            text = "$totalPhotos ${if (totalPhotos == 1) "foto" else "fotos"} · $totalVideos ${if (totalVideos == 1) "vídeo" else "vídeos"}",
            style = MaterialTheme.typography.titleLarge,
            color = VzOnBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = formatBytes(totalBytes),
            style = MaterialTheme.typography.bodyLarge,
            color = VzSecondary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Principal  ·  $visiblePhotos ${if (visiblePhotos == 1) "foto" else "fotos"}, $visibleVideos ${if (visibleVideos == 1) "vídeo" else "vídeos"}",
            style = MaterialTheme.typography.bodyMedium,
            color = VzOnBackground.copy(alpha = 0.85f),
            fontSize = 12.sp
        )

        if (hiddenList.isNotEmpty()) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = "Ocultos  ·  $hiddenPhotos ${if (hiddenPhotos == 1) "foto" else "fotos"}, $hiddenVideos ${if (hiddenVideos == 1) "vídeo" else "vídeos"}",
                style = MaterialTheme.typography.bodyMedium,
                color = VzOnSurfaceVariant,
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "by Vtzin x · VzTechCore",
                style = MaterialTheme.typography.bodyMedium,
                color = VzOnSurfaceVariant.copy(alpha = 0.55f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.0f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.2f GB".format(gb)
}