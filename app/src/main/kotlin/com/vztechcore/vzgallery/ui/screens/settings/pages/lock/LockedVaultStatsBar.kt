package com.vztechcore.vzgallery.ui.screens.settings.pages.lock

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
import com.vztechcore.vzgallery.data.LockRepository
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzOnSurfaceVariant
import com.vztechcore.vzgallery.ui.theme.VzSecondary

@Composable
fun LockedVaultStatsBar(
    items: List<LockRepository.LockedFile>
) {
    val photos = remember(items) { items.count { !it.isVideo } }
    val videos = remember(items) { items.count { it.isVideo } }
    val totalBytes = remember(items) { items.sumOf { it.sizeBytes } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 28.dp)
    ) {
        Text(
            text = "$photos ${if (photos == 1) "foto" else "fotos"} · $videos ${if (videos == 1) "vídeo" else "vídeos"}",
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
            text = "Cofre  ·  ${items.size} ${if (items.size == 1) "arquivo" else "arquivos"} protegido${if (items.size == 1) "" else "s"}",
            style = MaterialTheme.typography.bodyMedium,
            color = VzOnBackground.copy(alpha = 0.85f),
            fontSize = 12.sp
        )

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