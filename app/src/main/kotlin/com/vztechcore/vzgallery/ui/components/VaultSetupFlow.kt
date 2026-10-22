package com.vztechcore.vzgallery.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.vztechcore.vzgallery.data.LockRepository
import com.vztechcore.vzgallery.data.LockType
import com.vztechcore.vzgallery.data.Prefs
import com.vztechcore.vzgallery.ui.screens.settings.pages.lock.BiometricSetupScreen
import com.vztechcore.vzgallery.ui.screens.settings.pages.lock.LockedVaultSettingsScreen
import com.vztechcore.vzgallery.ui.screens.settings.pages.lock.PasswordSetupScreen
import com.vztechcore.vzgallery.ui.screens.settings.pages.lock.PinSetupScreen
import kotlinx.coroutines.launch

/**
 * Fluxo completo de setup do cofre:
 *   1. Escolher tipo (PIN ou Senha)
 *   2. Criar + confirmar
 *   3. Ativar digital (opcional)
 *
 * Reusável no SettingsScreen E em qualquer lugar que precise configurar o cofre
 * (ex: quando o usuário tenta mover itens sem ter configuração).
 *
 * Não tem UI própria — só orquestra as 4 telas existentes.
 */
@Composable
fun VaultSetupFlow(
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { Prefs(context) }

    // 0 = escolher tipo, 1 = PIN, 2 = Senha, 3 = Digital
    var step by remember { mutableIntStateOf(0) }

    when (step) {
        0 -> LockedVaultSettingsScreen(
            onBack = onCancel,
            onChoosePin = { step = 1 },
            onChoosePassword = { step = 2 }
        )

        1 -> PinSetupScreen(
            onBack = { step = 0 },
            onPinSet = { pin ->
                scope.launch {
                    val salt = LockRepository.generateSalt()
                    val hash = LockRepository.hashPassword(pin, salt)
                    prefs.saveLock(LockType.PIN, hash, salt)
                    step = 3
                }
            }
        )

        2 -> PasswordSetupScreen(
            onBack = { step = 0 },
            onPasswordSet = { password ->
                scope.launch {
                    val salt = LockRepository.generateSalt()
                    val hash = LockRepository.hashPassword(password, salt)
                    prefs.saveLock(LockType.PASSWORD, hash, salt)
                    step = 3
                }
            }
        )

        3 -> BiometricSetupScreen(
            // Aqui o PIN/senha JÁ foram salvos. Se o usuário voltar,
            // só conclui o setup (não perde o que fez).
            onBack = onComplete,
            onDone = { biometricEnabled ->
                scope.launch {
                    prefs.setLockBiometricEnabled(biometricEnabled)
                    onComplete()
                }
            }
        )
    }
}