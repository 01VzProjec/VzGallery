package com.vztechcore.vzgallery.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzOnSurfaceVariant
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import com.vztechcore.vzgallery.ui.theme.VzSurface

/**
 * Mostra o diálogo correto ao clicar em "Proteger":
 *   - se o cofre já está configurado → diálogo de confirmação (explica o que acontece)
 *   - se NÃO está configurado → diálogo de "configure primeiro" (com botão Prosseguir)
 *
 * O caller só precisa controlar 2 states:
 *   var dialogoCofre by remember { mutableStateOf(false) }
 *   var setupCofre by remember { mutableStateOf(false) }
 *
 * E renderizar:
 *   if (dialogoCofre) {
 *       MoveToVaultDialogs(
 *           lockConfigured = lockConfigured,
 *           selectedCount = selecionados.size,
 *           onDismiss = { dialogoCofre = false },
 *           onConfirm = { dialogoCofre = false; moveSelectionToVault() },
 *           onGoToSetup = { dialogoCofre = false; setupCofre = true }
 *       )
 *   }
 */
@Composable
fun MoveToVaultDialogs(
    lockConfigured: Boolean,
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onGoToSetup: () -> Unit
) {
    if (lockConfigured) {
        ConfirmMoveDialog(
            count = selectedCount,
            onCancel = onDismiss,
            onConfirm = onConfirm
        )
    } else {
        SetupRequiredDialog(
            onCancel = onDismiss,
            onProceed = onGoToSetup
        )
    }
}

@Composable
private fun ConfirmMoveDialog(
    count: Int,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = VzSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Mover ${if (count == 1) "1 item" else "$count itens"} para o cofre?",
                style = MaterialTheme.typography.titleLarge,
                color = VzOnBackground
            )
        },
        text = {
            Column {
                InfoRow(
                    icon = Icons.Outlined.VisibilityOff,
                    title = "Ficam invisíveis pra outros apps",
                    description = "Os itens saem da galeria do celular. Nenhum outro app vai conseguir vê-los.",
                    tint = VzSecondary
                )
                Spacer(Modifier.height(10.dp))
                InfoRow(
                    icon = Icons.Outlined.Lock,
                    title = "Só você acessa",
                    description = "Pra ver ou restaurar, é preciso entrar na Área Trancada com sua senha/PIN ou digital.",
                    tint = VzSecondary
                )
                Spacer(Modifier.height(10.dp))
                InfoRow(
                    icon = Icons.Outlined.Restore,
                    title = "Você pode recuperar",
                    description = "A qualquer momento você pode restaurar. Os itens voltam pra galeria na data original.",
                    tint = VzSecondary
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Enquanto estiverem no cofre, os itens continuam ocupando o mesmo espaço no dispositivo — só mudam de pasta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = VzOnSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Mover",
                    color = VzSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancelar", color = VzOnSurfaceVariant)
            }
        }
    )
}

@Composable
private fun SetupRequiredDialog(
    onCancel: () -> Unit,
    onProceed: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = VzSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Configure a proteção primeiro",
                style = MaterialTheme.typography.titleLarge,
                color = VzOnBackground
            )
        },
        text = {
            Column {
                InfoRow(
                    icon = Icons.Outlined.Security,
                    title = "Por que preciso configurar?",
                    description = "O cofre é protegido por senha/PIN. É essa proteção que impede outros apps de verem os itens.",
                    tint = VzSecondary
                )
                Spacer(Modifier.height(10.dp))
                InfoRow(
                    icon = Icons.Outlined.LockOpen,
                    title = "Você só precisa fazer uma vez",
                    description = "Na próxima vez que mover itens pro cofre, o movimento será direto (só pede confirmação).",
                    tint = VzSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onProceed) {
                Text(
                    text = "Prosseguir",
                    color = VzSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancelar", color = VzOnSurfaceVariant)
            }
        }
    )
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    tint: Color
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = VzOnBackground,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = VzOnSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}