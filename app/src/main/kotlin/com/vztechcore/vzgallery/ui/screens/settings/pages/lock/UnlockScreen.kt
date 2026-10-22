package com.vztechcore.vzgallery.ui.screens.settings.pages.lock

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.vztechcore.vzgallery.data.LockType
import com.vztechcore.vzgallery.data.Prefs
import com.vztechcore.vzgallery.ui.theme.*
import kotlinx.coroutines.launch

private fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return ctx as? FragmentActivity
}

@Composable
fun UnlockScreen(
    onUnlocked: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { Prefs(context) }
    val lockType by prefs.lockType.collectAsState(initial = LockType.PIN)
    val biometricEnabled by prefs.lockBiometricEnabled.collectAsState(initial = false)

    var input by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPassword by remember { mutableStateOf(false) }

    val isPin = lockType == LockType.PIN
    val maxLen = if (isPin) 10 else 20
    val minLen = 6

    fun verify() {
        if (input.length < minLen) return
        scope.launch {
            val ok = prefs.verifyLockPassword(input)
            if (ok) {
                onUnlocked()
            } else {
                errorMessage = "Senha incorreta"
                input = ""
            }
        }
    }

    fun launchBiometric() {
        if (!biometricEnabled) return
        val activity = context.findFragmentActivity() ?: return
        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onUnlocked()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                ) {
                    errorMessage = "Erro ($errorCode): $errString"
                }
            }
        }
        val prompt = BiometricPrompt(activity, executor, callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloquear Área Trancada")
            .setSubtitle("Toque no sensor de impressão digital")
            .setNegativeButtonText("Cancelar")
            .setAllowedAuthenticators(
                Authenticators.BIOMETRIC_STRONG or Authenticators.BIOMETRIC_WEAK
            )
            .build()
        prompt.authenticate(info)
    }

    LaunchedEffect(Unit) {
        if (biometricEnabled) launchBiometric()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VzBackground)
            .windowInsetsPadding(WindowInsets.systemBars),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBar(title = "Área Trancada", onBack = onBack)

        Spacer(Modifier.height(40.dp))

        Text(
            text = if (isPin) "Digite seu PIN" else "Digite sua senha",
            style = MaterialTheme.typography.bodyMedium,
            color = VzOnSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        if (isPin) {
            PinDisplay(pin = input, showPin = showPassword, maxLength = maxLen)
        } else {
            OutlinedTextField(
                value = input,
                onValueChange = {
                    if (it.length <= maxLen) {
                        input = it
                        errorMessage = null
                    }
                },
                label = { Text("Senha") },
                visualTransformation = if (showPassword) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Outlined.VisibilityOff
                            else Icons.Outlined.Visibility,
                            contentDescription = null
                        )
                    }
                },
                singleLine = true,
                isError = errorMessage != null,
                supportingText = { errorMessage?.let { Text(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = ::verify,
                enabled = input.length >= minLen,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VzSecondary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(52.dp)
            ) {
                Text("Desbloquear", fontSize = 16.sp)
            }
        }

        if (isPin) {
            errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.weight(1f))
            PinKeyboard(
                onDigit = {
                    if (input.length < maxLen) {
                        input += it
                        errorMessage = null
                    }
                },
                onDelete = {
                    if (input.isNotEmpty()) input = input.dropLast(1)
                    errorMessage = null
                },
                onContinue = ::verify,
                canContinue = input.length >= minLen
            )
            Spacer(Modifier.height(32.dp))
        } else {
            Spacer(Modifier.weight(1f))
        }

        if (biometricEnabled) {
            TextButton(onClick = ::launchBiometric) {
                Icon(Icons.Outlined.Fingerprint, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Usar digital", color = VzSecondary)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PinDisplay(pin: String, showPin: Boolean, maxLength: Int) {
    val pinColor = VzSecondary
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(maxLength) { index ->
            val char = pin.getOrNull(index)
            val isFilled = char != null
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isFilled) VzSurfaceVariant
                        else VzSurfaceVariant.copy(alpha = 0.3f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    showPin && char != null -> Text(
                        char.toString(),
                        style = TextStyle(color = VzOnBackground, fontSize = 14.sp)
                    )
                    isFilled -> Canvas(Modifier.size(8.dp)) {
                        drawCircle(
                            color = pinColor,
                            radius = 4.dp.toPx(),
                            center = Offset(size.width / 2f, size.height / 2f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PinKeyboard(
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onContinue: () -> Unit,
    canContinue: Boolean
) {
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
                        "" -> Spacer(Modifier.size(64.dp))
                        "⌫" -> IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Apagar",
                                tint = VzOnBackground,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        else -> Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(VzSurface)
                                .clickable { onDigit(value) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                value,
                                style = TextStyle(color = VzOnBackground, fontSize = 22.sp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (canContinue) {
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VzSecondary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
                    .height(52.dp)
            ) {
                Text("Desbloquear", fontSize = 16.sp)
            }
        }
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