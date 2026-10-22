package com.vztechcore.vzgallery.ui.screens.settings.pages

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vztechcore.vzgallery.ui.screens.settings.SectionCard
import com.vztechcore.vzgallery.ui.screens.settings.SectionTitle
import com.vztechcore.vzgallery.ui.screens.settings.SettingsTopBar
import com.vztechcore.vzgallery.ui.theme.VzBackground
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzOnSurfaceVariant
import com.vztechcore.vzgallery.ui.theme.VzPrimary
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import com.vztechcore.vzgallery.ui.theme.VzSurface
import com.vztechcore.vzgallery.ui.theme.VzSurfaceVariant

private const val URL_ANDROIDIDE = "https://github.com/AndroidIDEOfficial/AndroidIDE"
private const val URL_CODE_STUDIO = "https://github.com/AndroidCSOfficial/android-code-studio"

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    BackHandler { onBack() }

    fun openInChrome(url: String) {
        val uri = Uri.parse(url)

        val chromeIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            component = ComponentName(
                "com.android.chrome",
                "com.google.android.apps.chrome.Main"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(chromeIntent)
            return
        } catch (_: ActivityNotFoundException) {
        } catch (_: Exception) {
        }

        try {
            val fallback = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        } catch (_: Exception) {
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VzBackground)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        SettingsTopBar(title = "Sobre", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // ===== Logo + Name =====
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(VzPrimary, VzSecondary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoLibrary,
                        contentDescription = null,
                        tint = VzOnBackground,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "VzGallery",
                    style = MaterialTheme.typography.displayLarge,
                    color = VzOnBackground
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Versão 1.0 (MVP)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VzOnSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "por Vtzin x",
                    style = MaterialTheme.typography.bodyLarge,
                    color = VzSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // ===== What is it =====
            SectionTitle("O que é")
            SectionCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "O VzGallery é uma galeria pessoal feita pra " +
                            "ter controle total sobre o que aparece na sua " +
                            "lista de fotos e vídeos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VzOnSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Com filtros, cofre de ocultos, visualizador " +
                            "próprio e uma retrospectiva anual — tudo pensado " +
                            "pra quem quer uma galeria do seu jeito.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VzOnSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ===== How it was born =====
            SectionTitle("Como nasceu")
            SectionCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Durante os projetos mobile do Vtzin x, ele " +
                            "enfrentava um problema recorrente: fotos e " +
                            "arquivos aleatórios apareciam na galeria do " +
                            "celular sem explicação.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VzOnSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Em vez de continuar convivendo com a bagunça, " +
                            "veio a ideia de construir um app de galeria " +
                            "próprio — com controle total sobre o que " +
                            "aparece e o que fica escondido.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VzOnSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Assim nasceu o VzGallery.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VzOnSurfaceVariant,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ===== Built with =====
            SectionTitle("Feito com")
            SectionCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    TechRow("Linguagem", "Kotlin")
                    Spacer(Modifier.height(8.dp))
                    TechRow("UI", "Jetpack Compose")
                    Spacer(Modifier.height(8.dp))
                    TechRow("Player de vídeo", "Media3 ExoPlayer")
                    Spacer(Modifier.height(8.dp))
                    TechRow("Carregamento de imagens", "Coil")
                    Spacer(Modifier.height(8.dp))
                    TechRow("Preferências", "DataStore")
                }
            }

            Spacer(Modifier.height(20.dp))

            // ===== IDE used =====
            SectionTitle("IDE usada")
            SectionCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Todo o desenvolvimento mobile foi feito usando " +
                            "IDEs que rodam direto no Android.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VzOnSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))

                    LinkRow(
                        title = "Android Code Studio",
                        subtitle = "Fork do AndroidIDE • recomendado pelo dev",
                        url = URL_CODE_STUDIO,
                        onOpen = { openInChrome(URL_CODE_STUDIO) }
                    )

                    Spacer(Modifier.height(10.dp))

                    LinkRow(
                        title = "AndroidIDE",
                        subtitle = "IDE original em que o Code Studio se baseia",
                        url = URL_ANDROIDIDE,
                        onOpen = { openInChrome(URL_ANDROIDIDE) }
                    )

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Nota: o AndroidIDE parou de funcionar em alguns " +
                            "dispositivos por conflito com o sistema. O Android " +
                            "Code Studio é um fork ativo, com quase a mesma " +
                            "experiência, e tem sido a escolha principal.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VzOnSurfaceVariant,
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ===== Project status =====
            SectionTitle("Estado do projeto")
            SectionCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🚧 Em desenvolvimento",
                        style = MaterialTheme.typography.bodyLarge,
                        color = VzSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "O app tá em constante evolução. Várias features " +
                            "estão previstas pra próximas atualizações, incluindo " +
                            "melhorias de performance e novas opções de " +
                            "personalização.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VzOnSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ===== Team =====
            SectionTitle("Equipe")
            SectionCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "VzTechCore",
                        style = MaterialTheme.typography.titleLarge,
                        color = VzOnBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Projeto pessoal de Vtzin x, desenvolvedor mobile " +
                            "apaixonado por Android e por fazer as coisas " +
                            "do jeito certo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VzOnSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ===== Thanks =====
            SectionTitle("Agradecimentos")
            SectionCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "A todos que testaram, deram feedback e ajudaram " +
                            "a encontrar bugs no caminho.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VzOnSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // ===== Footer =====
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Feito por Vtzin x",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VzSecondary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "©2026 VzTechCore",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VzOnSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ============================================================
// COMPONENTS
// ============================================================
@Composable
private fun TechRow(label: String, value: String) {
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
            color = VzSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LinkRow(
    title: String,
    subtitle: String,
    url: String,
    onOpen: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VzSurfaceVariant.copy(alpha = 0.5f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onOpen() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = VzOnBackground,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = VzOnSurfaceVariant,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = url.removePrefix("https://"),
                style = MaterialTheme.typography.bodyMedium,
                color = VzSecondary,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(10.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = "Abrir link",
            tint = VzSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}