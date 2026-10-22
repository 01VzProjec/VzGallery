
package com.vztechcore.vzgallery.ui.screens.settings.pages.lock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vztechcore.vzgallery.ui.theme.VzBackground
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzOnSurfaceVariant
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import com.vztechcore.vzgallery.ui.theme.VzSurface
import com.vztechcore.vzgallery.ui.theme.VzSurfaceVariant

private const val MIN_LENGTH = 6
private const val MAX_LENGTH = 10

enum class PinSetupStage {
    Create,
    Confirm
}

@Composable
fun PinSetupScreen(
    onBack: () -> Unit,
    onPinSet: (String) -> Unit
) {
    BackHandler { onBack() }

    var stage by remember { mutableStateOf(PinSetupStage.Create) }
    var firstPin by remember { mutableStateOf("") }
    var currentPin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val title = when (stage) {
        PinSetupStage.Create -> "Criar PIN"
        PinSetupStage.Confirm -> "Confirmar PIN"
    }

    val subtitle = when (stage) {
        PinSetupStage.Create -> "Use $MIN_LENGTH a $MAX_LENGTH dígitos"
        PinSetupStage.Confirm -> "Digite o PIN novamente para confirmar"
    }

    val canContinue = currentPin.length in MIN_LENGTH..MAX_LENGTH

    fun addDigit(digit: String) {
        if (currentPin.length >= MAX_LENGTH) return

        errorMessage = null
        currentPin += digit
    }

    fun removeDigit() {
        if (currentPin.isNotEmpty()) {
            currentPin = currentPin.dropLast(1)
        }
        errorMessage = null
    }

    fun continueSetup() {
        if (!canContinue) return

        when (stage) {
            PinSetupStage.Create -> {
                firstPin = currentPin
                currentPin = ""
                stage = PinSetupStage.Confirm
                showPin = false
                errorMessage = null
            }

            PinSetupStage.Confirm -> {
                if (currentPin == firstPin) {
                    onPinSet(firstPin)
                } else {
                    errorMessage = "Os PINs não coincidem. Tente novamente."
                    currentPin = ""
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
        TopBar(
            title = title,
            onBack = onBack
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = VzOnSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        PinDisplay(
            pin = currentPin,
            showPin = showPin,
            maxLength = MAX_LENGTH
        )

        Spacer(Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { showPin = !showPin }
            ) {
                Icon(
                    imageVector = if (showPin) {
                        Icons.Outlined.VisibilityOff
                    } else {
                        Icons.Outlined.Visibility
                    },
                    contentDescription = if (showPin) {
                        "Ocultar PIN"
                    } else {
                        "Mostrar PIN"
                    },
                    tint = VzOnSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        errorMessage?.let { message ->
            Spacer(Modifier.height(8.dp))

            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.weight(1f))

        NumberPad(
            onDigit = ::addDigit,
            onDelete = ::removeDigit,
            onContinue = ::continueSetup,
            canContinue = canContinue,
            continueLabel = if (stage == PinSetupStage.Create) {
                "Continuar"
            } else {
                "Confirmar PIN"
            }
        )

        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun PinDisplay(
    pin: String,
    showPin: Boolean,
    maxLength: Int
) {
    // Captura a cor no contexto composable, antes do Canvas.
    val pinColor = VzSecondary

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(maxLength) { index ->
            val char = pin.getOrNull(index)
            val isFilled = char != null

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isFilled) {
                            VzSurfaceVariant
                        } else {
                            VzSurfaceVariant.copy(alpha = 0.3f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    showPin && char != null -> {
                        Text(
                            text = char.toString(),
                            style = TextStyle(
                                color = VzOnBackground,
                                fontSize = 18.sp
                            )
                        )
                    }

                    isFilled -> {
                        Canvas(
                            modifier = Modifier.size(10.dp)
                        ) {
                            drawCircle(
                                color = pinColor,
                                radius = 5.dp.toPx(),
                                center = Offset(
                                    x = size.width / 2f,
                                    y = size.height / 2f
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberPad(
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onContinue: () -> Unit,
    canContinue: Boolean,
    continueLabel: String
) {
    val primaryColor = VzSecondary
    val onPrimaryColor = Color.White

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "⌫")
        )

        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { value ->
                    when (value) {
                        "" -> {
                            Spacer(
                                modifier = Modifier.size(64.dp)
                            )
                        }

                        "⌫" -> {
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Apagar",
                                    tint = VzOnBackground,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        else -> {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(VzSurface)
                                    .clickable {
                                        onDigit(value)
                                    }
                                    .semantics {
                                        contentDescription = value
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = value,
                                    style = TextStyle(
                                        color = VzOnBackground,
                                        fontSize = 22.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (canContinue) {
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = onPrimaryColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
            ) {
                Text(continueLabel)
            }
        }
    }
}

@Composable
private fun TopBar(
    title: String,
    onBack: () -> Unit
) {
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
                .clickable {
                    onBack()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
