package com.vztechcore.vzgallery.ui.screens.library.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.vztechcore.vzgallery.model.MediaItem
import com.vztechcore.vzgallery.model.ehVideo
import com.vztechcore.vzgallery.ui.components.VideoThumbCache
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzOnSurfaceVariant
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import com.vztechcore.vzgallery.ui.theme.VzSurfaceVariant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecentItemCard(
    item: MediaItem,
    reverse: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (reverse) {
            InfoColumn(item, reverse = true, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(14.dp))
            ThumbBox(item)
        } else {
            ThumbBox(item)
            Spacer(Modifier.width(14.dp))
            InfoColumn(item, reverse = false, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ThumbBox(item: MediaItem) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(VzSurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (item.ehVideo) {
            val bmp by produceState<ImageBitmap?>(null, item.uri) {
                value = VideoThumbCache.get(context, item.uri, 300)
            }
            bmp?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.uri)
                    .size(300)
                    .precision(Precision.INEXACT)
                    .crossfade(120)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (item.ehVideo) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoColumn(
    item: MediaItem,
    reverse: Boolean,
    modifier: Modifier
) {
    val align = if (reverse) Alignment.End else Alignment.Start

    Column(
        modifier = modifier,
        horizontalAlignment = align
    ) {
        Text(
            text = item.nome,
            style = MaterialTheme.typography.bodyLarge,
            color = VzOnBackground,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = formatDate(item.dataAdicionada),
            style = MaterialTheme.typography.bodyMedium,
            color = VzOnSurfaceVariant,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = formatSize(item.tamanhoBytes),
            style = MaterialTheme.typography.bodyMedium,
            color = VzSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatDate(dataAdicionada: Long): String {
    if (dataAdicionada <= 0) return "—"
    val fmt = SimpleDateFormat("dd 'de' MMM 'de' yyyy • HH:mm", Locale("pt", "BR"))
    return fmt.format(Date(dataAdicionada * 1000))
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "—"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.0f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.2f GB".format(gb)
}