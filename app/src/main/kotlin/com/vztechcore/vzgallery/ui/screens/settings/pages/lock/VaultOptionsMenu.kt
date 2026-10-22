package com.vztechcore.vzgallery.ui.screens.settings.pages.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vztechcore.vzgallery.data.LockType
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzOnSurfaceVariant
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import com.vztechcore.vzgallery.ui.theme.VzSurface
import com.vztechcore.vzgallery.ui.theme.VzSurfaceVariant
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun VaultOptionsMenu(
    lockType: LockType,
    biometricEnabled: Boolean,
    onChangePassword: () -> Unit,
    onAddBiometric: () -> Unit,
    onBackup: () -> Unit,
    onDisable: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR"))

        while (true) {
            val now = Calendar.getInstance().time
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now).replaceFirstChar { it.uppercase() }
            delay(1000)
        }
    }

    Column(
        modifier = modifier
            .width(250.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(VzSurface, VzSurfaceVariant)
                )
            )
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = currentDate,
                style = MaterialTheme.typography.bodyMedium,
                color = VzOnSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = currentTime,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 30.sp),
                color = VzSecondary
            )
        }

        HorizontalDivider(
            color = VzSurfaceVariant,
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )

        VaultMenuItem(
            icon = Icons.Outlined.Key,
            label = if (lockType == LockType.PIN) "Alterar PIN" else "Alterar senha",
            onClick = onChangePassword
        )

        if (!biometricEnabled) {
            VaultMenuItem(
                icon = Icons.Outlined.Fingerprint,
                label = "Adicionar digital",
                onClick = onAddBiometric
            )
        }

        VaultMenuItem(
            icon = Icons.Outlined.Backup,
            label = "Backup do cofre",
            onClick = onBackup
        )

        HorizontalDivider(
            color = VzSurfaceVariant,
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )

        VaultMenuItem(
            icon = Icons.Outlined.LockOpen,
            label = "Desativar proteção",
            labelColor = MaterialTheme.colorScheme.error,
            iconTint = MaterialTheme.colorScheme.error,
            onClick = onDisable
        )

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun VaultMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    labelColor: androidx.compose.ui.graphics.Color = VzOnBackground,
    iconTint: androidx.compose.ui.graphics.Color = VzSecondary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = labelColor
        )
    }
}