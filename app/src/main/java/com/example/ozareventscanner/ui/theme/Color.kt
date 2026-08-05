package com.example.ozareventscanner.ui.theme

import androidx.compose.ui.graphics.Color

// ── Colores primarios (Diseño Ozar) ──────────────────────────────────────────
val ColorPrimary     = Color(0xFF2563EB) // Azul para pestaña activa, botones principales y acentos
val ColorBackground  = Color(0xFFF8FAFC) // Fondo gris claro muy limpio
val ColorDark        = Color(0xFFF8FAFC) // Para compatibilidad con otras pantallas
val ColorSurface     = Color(0xFFFFFFFF) // Blanco puro para tarjetas
val ColorSurfaceHigh = Color(0xFFF1F5F9) // Fondo para inputs o cajas secundarias

// ── Colores de texto ──────────────────────────────────────────────────────────
val ColorTextPrimary    = Color(0xFF1E293B) // Gris oscuro para títulos principales
val ColorTextSecondary  = Color(0xFF64748B) // Gris medio para subtítulos y placeholders
val ColorTextDisabled   = Color(0xFF94A3B8)

// ── Colores semánticos y acentos específicos ──────────────────────────────────
val ColorError       = Color(0xFFEF4444)
val ColorWarning     = Color(0xFFF59E0B)
val ColorSuccess     = Color(0xFF00B074) // Verde esmeralda para botón "Validar" y barras de progreso
val ColorAccent      = Color(0xFF00B074) // Verde alias
val ColorInfo        = Color(0xFF2563EB)

// Fondos semánticos suaves
val ColorSuccessBg   = Color(0xFFE6F7F0) // Verde muy claro
val ColorWarningBg   = Color(0xFFFEF3C7)
val ColorInfoBg      = Color(0xFFEFF6FF)
val ColorErrorBg     = Color(0xFFFEE2E2)

// Gradiente principal de fondo
val GradientFondo    = listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9))

// Compatibilidad con Material Theme
val Purple80      = Color(0xFF2563EB)
val PurpleGrey80  = Color(0xFF93C5FD)
val Pink80        = Color(0xFFBFDBFE)
val Purple40      = Color(0xFF1D4ED8)
val PurpleGrey40  = Color(0xFF1E40AF)
val Pink40        = Color(0xFF1E3A8A)