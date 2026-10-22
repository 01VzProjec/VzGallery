package com.vztechcore.vzgallery.ui.screens.welcome

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vztechcore.vzgallery.ui.theme.VzBackground
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzOnSurfaceVariant
import com.vztechcore.vzgallery.ui.theme.VzPrimary
import com.vztechcore.vzgallery.ui.theme.VzPrimaryDark
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import com.vztechcore.vzgallery.ui.theme.VzSurface
import com.vztechcore.vzgallery.ui.theme.VzSurfaceVariant

@Composable
fun WelcomeScreen(
    onPermissionGranted: () -> Unit
) {
    val permissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            onPermissionGranted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(VzBackground, VzSurface)
                )
            )
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(48.dp))

        // Logo circular — gradiente azul marinho → azul claro
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(VzPrimary, VzSecondary)
                    )
                )
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PhotoLibrary,
                contentDescription = null,
                tint = VzOnBackground,
                modifier = Modifier.size(52.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "VzGallery",
            style = MaterialTheme.typography.displayLarge,
            color = VzOnBackground,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Sua galeria, do seu jeito",
            style = MaterialTheme.typography.bodyLarge,
            color = VzSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(10.dp))

        // Badge "projeto de estudo"
        Surface(
            color = VzSecondary.copy(alpha = 0.15f),
            shape = RoundedCornerShape(50),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "🧪  Projeto de estudo",
                style = MaterialTheme.typography.bodyMedium,
                color = VzSecondary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        Spacer(Modifier.height(32.dp))

        // === Seção: Por que esse app existe ===
        SectionTitle("Por que criei esse app?")

        Spacer(Modifier.height(10.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = VzSurfaceVariant,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Eu programo direto pelo celular, usando o AndroidIDE. " +
                        "Só que a cada build, um monte de SVG, PNG e arquivos " +
                        "temporários caía na minha galeria e deixava tudo uma " +
                        "bagunça. Era impossível achar as fotos de verdade no " +
                        "meio daquele caos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VzOnSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Em vez de brigar com a galeria do sistema, resolvi " +
                        "fazer a minha. Assim nasceu o VzGallery: uma galeria " +
                        "que te deixa escolher exatamente o que você quer ver — " +
                        "e esconder o resto.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VzOnSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // === Seção: O que dá pra fazer ===
        SectionTitle("O que dá pra fazer aqui")

        Spacer(Modifier.height(10.dp))

        FeatureItem(
            icon = Icons.Filled.VisibilityOff,
            title = "Ocultar fotos individuais",
            description = "Marca uma imagem específica e ela some da lista"
        )
        FeatureItem(
            icon = Icons.Filled.FolderOff,
            title = "Esconder pastas inteiras",
            description = "Pastas de projeto, temp, caches — tudo fora da vista"
        )
        FeatureItem(
            icon = Icons.Filled.Code,
            title = "Filtrar por tipo de arquivo",
            description = "Tchau pros SVGs aleatórios poluindo a galeria"
        )

        Spacer(Modifier.height(28.dp))

        // === Seção: Sobre o projeto ===
        SectionTitle("Sobre o projeto")

        Spacer(Modifier.height(10.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = VzSurfaceVariant,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                InfoRow("Desenvolvedor", "Vtzin x")
                Spacer(Modifier.height(8.dp))
                InfoRow("Equipe", "VzTechCore")
                Spacer(Modifier.height(8.dp))
                InfoRow("Versão", "1.0 (MVP)")
                Spacer(Modifier.height(8.dp))
                InfoRow("Tipo", "Projeto pessoal / estudo")
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Não é um app comercial — é um experimento pra " +
                        "aprender Compose, MediaStore e resolver um problema " +
                        "real do dia a dia. Feito com carinho (e umas boas " +
                        "horas de debug 😅).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VzOnSurfaceVariant,
                    fontStyle = FontStyle.Italic
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // === Card de permissão ===
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = VzPrimary.copy(alpha = 0.20f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "🔐  Por que preciso de permissão?",
                    style = MaterialTheme.typography.titleLarge,
                    color = VzSecondary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "O VzGallery precisa acessar suas fotos e vídeos " +
                        "para exibi-los e permitir que você escolha o que " +
                        "ocultar. Nada sai do seu celular — tudo fica local.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VzOnSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // Botão de permissão — gradiente navy → azul claro
        Button(
            onClick = { permissionLauncher.launch(permissions) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(VzPrimary, VzPrimaryDark)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentColor = VzOnBackground
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = VzSecondary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Dar permissão e continuar",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = "VzTechCore  •  por Vtzin x",
            style = MaterialTheme.typography.bodyMedium,
            color = VzOnSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )
    }
}

// ============ Componentes auxiliares ============

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = VzOnBackground
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = VzOnSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = VzSecondary
        )
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(VzPrimary.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VzSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = VzOnBackground
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = VzOnSurfaceVariant
            )
        }
    }
}