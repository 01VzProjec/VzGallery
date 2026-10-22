package com.vztechcore.vzgallery.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ===== DARK PALETTE =====
val DarkBackground = Color(0xFF000000)
val DarkSurface = Color(0xFF0A0E1A)
val DarkSurfaceVariant = Color(0xFF141A2E)
val DarkPrimary = Color(0xFF1E40AF)
val DarkSecondary = Color(0xFF38BDF8)
val DarkOnBackground = Color(0xFFFFFFFF)
val DarkOnSurface = Color(0xFFE5E7EB)
val DarkOnSurfaceVariant = Color(0xFF94A3B8)
val DarkError = Color(0xFFEF4444)

// ===== LIGHT PALETTE =====
val LightBackground = Color(0xFFFFFFFF)
val LightSurface = Color(0xFFF7F8FA)
val LightSurfaceVariant = Color(0xFFE5E7EB)
val LightPrimary = Color(0xFF1E40AF)
val LightSecondary = Color(0xFF0284C7)
val LightOnBackground = Color(0xFF0A0E1A)
val LightOnSurface = Color(0xFF1F2937)
val LightOnSurfaceVariant = Color(0xFF64748B)
val LightError = Color(0xFFDC2626)

// ===== FIXO (não muda entre temas) =====
val VzPrimaryDark = Color(0xFF0F1E4A) // usado em gradientes

// ===== COMPOSABLE GETTERS — leem do tema atual =====
val VzBackground: Color
    @Composable get() = MaterialTheme.colorScheme.background

val VzSurface: Color
    @Composable get() = MaterialTheme.colorScheme.surface

val VzSurfaceVariant: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant

val VzPrimary: Color
    @Composable get() = MaterialTheme.colorScheme.primary

val VzSecondary: Color
    @Composable get() = MaterialTheme.colorScheme.secondary

val VzOnBackground: Color
    @Composable get() = MaterialTheme.colorScheme.onBackground

val VzOnSurface: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface

val VzOnSurfaceVariant: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

val VzError: Color
    @Composable get() = MaterialTheme.colorScheme.error