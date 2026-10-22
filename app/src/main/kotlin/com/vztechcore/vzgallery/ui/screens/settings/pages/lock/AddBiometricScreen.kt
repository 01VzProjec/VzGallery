package com.vztechcore.vzgallery.ui.screens.settings.pages.lock

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.vztechcore.vzgallery.ui.theme.VzBackground
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzOnSurfaceVariant
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import com.vztechcore.vzgallery.ui.theme.VzSurface
import com.vztechcore.vzgallery.ui.theme.VzSurfaceVariant

private fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return ctx as? FragmentActivity
}

@Composable
fun AddBiometricScreen(
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }

    var authError by remember { mutableStateOf<String?>(null) }

    val biometricStatus = remember {
        BiometricManager.from(context).canAuthenticate(
            Authenticators.BIOMETRIC_STRONG or Authenticators.BIOMETRIC_WEAK
        )
    }
    val noneEnrolled = biometricStatus == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
    val noHardware = biometricStatus == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE

    fun openSecuritySettings() {
        try {
            context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
        } catch (_: Exception) {}
    }

    fun launchBiometric() {
        if (activity == null) {
            authError = "Erro interno: a tela atual não é FragmentActivity."
            return
        }
        authError = null
        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onDone()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                ) {
                    authError = "Erro ($errorCode): $errString"
                }
            }
        }
        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Adicionar digital")
            .setSubtitle("Toque no sensor de impressão digital")
            .setNegativeButtonText("Cancelar")
            .setAllowedAuthenticators(
                Authenticators.BIOMETRIC_STRONG or Authenticators.BIOMETRIC_WEAK
            )
            .build()
        prompt.authenticate(promptInfo)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VzBackground)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        TopBar(title = "Adicionar digital", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(VzSurfaceVariant.copy(alpha = 0.5f))
                    .padding(20.dp)
            ) {
                Column {
                    Icon(
                        Icons.Outlined.Fingerprint,
                        contentDescription = null,
                        tint = VzSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Atalho rápido e seguro",
                        style = MaterialTheme.typography.titleMedium,
                        color = VzOnBackground,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "A digital permite abrir a Área Trancada sem digitar sua senha ou PIN. É um atalho — não substitui a senha.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VzOnBackground,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Se a digital falhar, você continua podendo entrar com sua senha/PIN normalmente.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VzOnSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            if (noneEnrolled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(VzSurface)
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = null,
                                tint = VzSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Seu aparelho tem digital, mas nenhuma está cadastrada. Cadastre nas configurações do Android e volte aqui.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = VzOnSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = ::openSecuritySettings) {
                            Text("Abrir configurações de segurança", color = VzSecondary)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            if (noHardware) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(VzSurface)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = VzOnSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Este dispositivo não tem sensor de impressão digital. Você pode continuar protegendo com senha/PIN normalmente.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VzOnSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            authError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (!noHardware) {
                Button(
                    onClick = ::launchBiometric,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VzSecondary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        Icons.Outlined.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Ativar digital", fontSize = 15.sp)
                }
                Spacer(Modifier.height(12.dp))
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    "Cancelar",
                    color = VzOnSurfaceVariant,
                    fontSize = 15.sp
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = "Sua digital é processada pelo próprio Android. O VzGallery nunca armazena sua impressão digital.",
                style = MaterialTheme.typography.bodySmall,
                color = VzOnSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
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