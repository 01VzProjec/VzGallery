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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Password
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.vztechcore.vzgallery.data.LockRepository
import com.vztechcore.vzgallery.data.LockType
import com.vztechcore.vzgallery.data.Prefs
import com.vztechcore.vzgallery.ui.theme.VzBackground
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzOnSurfaceVariant
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import com.vztechcore.vzgallery.ui.theme.VzSurface
import com.vztechcore.vzgallery.ui.theme.VzSurfaceVariant
import kotlinx.coroutines.launch

private const val MIN_LEN = 6
private const val MAX_PIN = 10
private const val MAX_PWD = 20

private enum class ChangeLockStage {
    Verify,
    ChooseType,
    SetupCreate,
    SetupConfirm
}

private fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return ctx as? FragmentActivity
}

@Composable
fun ChangeLockScreen(
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { Prefs(context) }
    val activity = remember(context) { context.findFragmentActivity() }

    val currentType by prefs.lockType.collectAsState(initial = LockType.PIN)
    val biometricEnabled by prefs.lockBiometricEnabled.collectAsState(initial = false)

    var stage by remember { mutableStateOf(ChangeLockStage.Verify) }

    // Verify stage
    var verifyInput by remember { mutableStateOf("") }
    var verifyError by remember { mutableStateOf<String?>(null) }
    var showVerifyInput by remember { mutableStateOf(false) }

    // New lock stage
    var newType by remember { mutableStateOf<LockType?>(null) }
    var newInput by remember { mutableStateOf("") }
    var confirmInput by remember { mutableStateOf("") }
    var showNewInput by remember { mutableStateOf(false) }
    var newError by remember { mutableStateOf<String?>(null) }

    fun goToChooseType() {
        stage = ChangeLockStage.ChooseType
        verifyInput = ""
        verifyError = null
        showVerifyInput = false
    }

    fun verifyWithPassword() {
        if (verifyInput.length < MIN_LEN) return
        scope.launch {
            val ok = prefs.verifyLockPassword(verifyInput)
            if (ok) goToChooseType()
            else {
                verifyError = "Senha incorreta"
                verifyInput = ""
            }
        }
    }

    fun launchBiometric() {
        if (!biometricEnabled || activity == null) return
        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                goToChooseType()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                ) {
                    verifyError = "Erro ($errorCode): $errString"
                }
            }
        }
        val prompt = BiometricPrompt(activity, executor, callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Confirme sua identidade")
            .setSubtitle("Use sua digital")
            .setNegativeButtonText("Cancelar")
            .setAllowedAuthenticators(
                Authenticators.BIOMETRIC_STRONG or Authenticators.BIOMETRIC_WEAK
            )
            .build()
        prompt.authenticate(info)
    }

    val verifyIsPin = currentType == LockType.PIN
    val verifyMaxLen = if (verifyIsPin) MAX_PIN else MAX_PWD

    val setupIsPin = newType == LockType.PIN
    val setupMaxLen = if (setupIsPin) MAX_PIN else MAX_PWD

    fun goToSetupCreate(type: LockType) {
        newType = type
        newInput = ""
        confirmInput = ""
        newError = null
        showNewInput = false
        stage = ChangeLockStage.SetupCreate
    }

    fun goToSetupConfirm() {
        when {
            newInput.length < MIN_LEN -> {
                newError = "Mínimo de $MIN_LEN ${if (setupIsPin) "dígitos" else "caracteres"}"
            }
            else -> {
                confirmInput = ""
                newError = null
                showNewInput = false
                stage = ChangeLockStage.SetupConfirm
            }
        }
    }

    fun saveNewLock() {
        if (confirmInput != newInput) {
            newError = "As senhas não coincidem. Tente novamente."
            newInput = ""
            confirmInput = ""
            stage = ChangeLockStage.SetupCreate
            return
        }
        val type = newType ?: return
        scope.launch {
            val salt = LockRepository.generateSalt()
            val hash = LockRepository.hashPassword(confirmInput, salt)
            prefs.saveLock(type, hash, salt)
            onDone()
        }
    }

    val title = when (stage) {
        ChangeLockStage.Verify -> "Confirme sua identidade"
        ChangeLockStage.ChooseType -> "Novo tipo de proteção"
        ChangeLockStage.SetupCreate ->
            if (setupIsPin) "Criar novo PIN" else "Criar nova senha"
        ChangeLockStage.SetupConfirm ->
            if (setupIsPin) "Confirmar novo PIN" else "Confirmar nova senha"
    }

    val subtitle = when (stage) {
        ChangeLockStage.Verify ->
            if (biometricEnabled) "Digite sua senha atual ou use a digital"
            else if (verifyIsPin) "Digite seu PIN atual"
            else "Digite sua senha atual"
        ChangeLockStage.ChooseType -> "Escolha como quer proteger a Área Trancada"
        ChangeLockStage.SetupCreate ->
            if (setupIsPin) "Use $MIN_LEN a $MAX_PIN dígitos"
            else "Use $MIN_LEN a $MAX_PWD caracteres"
        ChangeLockStage.SetupConfirm -> "Digite novamente para confirmar"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VzBackground)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        TopBar(title = "Alterar proteção", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = VzOnBackground,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = VzOnSurfaceVariant,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(28.dp))

            when (stage) {
                ChangeLockStage.Verify -> VerifyStageContent(
                    isPin = verifyIsPin,
                    input = verifyInput,
                    showInput = showVerifyInput,
                    maxLen = verifyMaxLen,
                    error = verifyError,
                    biometricEnabled = biometricEnabled,
                    onInputChange = {
                        if (it.length <= verifyMaxLen) {
                            verifyInput = it
                            verifyError = null
                        }
                    },
                    onShowToggle = { showVerifyInput = !showVerifyInput },
                    onDigit = {
                        if (verifyInput.length < verifyMaxLen) {
                            verifyInput += it
                            verifyError = null
                        }
                    },
                    onDelete = {
                        if (verifyInput.isNotEmpty()) verifyInput = verifyInput.dropLast(1)
                        verifyError = null
                    },
                    onContinue = ::verifyWithPassword,
                    onBiometric = ::launchBiometric
                )

                ChangeLockStage.ChooseType -> ChooseTypeContent(
                    onChoosePin = { goToSetupCreate(LockType.PIN) },
                    onChoosePassword = { goToSetupCreate(LockType.PASSWORD) }
                )

                ChangeLockStage.SetupCreate -> SetupInputContent(
                    isPin = setupIsPin,
                    input = newInput,
                    showInput = showNewInput,
                    maxLen = setupMaxLen,
                    error = newError,
                    buttonLabel = "Continuar",
                    onInputChange = {
                        if (it.length <= setupMaxLen) {
                            newInput = it
                            newError = null
                        }
                    },
                    onShowToggle = { showNewInput = !showNewInput },
                    onDigit = {
                        if (newInput.length < setupMaxLen) {
                            newInput += it
                            newError = null
                        }
                    },
                    onDelete = {
                        if (newInput.isNotEmpty()) newInput = newInput.dropLast(1)
                        newError = null
                    },
                    onContinue = ::goToSetupConfirm
                )

                ChangeLockStage.SetupConfirm -> SetupInputContent(
                    isPin = setupIsPin,
                    input = confirmInput,
                    showInput = showNewInput,
                    maxLen = setupMaxLen,
                    error = newError,
                    buttonLabel = "Salvar",
                    onInputChange = {
                        if (it.length <= setupMaxLen) {
                            confirmInput = it
                            newError = null
                        }
                    },
                    onShowToggle = { showNewInput = !showNewInput },
                    onDigit = {
                        if (confirmInput.length < setupMaxLen) {
                            confirmInput += it
                            newError = null
                        }
                    },
                    onDelete = {
                        if (confirmInput.isNotEmpty()) confirmInput = confirmInput.dropLast(1)
                        newError = null
                    },
                    onContinue = ::saveNewLock
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ==================== SUB-COMPONENTES ====================

@Composable
private fun VerifyStageContent(
    isPin: Boolean,
    input: String,
    showInput: Boolean,
    maxLen: Int,
    error: String?,
    biometricEnabled: Boolean,
    onInputChange: (String) -> Unit,
    onShowToggle: () -> Unit,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onContinue: () -> Unit,
    onBiometric: () -> Unit
) {
    if (isPin) {
        PinDisplay(
            pin = input,
            showPin = showInput,
            maxLength = maxLen
        )
        Spacer(Modifier.height(8.dp))
        ShowToggleRow(showInput = showInput, onToggle = onShowToggle)
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(24.dp))
        NumberPad(
            onDigit = onDigit,
            onDelete = onDelete,
            onContinue = onContinue,
            canContinue = input.length >= MIN_LEN,
            continueLabel = "Continuar"
        )
        if (biometricEnabled) {
            Spacer(Modifier.height(16.dp))
            BiometricButton(onBiometric = onBiometric)
        }
    } else {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            label = { Text("Senha atual") },
            visualTransformation = if (showInput) VisualTransformation.None
            else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onShowToggle) {
                    Icon(
                        if (showInput) Icons.Outlined.VisibilityOff
                        else Icons.Outlined.Visibility,
                        contentDescription = null
                    )
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onContinue() }),
            isError = error != null,
            supportingText = { error?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onContinue,
            enabled = input.length >= MIN_LEN,
            colors = ButtonDefaults.buttonColors(
                containerColor = VzSecondary,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Continuar", fontSize = 15.sp)
        }
        if (biometricEnabled) {
            Spacer(Modifier.height(12.dp))
            BiometricButton(onBiometric = onBiometric)
        }
    }
}

@Composable
private fun ChooseTypeContent(
    onChoosePin: () -> Unit,
    onChoosePassword: () -> Unit
) {
    Column {
        TypeCard(
            icon = Icons.Outlined.Keyboard,
            title = "PIN",
            subtitle = "6 a 10 dígitos numéricos",
            onClick = onChoosePin
        )
        Spacer(Modifier.height(12.dp))
        TypeCard(
            icon = Icons.Outlined.Password,
            title = "Senha",
            subtitle = "6 a 20 caracteres",
            onClick = onChoosePassword
        )
    }
}

@Composable
private fun TypeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(VzSurface)
            .clickable { onClick() }
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(VzSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = VzSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = VzOnBackground,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = VzOnSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SetupInputContent(
    isPin: Boolean,
    input: String,
    showInput: Boolean,
    maxLen: Int,
    error: String?,
    buttonLabel: String,
    onInputChange: (String) -> Unit,
    onShowToggle: () -> Unit,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onContinue: () -> Unit
) {
    if (isPin) {
        PinDisplay(pin = input, showPin = showInput, maxLength = maxLen)
        Spacer(Modifier.height(8.dp))
        ShowToggleRow(showInput = showInput, onToggle = onShowToggle)
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(24.dp))
        NumberPad(
            onDigit = onDigit,
            onDelete = onDelete,
            onContinue = onContinue,
            canContinue = input.length >= MIN_LEN,
            continueLabel = buttonLabel
        )
    } else {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            label = { Text("Nova senha") },
            visualTransformation = if (showInput) VisualTransformation.None
            else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onShowToggle) {
                    Icon(
                        if (showInput) Icons.Outlined.VisibilityOff
                        else Icons.Outlined.Visibility,
                        contentDescription = null
                    )
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onContinue() }),
            isError = error != null,
            supportingText = { error?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onContinue,
            enabled = input.length >= MIN_LEN,
            colors = ButtonDefaults.buttonColors(
                containerColor = VzSecondary,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(buttonLabel, fontSize = 15.sp)
        }
    }
}

@Composable
private fun BiometricButton(onBiometric: () -> Unit) {
    TextButton(onClick = onBiometric) {
        Icon(Icons.Outlined.Fingerprint, contentDescription = null, tint = VzSecondary)
        Spacer(Modifier.width(8.dp))
        Text("Usar digital", color = VzSecondary)
    }
}

@Composable
private fun ShowToggleRow(showInput: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (showInput) Icons.Outlined.VisibilityOff
                else Icons.Outlined.Visibility,
                contentDescription = if (showInput) "Ocultar" else "Mostrar",
                tint = VzOnSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun PinDisplay(pin: String, showPin: Boolean, maxLength: Int) {
    val pinColor = VzSecondary
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
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
private fun NumberPad(
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onContinue: () -> Unit,
    canContinue: Boolean,
    continueLabel: String
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
        Spacer(Modifier.height(8.dp))
        if (canContinue) {
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VzSecondary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(continueLabel, fontSize = 15.sp)
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