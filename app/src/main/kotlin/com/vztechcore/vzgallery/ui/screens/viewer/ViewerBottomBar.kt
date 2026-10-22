package com.vztechcore.vzgallery.ui.screens.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vztechcore.vzgallery.model.MediaItem
import com.vztechcore.vzgallery.ui.components.media.FitMode
import com.vztechcore.vzgallery.ui.components.media.VideoPlaybackControls
import com.vztechcore.vzgallery.ui.theme.VzBackground
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzSecondary

@Composable
fun ViewerBottomBar(
    item: MediaItem?,
    isVideo: Boolean,
    durationMs: Long,
    positionMs: Long,
    isPlaying: Boolean,
    trashMode: Boolean,
    hiddenMode: Boolean,
    isFavorite: Boolean,
    muted: Boolean,
    speed: Float,
    fitMode: FitMode,
    submenuOpen: Boolean,
    onSubmenuChange: (Boolean) -> Unit,
    onSeek: (Long) -> Unit,
    onSeekStart: () -> Unit,
    onSeekEnd: () -> Unit,
    onMuteToggle: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onFitModeChange: (FitMode) -> Unit,
    onShare: () -> Unit,
    onHideOrRestore: () -> Unit,
    onDelete: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onEdit: () -> Unit,
    onRestore: (() -> Unit)? = null,
    onDeletePermanently: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.55f))
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        if (item != null && isVideo && durationMs > 0) {
            VideoScrubber(
                uri = item.uri,
                duracaoMs = durationMs,
                posicaoMs = positionMs,
                onSeek = onSeek,
                onSeekStart = onSeekStart,
                onSeekEnd = onSeekEnd,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (trashMode && onRestore != null && onDeletePermanently != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onDeletePermanently,
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.DeleteForever, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Excluir", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Button(
                    onClick = onRestore,
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VzSecondary,
                        contentColor = VzBackground
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Restore, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Restaurar", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        } else if (isVideo && isPlaying) {
            VideoPlaybackControls(
                muted = muted,
                speed = speed,
                fitMode = fitMode,
                submenuOpen = submenuOpen,
                onSubmenuChange = onSubmenuChange,
                onMuteToggle = onMuteToggle,
                onSpeedChange = onSpeedChange,
                onFitModeChange = onFitModeChange
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionIcon(Icons.Outlined.Share, "Enviar", onAction = onShare)
                if (hiddenMode) {
                    ActionIcon(
                        Icons.Outlined.Visibility, "Restaurar",
                        color = VzSecondary,
                        onAction = onHideOrRestore
                    )
                } else {
                    ActionIcon(
                        Icons.Outlined.VisibilityOff, "Ocultar",
                        onAction = onHideOrRestore
                    )
                }
                ActionIcon(
                    Icons.Outlined.Delete, "Excluir",
                    color = Color(0xFFEF4444),
                    onAction = onDelete
                )
                ActionIcon(
                    if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    "Favoritar",
                    color = if (isFavorite) Color(0xFFEF4444) else VzOnBackground,
                    onAction = onFavoriteToggle
                )
                ActionIcon(
                    if (isVideo) Icons.Outlined.ContentCut else Icons.Outlined.Edit,
                    if (isVideo) "Cortar" else "Editar",
                    onAction = onEdit
                )
            }
        }
    }
}

@Composable
private fun ActionIcon(
    icon: ImageVector,
    title: String,
    onAction: () -> Unit,
    color: Color = VzOnBackground
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onAction() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, title, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, color = color, fontSize = 11.sp)
    }
}