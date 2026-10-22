package com.vztechcore.vzgallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import com.vztechcore.vzgallery.ui.theme.VzSurface
import com.vztechcore.vzgallery.ui.theme.VzSurfaceVariant

@Composable
fun SelectionBottomBar(
    onDelete: () -> Unit,
    onHide: () -> Unit,
    onShare: () -> Unit,
    onProtect: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        VzSurface.copy(alpha = 0f),
                        VzSurface.copy(alpha = 0.95f),
                        VzSurface
                    )
                )
            )
            .padding(horizontal = 8.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionItem(
                icon = Icons.Outlined.Delete,
                title = "Excluir",
                onClick = onDelete
            )
            ActionItem(
                icon = Icons.Outlined.VisibilityOff,
                title = "Ocultar",
                onClick = onHide
            )
            if (onProtect != null) {
                ActionItem(
                    icon = Icons.Outlined.Lock,
                    title = "Proteger",
                    onClick = onProtect
                )
            }
            ActionItem(
                icon = Icons.Outlined.Share,
                title = "Enviar",
                onClick = onShare
            )
        }
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(VzSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = VzSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = VzOnBackground
        )
    }
}