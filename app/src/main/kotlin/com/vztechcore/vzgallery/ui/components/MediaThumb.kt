package com.vztechcore.vzgallery.ui.components

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import coil.size.Precision
import com.vztechcore.vzgallery.model.MediaItem
import com.vztechcore.vzgallery.ui.theme.VzPrimary
import com.vztechcore.vzgallery.ui.theme.VzSurfaceVariant

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaThumb(
    item: MediaItem,
    isSelected: Boolean = false,
    isFavorite: Boolean = false,
    showName: Boolean = false,
    showFavoriteBadge: Boolean = true,
    showCloudOffBadge: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onPositioned: (Rect) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.90f else 1f,
        animationSpec = tween(140),
        label = "scale"
    )

    val radius = RoundedCornerShape(10.dp)
    val borderWidth = if (isSelected) 3.dp else 0.dp

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .onGloballyPositioned { coords ->
                onPositioned(coords.boundsInWindow())
            }
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
            if (item.isVideo) {
                VideoThumb(uri = item.uri, durationMs = item.duracaoMs)
            } else {
                ImageThumb(item = item)
            }
        }

        // Badge de formato (canto superior ESQUERDO)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = formatarFormato(item),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Badge de favorito (canto superior DIREITO)
        if (showFavoriteBadge && isFavorite) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Favorito",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Badge de "oculto" (nuvem riscada) — canto superior DIREITO
        if (showCloudOffBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CloudOff,
                    contentDescription = "Oculto",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Badge de duração (canto inferior DIREITO — só vídeo)
        if (item.isVideo && item.duracaoMs != null && item.duracaoMs > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = formatarDuracao(item.duracaoMs),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Nome do arquivo (rodapé sobreposto, se ativado)
        if (showName) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = item.nome,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ImageThumb(item: MediaItem) {
    val request = ImageRequest.Builder(LocalContext.current)
        .data(item.uri)
        .size(300)
        .precision(Precision.INEXACT)
        .crossfade(120)
        .memoryCacheKey(item.uri.toString())
        .build()

    AsyncImage(
        model = request,
        contentDescription = item.nome,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun VideoThumb(uri: Uri, durationMs: Long?) {
    val context = LocalContext.current

    // 🔥 Pega um frame em ~25% do vídeo (ou 1s se não tiver duração conhecida).
    // Isso evita frames pretos que costumam aparecer no início de muitos vídeos.
    val frameMs: Long = if ((durationMs ?: 0L) > 3000L) {
        (durationMs!! / 4L).coerceAtLeast(500L)
    } else {
        1000L
    }

    val request = ImageRequest.Builder(context)
        .data(uri)
        .videoFrameMillis(frameMs)
        .size(300)
        .precision(Precision.INEXACT)
        .crossfade(120)
        // Memory key inclui o timestamp, pra não reaproveitar cache de frame diferente
        .memoryCacheKey("${uri}@${frameMs}")
        .build()

    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}

private fun formatarFormato(item: MediaItem): String {
    val ext = item.nome.substringAfterLast('.', "").uppercase()
    return when {
        ext.isNotEmpty() -> ext
        item.isVideo -> "VIDEO"
        else -> "IMG"
    }
}

private fun formatarDuracao(ms: Long): String {
    val totalSegundos = ms / 1000
    val minutos = totalSegundos / 60
    val segundos = totalSegundos % 60
    return "%d:%02d".format(minutos, segundos)
}