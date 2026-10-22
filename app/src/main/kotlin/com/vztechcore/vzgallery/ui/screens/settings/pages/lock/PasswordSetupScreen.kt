package com.vztechcore.vzgallery.ui.screens.settings.pages.lock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vztechcore.vzgallery.ui.theme.VzBackground
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzOnSurfaceVariant
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import com.vztechcore.vzgallery.ui.theme.VzSurface

private const val MIN_LENGTH = 6
private const val MAX_LENGTH = 20

enum class PasswordSetupStage {
    Create,
    Confirm
}

@Composable
fun PasswordSetupScreen(
    onBack: () -> Unit,
    onPasswordSet: (String) -> Unit
) {
    BackHandler { onBack() }

    var stage by remember { mutableStateOf(PasswordSetupStage.Create) }
    var firstPassword by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val title = when (stage) {
        PasswordSetupStage.Create -> "Criar Senha"
        PasswordSetupStage.Confirm -> "Confirmar Senha"
    }
    val subtitle = when (stage) {
        PasswordSetupStage.Create -> "Use $MIN_LENGTH a $MAX_LENGTH caracteres"
        PasswordSetupStage.Confirm -> "Digite a senha novamente para confirmar"
    }

    fun validateAndProceed() {
        errorMessage = null
        when (stage) {
            PasswordSetupStage.Create -> {
                if (currentPassword.length < MIN_LENGTH) {
                    errorMessage = "Mínimo de $MIN_LENGTH caracteres"
                } else if (currentPassword.length > MAX_LENGTH) {
                    errorMessage = "Máximo de $MAX_LENGTH caracteres"
                } else {
                    firstPassword = currentPassword
                    currentPassword = ""
                    stage = PasswordSetupStage.Confirm
                }
            }
            PasswordSetupStage.Confirm -> {
                if (currentPassword == firstPassword) {
                    onPasswordSet(firstPassword)
                } else {
                    errorMessage = "As senhas não coincidem. Tente novamente."
                    currentPassword = ""
                    firstPassword = ""
                    stage = PasswordSetupStage.Create
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VzBackground)
            .windowInsetsPadding(WindowInsets.systemBars),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBar(title = title, onBack = onBack)

        Spacer(Modifier.height(40.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = VzOnSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = currentPassword,
            onValueChange = {
                if (it.length <= MAX_LENGTH) {
                    currentPassword = it
                    errorMessage = null
                }
            },
            label = { Text(if (stage == PasswordSetupStage.Create) "Senha" else "Confirmar senha") },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val icon = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility
                val desc = if (showPassword) "Ocultar senha" else "Mostrar senha"
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(icon, contentDescription = desc)
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { validateAndProceed() }
            ),
            isError = errorMessage != null,
            supportingText = {
                errorMessage?.let { Text(it) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = ::validateAndProceed,
            enabled = currentPassword.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = VzSecondary,
                contentColor = androidx.compose.ui.graphics.Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(52.dp)
        ) {
            Text(
                text = when (stage) {
                    PasswordSetupStage.Create -> "Continuar"
                    PasswordSetupStage.Confirm -> "Confirmar"
                },
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Voltar",
                tint = VzOnBackground
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.displayLarge,
            color = VzOnBackground,
            fontSize = 24.sp
        )
    }
}
