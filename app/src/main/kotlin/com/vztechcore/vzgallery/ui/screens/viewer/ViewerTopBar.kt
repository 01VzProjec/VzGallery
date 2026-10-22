package com.vztechcore.vzgallery.ui.screens.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ScreenRotation
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
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ViewerTopBar(
    item: MediaItem?,
    landscapeMode: Boolean,
    onBack: () -> Unit,
    onToggleLandscape: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.55f))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TopIcon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", onClick = onBack)

            if (item != null) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val dateFormatted = remember(item.dataAdicionada) {
                        SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("pt", "BR"))
                            .format(Date(item.dataAdicionada * 1000))
                            .replaceFirstChar { it.uppercase() }
                    }
                    val timeFormatted = remember(item.dataAdicionada) {
                        SimpleDateFormat("HH:mm", Locale.getDefault())
                            .format(Date(item.dataAdicionada * 1000))
                    }
                    Text(
                        dateFormatted,
                        style = MaterialTheme.typography.bodyMedium,
                        color = VzOnBackground,
                        fontSize = 13.sp
                    )
                    Text(
                        timeFormatted,
                        style = MaterialTheme.typography.titleLarge,
                        color = VzSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            TopIcon(
                Icons.Filled.ScreenRotation,
                "Girar tela",
                tint = if (landscapeMode) VzSecondary else VzOnBackground,
                onClick = onToggleLandscape
            )
        }
    }
}

@Composable
private fun TopIcon(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: Color = VzOnBackground
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, tint = tint, modifier = Modifier.size(24.dp))
    }
}