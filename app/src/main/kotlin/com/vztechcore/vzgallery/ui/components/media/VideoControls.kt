package com.vztechcore.vzgallery.ui.components.media

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Replay30
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import com.vztechcore.vzgallery.ui.theme.VzSurface
import com.vztechcore.vzgallery.ui.theme.VzSurfaceVariant

private val SPEEDS = listOf(0.1f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 3.0f, 4.0f)

/**
 * Center overlay: play/pause circle + skip buttons.
 * Shown when video controls are visible.
 */
@Composable
fun VideoCenterControls(
    isPlaying: Boolean,
    isPrepared: Boolean,
    onPlayPause: () -> Unit,
    onReplay30: () -> Unit,
    onReplay10: () -> Unit,
    onForward10: () -> Unit,
    onForward30: () -> Unit
) {
    if (!isPrepared) return

    if (!isPlaying) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onPlayPause() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Tocar",
                tint = VzOnBackground,
                modifier = Modifier.size(48.dp)
            )
        }
    } else {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SkipButton(icon = Icons.Filled.Replay30, onClick = onReplay30)
            SkipButton(icon = Icons.Filled.Replay10, onClick = onReplay10)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(VzSecondary)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onPlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Pause,
                    contentDescription = "Pausar",
                    tint = VzOnBackground,
                    modifier = Modifier.size(32.dp)
                )
            }
            SkipButton(icon = Icons.Filled.Forward10, onClick = onForward10)
            SkipButton(icon = Icons.Filled.Forward30, onClick = onForward30)
        }
    }
}

@Composable
private fun SkipButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = VzOnBackground, modifier = Modifier.size(28.dp))
    }
}

/**
 * Row of playback controls: mute, fit mode, speed.
 */
@Composable
fun VideoPlaybackControls(
    muted: Boolean,
    speed: Float,
    fitMode: FitMode,
    submenuOpen: Boolean,
    onSubmenuChange: (Boolean) -> Unit,
    onMuteToggle: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onFitModeChange: (FitMode) -> Unit
) {
    var fitMenuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onMuteToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                contentDescription = if (muted) "Ativar som" else "Silenciar",
                tint = if (muted) VzSecondary else VzOnBackground,
                modifier = Modifier.size(24.dp)
            )
        }

        Box {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { fitMenuOpen = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AspectRatio,
                    contentDescription = "Ajuste de tela",
                    tint = if (fitMode != FitMode.ORIGINAL) VzSecondary else VzOnBackground,
                    modifier = Modifier.size(24.dp)
                )
            }
            DropdownMenu(
                expanded = fitMenuOpen,
                onDismissRequest = { fitMenuOpen = false },
                modifier = Modifier.background(VzSurface)
            ) {
                DropdownMenuItem(
                    text = { Text("Original", color = if (fitMode == FitMode.ORIGINAL) VzSecondary else VzOnBackground) },
                    onClick = { onFitModeChange(FitMode.ORIGINAL); fitMenuOpen = false }
                )
                DropdownMenuItem(
                    text = { Text("Preencher (zoom)", color = if (fitMode == FitMode.FILL) VzSecondary else VzOnBackground) },
                    onClick = { onFitModeChange(FitMode.FILL); fitMenuOpen = false }
                )
                DropdownMenuItem(
                    text = { Text("Alongar", color = if (fitMode == FitMode.STRETCH) VzSecondary else VzOnBackground) },
                    onClick = { onFitModeChange(FitMode.STRETCH); fitMenuOpen = false }
                )
            }
        }

        Box {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(VzSurfaceVariant.copy(alpha = 0.5f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onSubmenuChange(true) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "${formatSpeed(speed)}x",
                    color = VzSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            DropdownMenu(
                expanded = submenuOpen,
                onDismissRequest = { onSubmenuChange(false) },
                modifier = Modifier.background(VzSurface)
            ) {
                SPEEDS.forEach { v ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                "${formatSpeed(v)}x",
                                color = if (v == speed) VzSecondary else VzOnBackground,
                                fontWeight = if (v == speed) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onSpeedChange(v)
                            onSubmenuChange(false)
                        }
                    )
                }
            }
        }
    }
}

fun formatSpeed(v: Float): String =
    if (v == v.toInt().toFloat()) "${v.toInt()}.0" else v.toString()